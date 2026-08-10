/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import fi.dy.masa.malilib.util.KeyCodes;
import io.github.darkkronicle.advancedchatbox.config.ChatBoxConfigStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import io.github.darkkronicle.advancedchatcore.util.Colors;
import io.github.darkkronicle.advancedchatcore.util.ModifierKeyUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.resources.language.I18n;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

@Environment(EnvType.CLIENT)
public class ChatSuggestorGui {
    private final Minecraft client;
    private final Screen owner;
    private final EditBox textField;
    private final Font textRenderer;
    private final boolean slashOptional;
    private final boolean suggestingWhenEmpty;
    private final int inWindowIndexOffset;
    private final int maxSuggestionSize;
    private final boolean chatScreenSized;
    private final List<FormattedCharSequence> messages = Lists.newArrayList();
    private int x;
    private int width;
    private SuggestionWindow window;

    private boolean windowActive;
    private boolean completingSuggestions;
    private boolean showSuggestionsWhenReady;

    private final ChatFormatter formatter;
    private final ChatSuggestor suggestor;

    public ChatSuggestorGui(Minecraft client, Screen owner, EditBox textField, Font textRenderer,
            boolean slashRequired, boolean suggestingWhenEmpty, int inWindowIndexOffset, int maxSuggestionSize,
            boolean chatScreenSized) {
        this.client = client;
        this.owner = owner;
        this.textField = textField;
        this.textRenderer = textRenderer;
        this.slashOptional = slashRequired;
        this.suggestingWhenEmpty = suggestingWhenEmpty;
        this.inWindowIndexOffset = inWindowIndexOffset;
        this.maxSuggestionSize = maxSuggestionSize;
        this.chatScreenSized = chatScreenSized;
        this.suggestor = new ChatSuggestor(textField);
        this.formatter = new ChatFormatter(textField, suggestor);
    }

    public void setWindowActive(boolean windowActive) {
        this.windowActive = windowActive;
        if (!windowActive) {
            this.window = null;
        }
    }

    public boolean keyPressed(KeyEvent input) {
        if (this.window != null && this.window.keyPressed(input)) {
            return true;
        }
        if (this.owner.getFocused() == this.textField && input.key() == KeyCodes.KEY_TAB) {
            this.windowActive = true;
            this.showSuggestionsWhenReady = true;
            if (isCommand(this.textField.getValue())) {
                if (this.suggestor.updateCommandSpellcheckSuggestions()) {
                    this.showSuggestions(true);
                    this.showSuggestionsWhenReady = false;
                    return true;
                }
                this.suggestor.updateCommandSuggestions(true, () -> this.client.execute(() -> {
                    if (this.showSuggestionsWhenReady && this.suggestor.isDone()) {
                        this.showIfActive(true);
                    }
                }));
                return true;
            }
            this.suggestor.updateChatSuggestions(true);
            this.showSuggestions(true);
            this.showSuggestionsWhenReady = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double amount) {
        return (this.window != null && this.window.mouseScrolled(Mth.clamp(amount, -1.0D, 1.0D)));
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return (this.window != null && this.window.mouseClicked((int) click.x(), (int) click.y(), click.button()));
    }

    public void showSuggestions(boolean narrateFirstSuggestion) {
        if (!suggestor.isDone()) {
            return;
        }
        List<AdvancedSuggestion> suggestions = suggestor.getSuggestions();
        if (suggestions.isEmpty()) {
            return;
        }
        int width = 0;
        for (AdvancedSuggestion value : suggestions) {
            // Have suggestion be as wide as the biggest
            width = Math.max(width, this.textRenderer.width(value.getRender()));
        }

        int x = Mth.clamp(this.textField.getScreenX(suggestor.getRange().getStart()), 0,
                this.textField.getScreenX(0) + this.textField.getInnerWidth() - width);
        int y = this.chatScreenSized ? this.owner.height - 12 : 72;
        this.window = new SuggestionWindow(x, y, width, suggestions, narrateFirstSuggestion);
    }

    public void refresh() {
        String currentText = this.textField.getValue();
        if (this.suggestor.getParse() != null
                && !this.suggestor.getParse().getReader().getString().equals(currentText)) {
            // Set it to null to signify that if it is still a command, to parse it
            this.suggestor.invalidateParse();
        }

        if (!this.completingSuggestions) {
            this.textField.setSuggestion(null);
            this.window = null;
        }

        this.messages.clear();
        StringReader stringReader = new StringReader(currentText);
        boolean command = stringReader.canRead() && stringReader.peek() == '/';
        if (command) {
            stringReader.skip();
        }

        boolean suggestCommand = this.slashOptional || command;
        if (suggestCommand) {
            int cursorIndex = this.textField.getCursorPosition();
            this.suggestor.updateParse(stringReader);
            // Index 1 will enforce that the player typed at LEAST ONE character
            if ((this.suggestingWhenEmpty || cursorIndex >= stringReader.getCursor())
                    && (this.window == null || !this.completingSuggestions)) {
                if (this.suggestor.updateCommandSpellcheckSuggestions()) {
                    this.showSuggestions(false);
                    this.showSuggestionsWhenReady = false;
                    return;
                }
                this.suggestor.updateCommandSuggestions(() -> {
                    if (this.suggestor.isDone()) {
                        this.client.execute(() -> this.showIfActive(true));
                    }
                });
            }
        } else {
            this.suggestor.updateChatSuggestions(false);
            if (this.windowActive && this.suggestor.isDone()) {
                this.showSuggestions(false);
            }
        }
    }

    private static FormattedCharSequence formatException(CommandSyntaxException exception) {
        Component text = ComponentUtils.fromMessage(exception.getRawMessage());
        String string = exception.getContext();
        return string == null ? text.getVisualOrderText()
                : (Component.translatable("command.context.parse_error", text, exception.getCursor(), string))
                        .getVisualOrderText();
    }

    private void showIfActive(boolean autoShow) {
        ParseResults<ClientSuggestionProvider> parse = this.suggestor.getParse();
        if (parse == null) {
            this.x = 0;
            this.width = this.owner.width;
            this.window = null;
            if (autoShow || this.windowActive) {
                this.showSuggestions(false);
            }
            this.showSuggestionsWhenReady = false;
            return;
        }

        if (this.textField.getCursorPosition() == this.textField.getValue().length()) {
            if (this.suggestor.getSuggestions().isEmpty() && !parse.getExceptions().isEmpty()) {
                int builtInExceptions = 0;

                for (Map.Entry<CommandNode<ClientSuggestionProvider>, CommandSyntaxException> commandNodeCommandSyntaxExceptionEntry : parse
                        .getExceptions().entrySet()) {
                    CommandSyntaxException commandSyntaxException = commandNodeCommandSyntaxExceptionEntry.getValue();
                    if (commandSyntaxException.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS
                            .literalIncorrect()) {
                        ++builtInExceptions;
                    } else {
                        this.messages.add(formatException(commandSyntaxException));
                    }
                }

                if (builtInExceptions > 0) {
                    this.messages.add(formatException(
                            CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create()));
                }
            } else if (parse.getReader().canRead()) {
                this.messages.add(formatException(Commands.getParseException(parse)));
            }
        }
        this.x = 0;
        this.width = this.owner.width;
        if (this.messages.isEmpty()) {
            this.showUsages(ChatFormatting.GRAY);
        }
        this.window = null;
        if (autoShow || this.windowActive) {
            this.showSuggestions(false);
        }
        this.showSuggestionsWhenReady = false;
    }

    private static boolean isCommand(String currentText) {
        return !currentText.isEmpty() && currentText.charAt(0) == '/';
    }

    private void showUsages(ChatFormatting formatting) {
        if (this.suggestor.getParse() == null) {
            return;
        }
        CommandContextBuilder<ClientSuggestionProvider> commandContextBuilder = this.suggestor.getParse().getContext();
        SuggestionContext<ClientSuggestionProvider> suggestionContext =
                commandContextBuilder.findSuggestionContext(this.textField.getCursorPosition());
        Map<CommandNode<ClientSuggestionProvider>, String> map = this.client.player.connection.getCommands()
                .getSmartUsage(suggestionContext.parent, this.client.player.connection.getSuggestionsProvider());
        List<FormattedCharSequence> list = new ArrayList<>();
        int i = 0;
        Style style = Style.EMPTY.withColor(formatting);

        for (Map.Entry<CommandNode<ClientSuggestionProvider>, String> commandNodeStringEntry : map.entrySet()) {
            if (!(commandNodeStringEntry.getKey() instanceof LiteralCommandNode)) {
                list.add(FormattedCharSequence.forward(commandNodeStringEntry.getValue(), style));
                i = Math.max(i, this.textRenderer.width(commandNodeStringEntry.getValue()));
            }
        }

        if (!list.isEmpty()) {
            this.messages.addAll(list);
            this.x = Mth.clamp(this.textField.getScreenX(suggestionContext.startPos), 0,
                    this.textField.getScreenX(0) + this.textField.getInnerWidth() - i);
            this.width = i;
        }
    }

    private FormattedCharSequence provideRenderText(String original, int firstCharacterIndex) {
        return this.formatter.apply(original, firstCharacterIndex);
    }

    @Nullable
    private static String getSuggestionSuffix(String original, String suggestion) {
        return suggestion.startsWith(original) ? suggestion.substring(original.length()) : null;
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (this.window != null) {
            this.window.render(context, mouseX, mouseY);
        } else {
            int i = 0;

            for (FormattedCharSequence message : this.messages) {
                i++;
                int j = this.chatScreenSized ? this.owner.height - 14 - 13 - 12 * i : 72 + 12 * i;
                context.fill(this.x - 1, j, this.x + this.width + 1, j + 12,
                        ChatBoxConfigStorage.General.BACKGROUND_COLOR.config.getIntegerValue());
                if (message != null) {
                    context.text(textRenderer, message, this.x, j + 2, -1);
                }
            }
        }
    }

    public String getNarration() {
        return this.window != null ? "\n" + this.window.getNarration() : "";
    }

    @Environment(EnvType.CLIENT)
    public class SuggestionWindow {
        private final Rect2i area;
        private final String typedText;
        private final List<AdvancedSuggestion> suggestions;
        private int inWindowIndex;
        private int selection;
        private Vec2 mouse;
        private boolean completed;
        private int lastNarrationIndex;

        private SuggestionWindow(int x, int y, int width, List<AdvancedSuggestion> list,
                boolean narrateFirstSuggestion) {
            this.mouse = Vec2.ZERO;
            int renderX = x - 1;
            int renderY = ChatSuggestorGui.this.chatScreenSized
                    ? y - 3 - Math.min(list.size(), ChatSuggestorGui.this.maxSuggestionSize) * 12
                    : y;
            this.area = new Rect2i(renderX, renderY, width + 1,
                    Math.min(list.size(), ChatSuggestorGui.this.maxSuggestionSize) * 12);
            this.typedText = ChatSuggestorGui.this.textField.getValue();
            this.lastNarrationIndex = narrateFirstSuggestion ? -1 : 0;
            this.suggestions = list;
            this.select(0);
        }

        public void render(GuiGraphicsExtractor context, int mouseX, int mouseY) {
            int suggestionSize = Math.min(this.suggestions.size(), ChatSuggestorGui.this.maxSuggestionSize);
            boolean moreBelow = this.inWindowIndex > 0;
            boolean moreAbove = this.suggestions.size() > this.inWindowIndex + suggestionSize;
            boolean more = moreBelow || moreAbove;
            boolean updateMouse = this.mouse.x != (float) mouseX || this.mouse.y != (float) mouseY;
            if (updateMouse) {
                this.mouse = new Vec2((float) mouseX, (float) mouseY);
            }

            if (more) {
                // Draw lines to signify that there is more
                context.fill(this.area.getX(), this.area.getY() - 1,
                        this.area.getX() + this.area.getWidth(), this.area.getY(),
                        ChatBoxConfigStorage.General.BACKGROUND_COLOR.config.getIntegerValue());
                context.fill(this.area.getX(), this.area.getY() + this.area.getHeight(),
                        this.area.getX() + this.area.getWidth(), this.area.getY() + this.area.getHeight() + 1,
                        ChatBoxConfigStorage.General.BACKGROUND_COLOR.config.getIntegerValue());
                int x;
                if (moreBelow) {
                    // Dotted
                    for (x = 0; x < this.area.getWidth(); ++x) {
                        if (x % 2 == 0) {
                            context.fill(this.area.getX() + x, this.area.getY() - 1,
                                    this.area.getX() + x + 1, this.area.getY(),
                                    Colors.getInstance().getColorOrWhite("white").color());
                        }
                    }
                }

                if (moreAbove) {
                    // Dotted
                    for (x = 0; x < this.area.getWidth(); ++x) {
                        if (x % 2 == 0) {
                            context.fill(this.area.getX() + x,
                                    this.area.getY() + this.area.getHeight(), this.area.getX() + x + 1,
                                    this.area.getY() + this.area.getHeight() + 1,
                                    Colors.getInstance().getColorOrWhite("white").color());
                        }
                    }
                }
            }

            boolean hover = false;

            for (int s = 0; s < suggestionSize; ++s) {
                AdvancedSuggestion suggestion = this.suggestions.get(s + this.inWindowIndex);
                context.fill(this.area.getX(), this.area.getY() + 12 * s,
                        this.area.getX() + this.area.getWidth(), this.area.getY() + 12 * s + 12,
                        ChatBoxConfigStorage.General.BACKGROUND_COLOR.config.getIntegerValue());
                if (mouseX > this.area.getX() && mouseX < this.area.getX() + this.area.getWidth()
                        && mouseY > this.area.getY() + 12 * s && mouseY < this.area.getY() + 12 * s + 12) {
                    if (updateMouse) {
                        this.select(s + this.inWindowIndex);
                    }

                    hover = true;
                }
                context.text(ChatSuggestorGui.this.textRenderer, suggestion.getRender(),
                        (this.area.getX() + 1), (this.area.getY() + 2 + 12 * s),
                        (s + this.inWindowIndex) == this.selection
                                ? ChatBoxConfigStorage.General.HIGHLIGHT_COLOR.config.getIntegerValue()
                                : ChatBoxConfigStorage.General.UNHIGHLIGHT_COLOR.config.getIntegerValue());
            }

            if (hover) {
                Message message = this.suggestions.get(this.selection).getTooltip();
                if (message != null) {
                    context.setTooltipForNextFrame(textRenderer, ComponentUtils.fromMessage(message), mouseX, mouseY);
                }
            }
        }

        public boolean mouseClicked(int x, int y, int button) {
            if (!this.area.contains(x, y)) {
                return false;
            }
            int i = (y - this.area.getY()) / 12 + this.inWindowIndex;
            if (i >= 0 && i < this.suggestions.size()) {
                this.select(i);
                this.complete();
            }

            return true;
        }

        public boolean mouseScrolled(double amount) {
            int x = (int) (ChatSuggestorGui.this.client.mouseHandler.xpos()
                    * (double) ChatSuggestorGui.this.client.getWindow().getGuiScaledWidth()
                    / (double) ChatSuggestorGui.this.client.getWindow().getWidth());
            int y = (int) (ChatSuggestorGui.this.client.mouseHandler.ypos()
                    * (double) ChatSuggestorGui.this.client.getWindow().getGuiScaledHeight()
                    / (double) ChatSuggestorGui.this.client.getWindow().getHeight());
            if (this.area.contains(x, y)) {
                this.inWindowIndex = Mth.clamp((int) ((double) this.inWindowIndex - amount), 0,
                        Math.max(this.suggestions.size() - ChatSuggestorGui.this.maxSuggestionSize, 0));
                return true;
            }
            return false;
        }

        public boolean keyPressed(KeyEvent input) {
            if (input.key() == KeyCodes.KEY_UP) {
                this.scroll(-1);
                this.completed = false;
                return true;
            }
            if (input.key() == KeyCodes.KEY_DOWN) {
                this.scroll(1);
                this.completed = false;
                return true;
            }
            if (input.key() == KeyCodes.KEY_TAB) {
                if (this.completed) {
                    this.scroll(ModifierKeyUtil.hasShiftDown() ? -1 : 1);
                }

                this.complete();
                return true;
            }
            if (input.key() == KeyCodes.KEY_ESCAPE) {
                this.discard();
                return true;
            }
            return false;
        }

        public void scroll(int offset) {
            this.select(this.selection + offset);
            int windowIndex = this.inWindowIndex;
            int maxInWindow = this.inWindowIndex + ChatSuggestorGui.this.maxSuggestionSize - 1;
            // Moving window logic
            if (this.selection < windowIndex) {
                this.inWindowIndex = Mth.clamp(this.selection, 0,
                        Math.max(this.suggestions.size() - ChatSuggestorGui.this.maxSuggestionSize, 0));
            } else if (this.selection > maxInWindow) {
                this.inWindowIndex = Mth.clamp(
                        this.selection + ChatSuggestorGui.this.inWindowIndexOffset
                                - ChatSuggestorGui.this.maxSuggestionSize,
                        0, Math.max(this.suggestions.size() - ChatSuggestorGui.this.maxSuggestionSize, 0));
            }
        }

        public void select(int index) {
            this.selection = index;
            if (this.selection < 0) {
                // Wrap to the top
                this.selection += this.suggestions.size();
            }

            if (this.selection >= this.suggestions.size()) {
                // Wrap to the bottom
                this.selection -= this.suggestions.size();
            }

            Suggestion suggestion = this.suggestions.get(this.selection);
            ChatSuggestorGui.this.textField.setSuggestion(ChatSuggestorGui
                    .getSuggestionSuffix(ChatSuggestorGui.this.textField.getValue(), suggestion.apply(this.typedText)));
            if (client.getNarrator().isActive() && this.lastNarrationIndex != this.selection) {
                client.getNarrator().saySystemChatQueued(this.getNarration());
            }
        }

        public void complete() {
            Suggestion suggestion = this.suggestions.get(this.selection);
            ChatSuggestorGui.this.completingSuggestions = true;
            ChatSuggestorGui.this.textField.setValue(suggestion.apply(this.typedText));
            int i = suggestion.getRange().getStart() + suggestion.getText().length();
            ChatSuggestorGui.this.textField.setCursorPosition(i);
            ChatSuggestorGui.this.textField.setHighlightPos(i);
            this.select(this.selection);
            ChatSuggestorGui.this.completingSuggestions = false;
            this.completed = true;
        }

        private Component getNarration() {
            this.lastNarrationIndex = this.selection;
            Suggestion suggestion = this.suggestions.get(this.selection);
            Message message = suggestion.getTooltip();
            return Component.literal(message != null
                    ? I18n.get("narration.suggestion.tooltip", this.selection + 1, this.suggestions.size(),
                            suggestion.getText(), message.getString())
                    : I18n.get("narration.suggestion", this.selection + 1, this.suggestions.size(),
                            suggestion.getText()));
        }

        public void discard() {
            ChatSuggestorGui.this.window = null;
        }
    }
}

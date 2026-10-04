/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.config.gui;

import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatbox.config.ChatBoxConfigStorage;
import io.github.darkkronicle.advancedchatcore.gui.WidgetConfigListEntry;
import io.github.darkkronicle.advancedchatcore.gui.buttons.NamedSimpleButton;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class CommandSpellcheckEntryWidget extends WidgetConfigListEntry<CommandSpellcheckEntry> {
    private final CommandSpellcheckListWidget parent;
    private final TextFieldWrapper<GuiTextFieldGeneric> command;
    private final List<TextFieldWrapper<GuiTextFieldGeneric>> texts = new ArrayList<>();

    public CommandSpellcheckEntryWidget(int x, int y, int width, int height, boolean isOdd,
            CommandSpellcheckEntry entry, int listIndex, CommandSpellcheckListWidget parent) {
        super(x, y, width, height, isOdd, entry, listIndex);
        this.parent = parent;
        y += 1;
        int pos = x + width - 2;

        int removeWidth = addButton(pos, y, "advancedchatbox.config.commandspellcheck.remove",
                (button, mouseButton) -> {
                    parent.screen.getCommands().remove(entry);
                    parent.refreshEntries();
                    parent.screen.saveCommandsToFile();
                }) + 1;
        pos -= removeWidth;

        GuiTextFieldGeneric commandField = new GuiTextFieldGeneric(x, y, pos - x, 20,
                Minecraft.getInstance().font);
        commandField.setMaxLength(128);
        commandField.setValue(entry.getCommand());
        command = new TextFieldWrapper<>(commandField, new SaveListener(this));
        texts.add(command);
        parent.addTextField(command);
    }

    @Override
    public List<TextFieldWrapper<GuiTextFieldGeneric>> getTextFields() {
        return texts;
    }

    @Override
    public String getName() {
        return null;
    }

    public void save() {
        entry.setCommand(ChatBoxConfigStorage.General.normalizeCommand(command.textField().getValue()));
    }

    private static class SaveListener implements ITextFieldListener<GuiTextFieldGeneric> {
        private final CommandSpellcheckEntryWidget parent;

        public SaveListener(CommandSpellcheckEntryWidget parent) {
            this.parent = parent;
        }

        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField) {
            parent.entry.setCommand(textField.getValue());
            parent.parent.screen.saveCommands();
            return false;
        }
    }

    protected int addButton(int x, int y, String translation, IButtonActionListener listener) {
        ButtonGeneric button = new NamedSimpleButton(x, y, StringUtils.translate(translation), false);
        this.addButton(button, listener);
        return button.getWidth();
    }
}

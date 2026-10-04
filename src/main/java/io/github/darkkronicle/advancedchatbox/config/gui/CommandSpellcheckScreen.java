/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.config.gui;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.KeyCodes;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatbox.config.ChatBoxConfigStorage;
import io.github.darkkronicle.advancedchatcore.config.gui.GuiConfig;
import io.github.darkkronicle.advancedchatcore.gui.CoreGuiListBase;
import io.github.darkkronicle.advancedchatcore.gui.buttons.NamedSimpleButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

public class CommandSpellcheckScreen
        extends CoreGuiListBase<CommandSpellcheckEntry, CommandSpellcheckEntryWidget, CommandSpellcheckListWidget> {
    private final List<CommandSpellcheckEntry> commands = new ArrayList<>();

    public CommandSpellcheckScreen(Screen parent) {
        super(10, 60);
        this.title = StringUtils.translate("advancedchatbox.config.commandspellcheck.screen.name");
        this.setParent(parent);
        for (String command : ChatBoxConfigStorage.General.getCommandSpellcheckCommands()) {
            commands.add(new CommandSpellcheckEntry(command));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = 10;
        int y = 26;
        y += GuiConfig.addTabButtons(this, x, y) * 22;
        y += GuiConfig.addAllChildrenButtons(this, GuiConfig.TAB, x, y) * 22;

        this.addLabel(x, y + 5, this.width - 150, 12, -1,
                StringUtils.translate("advancedchatbox.config.commandspellcheck.description"));
        x = this.width - 10;
        this.addButton(x, y, "advancedchatbox.config.commandspellcheck.add",
                (button, mouseButton) -> this.addCommand());
        y += 24;

        int scrollbarPosition = this.getListWidget().getScrollbar().getValue();
        this.setListPosition(this.getListX(), y);
        this.reCreateListWidget();
        this.getListWidget().getScrollbar().setValue(scrollbarPosition);
        this.getListWidget().refreshEntries();
    }

    protected void addButton(int x, int y, String translation, IButtonActionListener listener) {
        ButtonGeneric button = new NamedSimpleButton(x, y, StringUtils.translate(translation), false);
        this.addButton(button, listener);
    }

    @Override
    public boolean onKeyTyped(KeyEvent input) {
        if (input.key() == KeyCodes.KEY_ESCAPE) {
            closeGui(false);
            return true;
        }
        return super.onKeyTyped(input);
    }

    public List<CommandSpellcheckEntry> getCommands() {
        return commands;
    }

    public void addCommand() {
        commands.add(new CommandSpellcheckEntry("msg"));
        getListWidget().refreshEntries();
        saveCommandsToFile();
    }

    public void saveCommands() {
        List<String> savedCommands = new ArrayList<>();
        for (CommandSpellcheckEntry command : commands) {
            savedCommands.add(command.getCommand());
        }
        ChatBoxConfigStorage.General.setCommandSpellcheckCommands(savedCommands);
    }

    public void saveCommandsToFile() {
        saveCommands();
        ChatBoxConfigStorage.saveFromFile();
    }

    @Override
    protected void closeGui(boolean showParent) {
        if (getListWidget() != null) {
            getListWidget().save();
        } else {
            saveCommands();
        }
        ChatBoxConfigStorage.saveFromFile();
        super.closeGui(showParent);
    }

    @Override
    protected CommandSpellcheckListWidget createListWidget(int listX, int listY) {
        return new CommandSpellcheckListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null,
                this);
    }

    @Override
    protected int getBrowserWidth() {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight() {
        return this.height - 6 - this.getListY();
    }

}

/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.config.gui;

import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import io.github.darkkronicle.advancedchatcore.gui.WidgetConfigList;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;

public class CommandSpellcheckListWidget
        extends WidgetConfigList<CommandSpellcheckEntry, CommandSpellcheckEntryWidget> {
    public final CommandSpellcheckScreen screen;

    public CommandSpellcheckListWidget(int x, int y, int width, int height,
            @Nullable ISelectionListener<CommandSpellcheckEntry> selectionListener, CommandSpellcheckScreen screen) {
        super(x, y, width, height, selectionListener, screen);
        this.screen = screen;
        this.setParent(screen);
    }

    @Override
    protected CommandSpellcheckEntryWidget createListEntryWidget(int x, int y, int listIndex, boolean isOdd,
            CommandSpellcheckEntry entry) {
        return new CommandSpellcheckEntryWidget(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }

    @Override
    public boolean onKeyTyped(KeyInput input) {
        boolean value = super.onKeyTyped(input);
        save();
        return value;
    }

    public void save() {
        for (CommandSpellcheckEntryWidget widget : this.listWidgets) {
            widget.save();
        }
        screen.saveCommands();
    }

    @Override
    protected Collection<CommandSpellcheckEntry> getAllEntries() {
        return screen.getCommands();
    }
}

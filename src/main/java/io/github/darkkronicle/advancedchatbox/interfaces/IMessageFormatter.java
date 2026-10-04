/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.interfaces;

import com.mojang.brigadier.ParseResults;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** An interface for formatting the chat text box on the chat screen. */
public interface IMessageFormatter {
    /**
     * Changes how the chat text bar is rendered on the chat screen
     *
     * @param text Current text that will be rendered
     * @param parse Current commands that have been parsed
     * @return Component that should render on the chat text bar. If empty it won't modify.
     */
    Optional<Component> format(Component text, @Nullable ParseResults<ClientSuggestionProvider> parse);
}

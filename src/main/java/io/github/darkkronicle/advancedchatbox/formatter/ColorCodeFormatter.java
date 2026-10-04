/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.formatter;

import com.mojang.brigadier.ParseResults;
import io.github.darkkronicle.advancedchatbox.interfaces.IMessageFormatter;
import io.github.darkkronicle.advancedchatcore.util.*;

import java.util.Optional;
import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

@Environment(EnvType.CLIENT)
public class ColorCodeFormatter implements IMessageFormatter {

    @Override
    public Optional<Component> format(Component text, @Nullable ParseResults<ClientSuggestionProvider> parse) {
        if (parse != null) {
            return Optional.empty();
        }
        String string = text.getString();
        if (!string.contains("&")) {
            return Optional.empty();
        }
        SearchResult search = SearchResult.searchOf(string, "(?i)&[0-9A-FK-OR]", FindType.REGEX);
        if (search.size() == 0) {
            return Optional.empty();
        }
        int index = 0;
        Style last = Style.EMPTY;
        TextBuilder formatted = new TextBuilder();
        for (StringMatch match : search.getMatches()) {
            formatted.append(TextUtil.truncate(text, new StringMatch("", index, match.start)).withStyle(last));
            ChatFormatting format = ChatFormatting.getByCode(match.match.charAt(1));
            last = last.applyFormat(format);
            index = match.start;
        }
        MutableComponent small = TextUtil.truncate(text, new StringMatch("", index, string.length()));
        if (!small.getString().isEmpty()) {
            formatted.append(small.withStyle(last));
        }
        return Optional.of(formatted.build());
    }
}

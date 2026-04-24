/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatbox.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatbox.AdvancedChatBox;
import io.github.darkkronicle.advancedchatbox.registry.ChatFormatterRegistry;
import io.github.darkkronicle.advancedchatbox.registry.ChatSuggestorRegistry;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.config.SaveableConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ChatBoxConfigStorage implements IConfigHandler {
    public static final String CONFIG_FILE_NAME = AdvancedChatBox.MOD_ID + ".json";
    private static final int CONFIG_VERSION = 1;

    public static class General {
        public static final String NAME = "general";

        public static String translate(String key) {
            return StringUtils.translate("advancedchatbox.config.general." + key);
        }

        public static final SaveableConfig<ConfigColor> HIGHLIGHT_COLOR =
                SaveableConfig.fromConfig("highlightColor", new ConfigColor(translate("highlightcolor"),
                        "#FFFFFF00", translate("info.highlightcolor")));
        public static final SaveableConfig<ConfigColor> UNHIGHLIGHT_COLOR =
                SaveableConfig.fromConfig("unhighlightColor", new ConfigColor(translate("unhighlightcolor"),
                        "#FFAAAAAA", translate("info.unhighlightcolor")));
        public static final SaveableConfig<ConfigColor> BACKGROUND_COLOR =
                SaveableConfig.fromConfig("backgroundColor", new ConfigColor(translate("backgroundcolor"),
                        "#AA000000", translate("info.backgroundcolor")));
        public static final SaveableConfig<ConfigInteger> SUGGESTION_SIZE = SaveableConfig.fromConfig("suggestionSize",
                new ConfigInteger(translate("suggestionsize"), 10, 1, 50, translate("info.suggestionsize")));
        public static final SaveableConfig<ConfigBoolean> REMOVE_IDENTIFIER =
                SaveableConfig.fromConfig("removeIdentifier",
                        new ConfigBoolean(translate("removeidentifier"), true, translate("info.removeidentifier")));
        public static final SaveableConfig<ConfigBoolean> PRUNE_PLAYER_SUGGESTIONS = SaveableConfig.fromConfig(
                "prunePlayerSuggestions",
                new ConfigBoolean(translate("pruneplayersuggestions"), true, translate("info.pruneplayersuggestions")));
        public static final SaveableConfig<ConfigColor> AVAILABLE_SUGGESTION_COLOR = SaveableConfig
                .fromConfig("availableSuggestionColor", new ConfigColor(translate("availablesuggestioncolor"),
                        "#FF969696", translate("info.availablesuggestioncolor")));
        public static final SaveableConfig<ConfigStringList> COMMAND_SPELLCHECK_COMMANDS = SaveableConfig.fromConfig(
                "commandSpellcheckCommands",
                new ConfigStringList(translate("commandspellcheckcommands"),
                        ImmutableList.of("msg", "tell", "w", "r", "reply", "me"),
                        translate("info.commandspellcheckcommands")));

        public static final ImmutableList<SaveableConfig<? extends IConfigBase>> OPTIONS =
                ImmutableList.of(HIGHLIGHT_COLOR, UNHIGHLIGHT_COLOR, BACKGROUND_COLOR, SUGGESTION_SIZE,
                        REMOVE_IDENTIFIER, PRUNE_PLAYER_SUGGESTIONS, AVAILABLE_SUGGESTION_COLOR);
        public static final ImmutableList<SaveableConfig<? extends IConfigBase>> HIDDEN_OPTIONS =
                ImmutableList.of(COMMAND_SPELLCHECK_COMMANDS);

        public static List<String> getCommandSpellcheckCommands() {
            return normalizeCommands(COMMAND_SPELLCHECK_COMMANDS.config.getStrings());
        }

        public static void setCommandSpellcheckCommands(List<String> commands) {
            COMMAND_SPELLCHECK_COMMANDS.config.setStrings(normalizeCommands(commands));
        }

        public static String normalizeCommand(String command) {
            String normalized = command.trim().toLowerCase(Locale.ROOT);
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            int namespaceIndex = normalized.lastIndexOf(':');
            if (namespaceIndex >= 0 && namespaceIndex + 1 < normalized.length()) {
                normalized = normalized.substring(namespaceIndex + 1);
            }
            return normalized;
        }

        private static List<String> normalizeCommands(List<String> commands) {
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String command : commands) {
                String normalizedCommand = normalizeCommand(command);
                if (!normalizedCommand.isEmpty()) {
                    normalized.add(normalizedCommand);
                }
            }
            return new ArrayList<>(normalized);
        }
    }

    public static class SpellChecker {
        public static final String NAME = "spellchecker";

        public static String translate(String key) {
            return StringUtils.translate("advancedchatbox.config.spellchecker." + key);
        }

        public static final SaveableConfig<ConfigString> HOVER_TEXT = SaveableConfig.fromConfig("hoverText",
                new ConfigString(translate("hovertext"), "&7$1&b$2&7$3", translate("info.hovertext")));

        // public static final SaveableConfig<ConfigBoolean>
        // SUGGEST_CAPITAL =
        // SaveableConfig.fromConfig(
        // "suggest_capital",
        // new ConfigBoolean(
        // translate("suggestcapital"),
        // true,
        // translate("info.suggestcapital")
        // )
        // );

        public static final ImmutableList<SaveableConfig<? extends IConfigBase>> OPTIONS = ImmutableList.of(HOVER_TEXT
        // SUGGEST_CAPITAL
        );
    }

    public static void loadFromFile() {
        File configFile =
                FileUtils.getConfigDirectoryAsPath().resolve("advancedchat").resolve(CONFIG_FILE_NAME).toFile();

        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            JsonElement element = ConfigStorage.parseJsonFile(configFile);

            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();

                ConfigStorage.readOptions(root, General.NAME, (List<SaveableConfig<?>>) General.OPTIONS);
                ConfigStorage.readOptions(root, General.NAME, (List<SaveableConfig<?>>) General.HIDDEN_OPTIONS);
                ConfigStorage.readOptions(root, SpellChecker.NAME, (List<SaveableConfig<?>>) SpellChecker.OPTIONS);
                General.setCommandSpellcheckCommands(General.COMMAND_SPELLCHECK_COMMANDS.config.getStrings());

                ConfigStorage.applyRegistry(root.get(ChatFormatterRegistry.NAME), ChatFormatterRegistry.getInstance());
                ConfigStorage.applyRegistry(root.get(ChatSuggestorRegistry.NAME), ChatSuggestorRegistry.getInstance());

                int version = JsonUtils.getIntegerOrDefault(root, "configVersion", 0);
            }
        }
    }

    public static void saveFromFile() {
        File dir = FileUtils.getConfigDirectoryAsPath().resolve("advancedchat").toFile();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();

            General.setCommandSpellcheckCommands(General.COMMAND_SPELLCHECK_COMMANDS.config.getStrings());
            ConfigStorage.writeOptions(root, General.NAME, (List<SaveableConfig<?>>) General.OPTIONS);
            ConfigStorage.writeOptions(root, General.NAME, (List<SaveableConfig<?>>) General.HIDDEN_OPTIONS);
            ConfigStorage.writeOptions(root, SpellChecker.NAME, (List<SaveableConfig<?>>) SpellChecker.OPTIONS);

            root.add("config_version", new JsonPrimitive(CONFIG_VERSION));

            root.add(ChatFormatterRegistry.NAME, ConfigStorage.saveRegistry(ChatFormatterRegistry.getInstance()));
            root.add(ChatSuggestorRegistry.NAME, ConfigStorage.saveRegistry(ChatSuggestorRegistry.getInstance()));

            ConfigStorage.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveFromFile();
    }
}

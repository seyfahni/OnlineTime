/*
 * MIT License
 *
 * Copyright (c) 2018 Marvin Klar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mr.minecraft15.onlinetime.bungee;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.api.PlayerData;
import mr.minecraft15.onlinetime.api.PluginProxy;
import mr.minecraft15.onlinetime.api.PluginScheduler;
import mr.minecraft15.onlinetime.common.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;

public class OnlineTimeBungeePlugin extends Plugin implements PluginProxy {

    private static final int CONFIG_VERSION = 1;

    private Configuration config;

    private PluginScheduler scheduler;

    private MineDown messageFormat;
    private Localization localization;
    private TimeParser parser;

    private BaseComponent[] serverName;

    private long saveInterval;

    private AccumulatingOnlineTimeStorage onlineTimeStorage;
    private PlayerNameStorage playerNameStorage;

    private ScheduledTask flushCacheTask;

    @Override
    public void onEnable() {
        this.scheduler = new BungeeSchedulerAdapter(this, getProxy().getScheduler());

        if (!(loadConfig() && loadStorage())) {
            getLogger().log(Level.SEVERE, "Could not enable OnlineTime!");
            return;
        }

        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerCommand(this, new PluginCommandBungeeAdapter(
                new OnlineTimeCommand(this, localization, onlineTimeStorage),
                "onlinetime", "onlinet", "otime", "ot"));
        pluginManager.registerCommand(this, new PluginCommandBungeeAdapter(
                new OnlineTimeAdminCommand(this, localization, parser, onlineTimeStorage),
                "onlinetimeadmin", "ota", "otadmin", "onlinetimea"));
        pluginManager.registerListener(this, new PlayerNameBungeeListener(this, playerNameStorage));
        pluginManager.registerListener(this, new OnlineTimeAccumulatorBungeeListener(this, onlineTimeStorage));
        flushCacheTask = getProxy().getScheduler().schedule(this, this::flushOnlineTimeCache, saveInterval / 2L, saveInterval, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
        flushCacheTask.cancel();
        flushOnlineTimeCache();
        if (onlineTimeStorage != null) {
            try {
                onlineTimeStorage.close();
            } catch (StorageException ex) {
                getLogger().log(Level.SEVERE, "error while closing online time storage", ex);
            }
        }
        if (playerNameStorage != null) {
            try {
                playerNameStorage.close();
            } catch (StorageException ex) {
                getLogger().log(Level.SEVERE, "error while closing player name storage", ex);
            }
        }
    }

    @Override
    public MineDown getFormattedMessage(MineDown rawMessage) {
        return messageFormat.copy()
                .replace("message", rawMessage
                    .replace("server", serverName)
                    .toComponent());
    }

    private boolean loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        this.config = loadOrCreateYamlConfig("config.yml");
        if (config == null) {
            getLogger().severe("Could not load main configuration.");
            return false;
        }

        int currentConfigVersion = config.getInt("configversion", 0);
        if (currentConfigVersion != CONFIG_VERSION) {
            getLogger().warning("Old configuration found! Migrating...");
            boolean success = migrateConfig(currentConfigVersion);
            if (!success) {
                getLogger().severe("Could not migrate from old configuration.");
                return false;
            }
        }

        this.messageFormat = new MineDown(config.getString("messageformat"));
        this.serverName = new MineDown(config.getString("servername", "this server")).toComponent();
        this.saveInterval = config.getLong("saveinterval", 30);

        Configuration langConfig = loadOrCreateYamlConfig("messages.yml");
        if (langConfig == null) {
            getLogger().severe("Could not load language configuration.");
            return false;
        }
        String language = config.getString("language");
        this.localization = loadLocalization(langConfig, language);
        try {
            parser = TimeParser.builder()
                    .addUnit(1, getUnits(langConfig, language, "second"))
                    .addUnit(60, getUnits(langConfig, language, "minute"))
                    .addUnit(60 * 60, getUnits(langConfig, language, "hour"))
                    .addUnit(60 * 60 * 24, getUnits(langConfig, language, "day"))
                    .addUnit(60 * 60 * 24 * 7, getUnits(langConfig, language, "week"))
                    .addUnit(60 * 60 * 24 * 30, getUnits(langConfig, language, "month"))
                    .addUnit(60 * 60 * 24 * 30 * 12, getUnits(langConfig, language, "year"))
                    .build();
        } catch (IllegalArgumentException ex) {
            getLogger().log(Level.SEVERE, "Could not create time parser.", ex);
            return false;
        }
        return true;
    }

    private boolean migrateConfig(int currentConfigVersion) {
        switch (currentConfigVersion) {
            case 0:
                getLogger().warning("Unspecified config version. Creating config backup and resetting everything...");
                return backupAndRecreateConfig();
            default:
                getLogger().severe("Illegal config version! Creating config backup and resetting everything...");
                return backupAndRecreateConfig();
        }
    }

    private boolean backupAndRecreateConfig() {
        Path configFilePath = new File(getDataFolder(), "config.yml").toPath();
        Path messagesFilePath = new File(getDataFolder(), "messages.yml").toPath();
        Path databaseFilePath = new File(getDataFolder(), "database.properties").toPath();
        try {
            Files.move(configFilePath, configFilePath.resolveSibling("config.old.yml"));
            Files.move(messagesFilePath, messagesFilePath.resolveSibling("messages.old.yml"));
            Files.move(databaseFilePath, databaseFilePath.resolveSibling("database.old.properties"));

            this.config = loadOrCreateYamlConfig("config.yml");
            return config != null;
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not backup old configuration, aborting mirgation...", ex);
            return false;
        }
    }

    private Localization loadLocalization(Configuration langConfig, String language) {
        Configuration translationConfig = langConfig.getSection(language);
        Map<String, String> translations = new HashMap<>();
        for (String key : translationConfig.getKeys()) {
            Object part = translationConfig.get(key, null);
            if (part instanceof Configuration) {
                translations.putAll(translationRecursive(translationConfig, key));
            } else if (part instanceof String) {
                translations.put(key, (String) part);
            } // else ignore
        }
        return new Localization(translations);
    }

    private Map<String, String> translationRecursive(Configuration translationConfig, String basePath) {
        Map<String, String> translations = new HashMap<>();
        Configuration currentConfig = basePath.isEmpty() ? translationConfig : translationConfig.getSection(basePath);
        for (String key : currentConfig.getKeys()) {
            Object part = currentConfig.get(key, null);
            if (part instanceof Configuration) {
                translations.putAll(translationRecursive(translationConfig, basePath + "." + key));
            } else if (part instanceof String) {
                translations.put(basePath + "." + key, (String) part);
            } // else ignore
        }
        return translations;
    }

    private String[] getUnits(Configuration langConfig, String language, String unit) {
        String singular = langConfig.getString(language + ".unit." + unit + ".singular");
        String plural = langConfig.getString(language + ".unit." + unit + ".plural");
        List<?> identifier = langConfig.getList(language + ".unit." + unit + ".identifier");
        Set<String> units = new HashSet<>();
        units.add(singular);
        units.add(plural);
        for (Object content : identifier) {
            if (content instanceof String) {
                units.add((String) content);
            } else {
                getLogger().warning("dangerous identifier definition (messages.yml): " + language + ".unit." + unit + "identifier: " + content.toString());
                units.add(content.toString());
            }
        }
        return units.toArray(new String[0]);
    }

    private Configuration loadOrCreateYamlConfig(String configFileName) {
        try {
            File configFile = getOrCreateConfigFile(configFileName);
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException ioe) {
            getLogger().log(Level.SEVERE, "could not load yaml configuration file " + configFileName, ioe);
            return null;
        }
    }

    private File getOrCreateConfigFile(String configFileName) {
        File configFile = new File(getDataFolder(), configFileName);
        if (!configFile.exists()) {
            try (InputStream input = getResourceAsStream(configFileName)) {
                if (input != null) {
                    Files.copy(input, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException ioe) {
                getLogger().log(Level.SEVERE, "could not create configuration file " + configFileName, ioe);
                return null;
            }
        }
        return configFile;
    }

    private boolean loadStorage() {
        try {
            String storageMethod = config.getString("storage", "yaml");
            switch (storageMethod.toLowerCase(Locale.ROOT)) {
                case "yml":
                case "yaml":
                case "file":
                    loadYamlStorage();
                    return true;
                case "sql":
                case "mysql":
                case "mariadb":
                case "db":
                case "database":
                    loadMysqlStorage();
                    return true;
                default:
                    getLogger().severe("illegal storage method " + storageMethod);
                    return false;
            }
        } catch (StorageException ex) {
            getLogger().log(Level.SEVERE, "could not initialize storage", ex);
            return false;
        }
    }

    private void loadMysqlStorage() throws StorageException {
        Properties properties = new Properties();
        File propertiesFile = getOrCreateConfigFile("database.properties");
        try (Reader fileReader = new FileReader(propertiesFile); BufferedReader reader = new BufferedReader(fileReader)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
        DatabaseStorage storage = new DatabaseStorage(properties);
        this.playerNameStorage = storage;
        this.onlineTimeStorage = new AccumulatingOnlineTimeStorage(storage);
    }

    private void loadYamlStorage() throws StorageException {
        this.playerNameStorage = new FilePlayerNameStorage(new BungeeYamlFileStorageProvider(this, "names.yml"));
        this.onlineTimeStorage = new AccumulatingOnlineTimeStorage(new FileOnlineTimeStorage(new BungeeYamlFileStorageProvider(this, "time.yml")));
    }

    private Optional<String> getOptionalPlayerName(UUID uuid) {
        ProxiedPlayer player = getProxy().getPlayer(uuid);
        if (player != null) {
            return Optional.of(player.getName());
        }
        try {
            return playerNameStorage.getName(uuid);
        } catch (StorageException ex) {
            getLogger().log(Level.WARNING, "could not get player name of " + uuid.toString(), ex);
            return Optional.empty();
        }
    }

    private Optional<UUID> getOptionalPlayerUuid(String name) {
        ProxiedPlayer player = getProxy().getPlayer(name);
        if (player != null) {
            return Optional.of(player.getUniqueId());
        }
        try {
            return playerNameStorage.getUuid(name);
        } catch (StorageException ex) {
            getLogger().log(Level.WARNING, "could not get uuid of " + name, ex);
            return Optional.empty();
        }
    }

    private void flushOnlineTimeCache() {
        try {
            onlineTimeStorage.flushOnlineTimeCache();
        } catch (StorageException ex) {
            getLogger().log(Level.SEVERE, "could not flush online time cache", ex);
        }
    }

    @Override
    public PluginScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Optional<PlayerData> findPlayer(String identifier) {
        Optional<UUID> optionalUuid = UuidUtil.fromString(identifier);
        if (optionalUuid.isPresent()) {
            return optionalUuid.map(uuid -> new PlayerData(uuid, getOptionalPlayerName(uuid)));
        } else {
            return getOptionalPlayerUuid(identifier)
                    .map(uuid -> new PlayerData(uuid, Optional.of(identifier)));
        }
    }
}

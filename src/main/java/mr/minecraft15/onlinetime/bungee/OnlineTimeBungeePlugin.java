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
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class OnlineTimeBungeePlugin extends Plugin implements PluginProxy {

    private static final int CONFIG_VERSION = 2;

    private boolean loadSuccessful = false;
    private String mode;

    private Configuration config;

    private PluginScheduler scheduler;

    private MineDown messageFormat;
    private String defaultLanguage;
    private Localization localization;
    private TimeParser parser;

    private BaseComponent[] serverName;

    private long saveInterval;

    private AccumulatingOnlineTimeStorage onlineTimeStorage;
    private PlayerNameStorage playerNameStorage;

    private ScheduledTask flushCacheTask;

    @Override
    public void onLoad() {
        this.scheduler = new BungeeSchedulerAdapter(this, getProxy().getScheduler());

        boolean success = false;
        try {
            success = loadConfig() && loadStorage();
        } finally {
            loadSuccessful = success;
        }
    }

    @Override
    public void onEnable() {
        if (!loadSuccessful) {
            getLogger().severe("Could not enable OnlineTime!");
            return;
        }

        PluginManager pluginManager = getProxy().getPluginManager();
        switch (mode) {
            case "master":
                getProxy().registerChannel("onlinetime:storage");
                pluginManager.registerListener(this, new OnlineTimeStorageRequestListener(this, onlineTimeStorage));
                getLogger().info("Enabled OnlineTimeStorageRequestListener.");
                break;
            case "standalone":
                break;
            default:
                getLogger().severe("invalid mode: " + mode);
                return;
        }

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
        if (onlineTimeStorage != null) {
            try {
                flushCacheTask.cancel();
                flushOnlineTimeCache();
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

    @Override
    public Localization getDefaultLocalization() {
        return localization;
    }

    @Override
    public Optional<Localization> getLocalization(String language) {
        if (Objects.equals(defaultLanguage, language)) {
            return Optional.of(getDefaultLocalization());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Localization getLocalizationFor(PlayerData player) {
        return getDefaultLocalization();
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
        this.mode = config.getString("mode");

        Configuration langConfig = loadOrCreateYamlConfig("messages.yml");
        if (langConfig == null) {
            getLogger().severe("Could not load language configuration.");
            return false;
        }
        defaultLanguage = config.getString("language");
        this.localization = loadLocalization(langConfig, defaultLanguage);
        try {
            parser = TimeParser.builder()
                    .addUnit(1, getUnits(langConfig, defaultLanguage, "second"))
                    .addUnit(60, getUnits(langConfig, defaultLanguage, "minute"))
                    .addUnit(60 * 60, getUnits(langConfig, defaultLanguage, "hour"))
                    .addUnit(60 * 60 * 24, getUnits(langConfig, defaultLanguage, "day"))
                    .addUnit(60 * 60 * 24 * 7, getUnits(langConfig, defaultLanguage, "week"))
                    .addUnit(60 * 60 * 24 * 30, getUnits(langConfig, defaultLanguage, "month"))
                    .addUnit(60 * 60 * 24 * 30 * 12, getUnits(langConfig, defaultLanguage, "year"))
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
            case 1:
                getLogger().warning("Migrating config from version 1...");
                return backupAndKeepConfig()
                        && addModeToConfiguration();
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
            Files.move(configFilePath, configFilePath.resolveSibling("config.old.yml"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(messagesFilePath, messagesFilePath.resolveSibling("messages.old.yml"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(databaseFilePath, databaseFilePath.resolveSibling("database.old.properties"), StandardCopyOption.REPLACE_EXISTING);

            this.config = loadOrCreateYamlConfig("config.yml");
            return config != null;
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not backup old configuration, aborting mirgation...", ex);
            return false;
        }
    }

    private boolean backupAndKeepConfig() {
        Path configFilePath = new File(getDataFolder(), "config.yml").toPath();
        Path messagesFilePath = new File(getDataFolder(), "messages.yml").toPath();
        Path databaseFilePath = new File(getDataFolder(), "database.properties").toPath();
        try {
            Files.copy(configFilePath, configFilePath.resolveSibling("config.old.yml"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(messagesFilePath, messagesFilePath.resolveSibling("messages.old.yml"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(databaseFilePath, databaseFilePath.resolveSibling("database.old.properties"), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not backup old configuration, aborting mirgation...", ex);
            return false;
        }
    }

    private boolean addModeToConfiguration() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            config.set("mode", "standalone");
            config.set("configversion", 2);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
            return true;
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "could not save updated configuration config.yml", ex);
            return false;
        }
    }

    private Localization loadLocalization(Configuration langConfig, String language) {
        Map<String, String> translations = BungeeConfigurationUtil
                .readAllRecursive(langConfig.getSection(language), "").entrySet().stream()
            .filter(entry -> entry.getValue() instanceof String)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue()));
        return new Localization(translations);
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
    public String getName() {
        return getDescription().getName();
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public String getAuthors() {
        return getDescription().getAuthor();
    }

    @Override
    public PluginScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Optional<PlayerData> findPlayer(String identifier) {
        Optional<UUID> optionalUuid = UuidUtil.fromString(identifier);
        if (optionalUuid.isPresent()) {
            return optionalUuid.map(uuid -> new PlayerData(uuid, getOptionalPlayerName(uuid).orElse(null)));
        } else {
            return getOptionalPlayerUuid(identifier)
                    .map(uuid -> new PlayerData(uuid, identifier));
        }
    }
}

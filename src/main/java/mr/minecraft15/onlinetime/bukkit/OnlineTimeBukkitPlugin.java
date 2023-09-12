/*
 * MIT License
 *
 * Copyright (c) 2019 Niklas Seyfarth
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

package mr.minecraft15.onlinetime.bukkit;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.api.*;
import mr.minecraft15.onlinetime.common.*;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class OnlineTimeBukkitPlugin extends JavaPlugin implements PluginProxy {

    private static final int CONFIG_VERSION = 2;

    private boolean loadSuccessful = false;
    private String mode;

    private PluginScheduler scheduler;

    private MineDown messageFormat;
    private String defaultLanguage;
    private Localization localization;
    private TimeParser parser;

    private BaseComponent[] serverName;

    private long saveInterval;
    private String storageMethod;

    private OnlineTimeStorage onlineTimeStorage;
    private OnlineTimeAccumulator onlineTimeAccumulator;
    private PlayerNameStorage playerNameStorage;

    private BukkitTask flushCacheTask;

    @Override
    public void onLoad() {
        this.scheduler = new BukkitSchedulerAdapter(this, getServer().getScheduler());

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
            getLogger().log(Level.SEVERE, "Could not enable OnlineTime!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginManager pluginManager = getServer().getPluginManager();
        switch (mode) {
            case "standalone":
                getCommand("onlinetime").setExecutor(new PluginCommandBukkitAdapter(
                        new OnlineTimeCommand(this, localization, onlineTimeStorage), this));
                getCommand("onlinetimeadmin").setExecutor(new PluginCommandBukkitAdapter(
                        new OnlineTimeAdminCommand(this, localization, parser, onlineTimeStorage), this));
                pluginManager.registerEvents(new PlayerNameBukkitListener(this, playerNameStorage), this);
                pluginManager.registerEvents(new OnlineTimeAccumulatorBukkitListener(this, onlineTimeAccumulator), this);
                flushCacheTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this::flushOnlineTimeCache, saveInterval * 10, saveInterval * 20);
                break;
            case "slave":
                Messenger messenger = getServer().getMessenger();
                BukkitSlavedOnlineTimeStorage slavedStorage = new BukkitSlavedOnlineTimeStorage(this, saveInterval * 20);
                pluginManager.registerEvents(slavedStorage, this);
                messenger.registerOutgoingPluginChannel(this, "onlinetime:storage");
                messenger.registerIncomingPluginChannel(this, "onlinetime:storage", slavedStorage);
                onlineTimeStorage = slavedStorage;
                break;
            default:
                getLogger().log(Level.SEVERE, "Invalid mode: " + mode);
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        getServer().getServicesManager().register(OnlineTimeStorage.class,onlineTimeStorage,this, ServicePriority.Lowest);
        registerPlaceholderApi();
    }

    private boolean loadConfig() {
        getDataFolder().mkdir();
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();

        int currentConfigVersion = config.getInt("configversion", 0);
        if (currentConfigVersion != CONFIG_VERSION) {
            getLogger().warning("Old configuration found! Migrating...");
            boolean success = migrateConfig(currentConfigVersion);
            if (!success) {
                getLogger().severe("Could not migrate from old configuration.");
                return false;
            }
        }

        this.mode = config.getString("mode");
        if (mode == null) {
            getLogger().severe("undefined mode");
            return false;
        }

        this.messageFormat = new MineDown(config.getString("messageformat"));
        this.serverName = new MineDown(config.getString("servername", "this server")).toComponent();
        this.saveInterval = config.getLong("saveinterval", 30);

        switch (mode) {
            case "standalone":
                this.storageMethod = config.getString("storage", "yaml");
                break;
            case "slave":
                this.storageMethod = "none";
                break;
            default:
                getLogger().severe("invalid mode: " + mode);
                return false;
        }

        File translationFile = new File(getDataFolder(), "messages.yml");
        if (!translationFile.exists()) {
            saveResource("messages.yml", false);
        }
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(translationFile);
        langConfig.setDefaults(YamlConfiguration.loadConfiguration(getTextResource("messages.yml")));

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

            saveDefaultConfig();
            reloadConfig();
            return true;
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
        FileConfiguration config = getConfig();
        config.set("mode", "standalone");
        config.set("configversion", 2);
        saveConfig();
        return true;
    }

    private Localization loadLocalization(FileConfiguration langConfig, String language) {
        ConfigurationSection translationConfig = langConfig.getConfigurationSection(language);
        Map<String, String> translations = new HashMap<>();
        for (String key : translationConfig.getKeys(true)) {
            if (translationConfig.isString(key)) {
                translations.put(key, translationConfig.getString(key));
            } // else ignore
        }
        return new Localization(translations);
    }

    private String[] getUnits(FileConfiguration langConfig, String language, String unit) {
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

    private boolean loadStorage() {
        try {
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
                case "none":
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

    private void loadYamlStorage() throws StorageException {
        this.playerNameStorage = new FilePlayerNameStorage(new BukkitYamlFileStorageProvider(this, "names.yml"));
        AccumulatingOnlineTimeStorage accumulatingTimeStorage = new AccumulatingOnlineTimeStorage(new FileOnlineTimeStorage(new BukkitYamlFileStorageProvider(this, "time.yml")));
        this.onlineTimeStorage = accumulatingTimeStorage;
        this.onlineTimeAccumulator = accumulatingTimeStorage;
    }

    private void loadMysqlStorage() throws StorageException {
        Properties properties = new Properties();

        File databasePropertiesFile = new File(getDataFolder(), "database.properties");
        if (!databasePropertiesFile.exists()) {
            saveResource("database.properties", false);
        }

        try (Reader fileReader = new FileReader(databasePropertiesFile);
             BufferedReader reader = new BufferedReader(fileReader)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
        DatabaseStorage storage = new DatabaseStorage(properties);
        this.playerNameStorage = storage;
        AccumulatingOnlineTimeStorage accumulatingTimeStorage = new AccumulatingOnlineTimeStorage(storage);
        this.onlineTimeStorage = accumulatingTimeStorage;
        this.onlineTimeAccumulator = accumulatingTimeStorage;
    }

    private void flushOnlineTimeCache() {
        try {
            onlineTimeAccumulator.flushOnlineTimeCache();
        } catch (StorageException ex) {
            getLogger().log(Level.SEVERE, "could not flush online time cache", ex);
        }
    }

    private void registerPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OnlineTimePlaceholderExpansion(this, onlineTimeStorage).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }
    }

    @Override
    public void onDisable() {
        if (onlineTimeAccumulator != null) {
            try {
                flushCacheTask.cancel();
                flushOnlineTimeCache();
                onlineTimeAccumulator.close();
            } catch (StorageException ex) {
                getLogger().log(Level.SEVERE, "error while closing online time accumulator", ex);
            }
        }
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

    private Optional<String> getOptionalPlayerName(UUID uuid) {
        Player player = getServer().getPlayer(uuid);
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
        Player player = getServer().getPlayerExact(name);
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

    @Override
    public MineDown getFormattedMessage(MineDown rawMessage) {
        return messageFormat.copy()
                .replace("message", rawMessage
                    .replace("server", serverName)
                    .toComponent());
    }

    @Override
    public String getAuthors() {
        return getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
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
}

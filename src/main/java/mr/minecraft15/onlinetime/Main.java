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

package mr.minecraft15.onlinetime;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.command.OnlineTimeAdminCommand;
import mr.minecraft15.onlinetime.command.OnlineTimeCommand;
import mr.minecraft15.onlinetime.listener.PlayerListener;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Main extends Plugin {

    public static final Pattern UUID_PATTERN = Pattern.compile("([0-9a-f]{8})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})", Pattern.CASE_INSENSITIVE);
    private static final int CONFIG_VERSION = 1;

    public final ConcurrentMap<UUID, Long> onlineSince = new ConcurrentHashMap<>();

    private Configuration config;

    private MineDown messageFormat;
    private Lang lang;
    private TimeParser parser;

    private BaseComponent[] serverName;

    private long saveInterval;

    private OnlineTimeStorage onlineTimeStorage;
    private PlayerNameStorage playerNameStorage;

    @Override
    public void onEnable() {
        if (!(loadConfig() && loadStorage())) {
            getProxy().getLogger().log(Level.SEVERE, "Could not enable OnlineTime!");
            return;
        }

        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerCommand(this, new OnlineTimeCommand(this, lang));
        pluginManager.registerCommand(this, new OnlineTimeAdminCommand(this, lang, parser));
        pluginManager.registerListener(this, new PlayerListener(this, playerNameStorage));
        getProxy().getScheduler().schedule(this, this::flushOnlineTimeCache, saveInterval / 2L, saveInterval, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
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

    public MineDown getFormattedMessage(BaseComponent... rawMessage) {
        return messageFormat.copy().replace("message", rawMessage);
    }

    public BaseComponent[] getServerName() {
        return Arrays.copyOf(serverName, serverName.length);
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
        String defaultLanguage = config.getString("language");
        this.lang = new Lang(langConfig, defaultLanguage);
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
        DatabaseStorage storage = new DatabaseStorage(this, properties);
        this.playerNameStorage = storage;
        this.onlineTimeStorage = storage;
    }

    private void loadYamlStorage() throws StorageException {
        this.playerNameStorage = new YamlPlayerNameStorage(this, "names.yml", saveInterval);
        this.onlineTimeStorage = new YamlOnlineTimeStorage(this,"time.yml", saveInterval);
    }

    public String getPlayerName(UUID uuid) {
        ProxiedPlayer player = getProxy().getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        try {
            return playerNameStorage.getName(uuid).orElse(null);
        } catch (StorageException ex) {
            getLogger().log(Level.WARNING, "could not get player name of " + uuid.toString(), ex);
            return null;
        }
    }

    public UUID getPlayerUuid(String name) {
        ProxiedPlayer player = getProxy().getPlayer(name);
        if (player != null) {
            return player.getUniqueId();
        }
        try {
            return playerNameStorage.getUuid(name).orElse(null);
        } catch (StorageException ex) {
            getLogger().log(Level.WARNING, "could not get uuid of " + name, ex);
            return null;
        }
    }

    public long getOnlineTime(String playerName) {
        UUID uuid = getPlayerUuid(playerName);
        if (uuid != null) {
            return getOnlineTime(uuid);
        } else {
            return 0;
        }
    }

    public long getOnlineTime(ProxiedPlayer player) {
        return getOnlineTime(player.getUniqueId());
    }

    public long getOnlineTime(UUID uuid) {
        try {
            long currentOnlineTime = onlineSince.containsKey(uuid) ? (System.currentTimeMillis() - onlineSince.get(uuid)) / 1000 : 0;
            return currentOnlineTime + onlineTimeStorage.getOnlineTime(uuid).orElse(0L);
        } catch (StorageException ex) {
            getLogger().log(Level.WARNING, "could not get online time of " + uuid.toString(), ex);
            return 0;
        }
    }

    public boolean modifyOnlineTime(String playerName, long modifyBy) {
        UUID uuid = getPlayerUuid(playerName);
        if (uuid != null) {
            return modifyOnlineTime(uuid, modifyBy);
        } else {
            return false;
        }
    }

    public boolean modifyOnlineTime(ProxiedPlayer player, long modifyBy) {
        return modifyOnlineTime(player.getUniqueId(), modifyBy);
    }

    public boolean modifyOnlineTime(UUID uuid, final long modifyBy) {
        if (null != onlineSince.computeIfPresent(uuid, (key, value) -> value + modifyBy)) {
            return true;
        } else {
            try {
                onlineTimeStorage.addOnlineTime(uuid, modifyBy);
                return true;
            } catch (StorageException ex) {
                getLogger().log(Level.SEVERE, "could not modify online time of " + uuid.toString(), ex);
                return false;
            }
        }
    }

    public void saveOnlineTimeAfterDisconnect(UUID uuid) {
        if (onlineSince.containsKey(uuid)) {
            Long from = onlineSince.remove(uuid);
            if (from == null) return; // concurrent change
            long currentOnlineTime = (System.currentTimeMillis() - from) / 1000;
            try {
                onlineTimeStorage.addOnlineTime(uuid, currentOnlineTime);
            } catch (StorageException ex) {
                getLogger().log(Level.SEVERE, "could not save online time of " + uuid.toString(), ex);
            }
        }
    }

    public void flushOnlineTimeCache() {
        if (onlineSince.isEmpty()) {
            return;
        }
        final Map<UUID, Long> onlineTime = new HashMap<>();
        final long now = System.currentTimeMillis();
        onlineSince.keySet().forEach(uuid -> {
            Long from = onlineSince.replace(uuid, now);
            if (from != null) { // protect from concurrent change
                onlineTime.put(uuid, (now - from) / 1000);
            }
        });
        try {
            onlineTimeStorage.addOnlineTimes(onlineTime);
        } catch (StorageException ex) {
            getLogger().log(Level.SEVERE, "could not flush online time cache", ex);
        }
    }
}

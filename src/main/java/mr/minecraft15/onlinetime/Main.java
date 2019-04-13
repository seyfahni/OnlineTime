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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Main extends Plugin {
    public final ConcurrentMap<UUID, Long> onlineSince = new ConcurrentHashMap<>();

    private Configuration config;

    private MineDown messageFormat;
    private Lang lang;
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
        pluginManager.registerCommand(this, new OnlineTimeCommand(this, lang, serverName));
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

    private boolean loadConfig() {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            this.config = loadOrCreateYamlConfig("config.yml");
            if (config == null) {
                return false;
            }

            this.messageFormat = new MineDown(config.getString("messageformat"));
            this.serverName = new MineDown(config.getString("servername", "this server")).toComponent();
            this.saveInterval = config.getLong("saveinterval", 30);

            Configuration langConfig = loadOrCreateYamlConfig("messages.yml");
            this.lang = new Lang(langConfig, config.getString("language"));
            return lang != null;
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
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }
}

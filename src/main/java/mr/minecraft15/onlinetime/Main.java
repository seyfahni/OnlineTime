package mr.minecraft15.onlinetime;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.command.OnlineTimeCommand;
import mr.minecraft15.onlinetime.listener.PlayerListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class Main extends Plugin {
    private static Plugin instance;
    public static HashMap<UUID, Long> onlineSince = new HashMap<>();

    private Configuration config;

    private MineDown messageFormat;
    private Lang lang;
    private String serverName;

    private long saveInterval;

    private OnlineTimeStorage onlineTimeStorage;
    private PlayerNameStorage playerNameStorage;

    public Main() {
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!(loadConfig() && loadStorage())) {
            getProxy().getLogger().log(Level.SEVERE, "Could not enable OnlineTime!");
            return;
        }

        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerCommand(this, new OnlineTimeCommand(this, lang, serverName));
        pluginManager.registerListener(this, new PlayerListener(this, playerNameStorage));
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
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

    public static Plugin getInstance() {
        return instance;
    }

    public MineDown getFormattedMessage(String rawMessage) {
        return messageFormat.copy().replace("%message%", rawMessage);
    }

    private boolean loadConfig() {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            this.config = loadOrCreateConfigFile("config.yml");
            if (config == null) {
                return false;
            }

            this.messageFormat = new MineDown(config.getString("messageformat"));
            this.serverName = config.getString("servername", "this server");
            this.saveInterval = config.getLong("saveinterval", 30);

            Configuration langConfig = loadOrCreateConfigFile("messages.yml");
            this.lang = new Lang(langConfig, config.getString("language"));
            return lang != null;
    }

    private Configuration loadOrCreateConfigFile(String configFileName) {
        try {
            File configFile = new File(getDataFolder(), configFileName);
            if (!configFile.exists()) {
                Files.copy(getResourceAsStream(configFileName), configFile.toPath());
            }
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException ioe) {
            getLogger().log(Level.SEVERE, "could not load configuration file " + configFileName, ioe);
            return null;
        }
    }

    private boolean loadStorage() {
        try {
            if (config.getBoolean("mysql.enabled", false)) {
                loadMysqlStorage();
            } else {
                loadYamlStorage();
            }
            return true;
        } catch (StorageException ex) {
            getLogger().log(Level.SEVERE, "could not initialize storage", ex);
            return false;
        }
    }

    private void loadMysqlStorage() throws StorageException {
        String host = config.getString("mysql.host", "127.0.0.1");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "minecraft_database");
        String username = config.getString("mysql.username", "minecraft_user");
        String password = config.getString("mysql.password", "password");
        MysqlStorage storage = new MysqlStorage(this, host, port, database, username, password);
        this.playerNameStorage = storage;
        this.onlineTimeStorage = storage;
    }

    private void loadYamlStorage() throws StorageException {
        this.playerNameStorage = new YamlPlayerNameStorage(this, "names.yml", saveInterval);
        this.onlineTimeStorage = new YamlOnlineTimeStorage(this,"time.yml", saveInterval);
    }

    public long getOnlineTime(String playerName) {
        try {
            ProxiedPlayer player = getProxy().getPlayer(playerName);
            if (player != null) {
                return getOnlineTime(player);
            } else {
                Optional<UUID> optionalUuid = playerNameStorage.getUuid(playerName);
                if (!optionalUuid.isPresent()) {
                    return 0;
                }
                return getOnlineTime(optionalUuid.get());
            }
        } catch (StorageException ex) {
            getLogger().log(Level.WARNING, "could not get uuid of " + playerName, ex);
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

    public void saveOnlineTime(UUID uuid) {
        if (onlineSince.containsKey(uuid)) {
            long currentOnlineTime = (System.currentTimeMillis() - onlineSince.get(uuid)) / 1000;
            onlineSince.remove(uuid);
            try {
                onlineTimeStorage.addOnlineTime(uuid, currentOnlineTime);
            } catch (StorageException ex) {
                getLogger().log(Level.SEVERE, "could not save online time of " + uuid.toString(), ex);
            }
        }
    }
}

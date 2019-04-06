package mr.minecraft15.OnlineTime.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

import mr.minecraft15.OnlineTime.Commands.OnlineTime;
import mr.minecraft15.OnlineTime.Listener.PlayerListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Main extends Plugin {
    private static Plugin instance;
    private static File file;
    private static Configuration cfg;
    private static String prefix;
    public static HashMap<UUID, Long> onlineSince = new HashMap<UUID, Long>();

    @Override
    public void onEnable() {
	instance = this;
	prefix = "�7[�3OnlineTime�7] ";

	loadConfig();

	Lang.lang = Lang.cfg.getString("Language");

	PluginManager PM = getProxy().getPluginManager();
	PM.registerCommand(Main.getInstance(), new OnlineTime());
	PM.registerListener(Main.getInstance(), new PlayerListener());
    }

    public static Plugin getInstance() {
	return instance;
    }

    public static String getPrefix() {
	return prefix;
    }

    public static void loadConfig() {
	try {
	    if (!Main.getInstance().getDataFolder().exists()) {
		Main.getInstance().getDataFolder().mkdir();
	    }

	    file = new File(Main.getInstance().getDataFolder(), "time.yml");
	    if (!file.exists()) {
		Files.copy(Main.getInstance().getResourceAsStream("time.yml"), file.toPath(), new CopyOption[0]);
	    }
	    cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

	    Names.file = new File(Main.getInstance().getDataFolder(), "names.yml");
	    if (!Names.file.exists()) {
		Files.copy(Main.getInstance().getResourceAsStream("names.yml"), Names.file.toPath(), new CopyOption[0]);
	    }
	    Names.cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(Names.file);

	    Lang.file = new File(Main.getInstance().getDataFolder(), "config.yml");
	    if (!Lang.file.exists()) {
		Files.copy(Main.getInstance().getResourceAsStream("config.yml"), Lang.file.toPath(), new CopyOption[0]);
	    }
	    Lang.cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(Lang.file);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void save() {
	try {
	    ConfigurationProvider.getProvider(YamlConfiguration.class).save(cfg, file);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public static void load() {
	try {
	    cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public static Configuration getConfig() {
	return cfg;
    }

    public static String getTime(long seconds) {
	long sec = 0, min = 0, h = 0, d = 0, w = 0, m = 0, y = 0;
	for (long i = 0; i < seconds; ++i) {
	    ++sec;
	    if (sec >= 60) {
		min++;
		sec = 0;
	    }
	    if (min >= 60) {
		h++;
		min = 0;
	    }
	    if (h >= 24) {
		d++;
		h = 0;
	    }
	    if (d >= 7) {
		w++;
		d = 0;
	    }
	    if (w >= 4) {
		m++;
		w = 0;
	    }
	    if (m >= 12) {
		y++;
		m = 0;
	    }
	}
	String secStr = (sec != 0)
		? sec + " " + ((sec == 1) ? Lang.getMessage("Time_Second") : Lang.getMessage("Time_Seconds"))
		: null,
		minStr = (min != 0)
			? min + " " + ((min == 1) ? Lang.getMessage("Time_Minute") : Lang.getMessage("Time_Minutes"))
			: null,
		hStr = (h != 0) ? h + " " + ((h == 1) ? Lang.getMessage("Time_Hour") : Lang.getMessage("Time_Hours"))
			: null,
		dStr = (d != 0) ? d + " " + ((d == 1) ? Lang.getMessage("Time_Day") : Lang.getMessage("Time_Days"))
			: null,
		wStr = (w != 0) ? w + " " + ((w == 1) ? Lang.getMessage("Time_Week") : Lang.getMessage("Time_Weeks"))
			: null,
		mStr = (m != 0) ? m + " " + ((m == 1) ? Lang.getMessage("Time_Month") : Lang.getMessage("Time_Months"))
			: null,
		yStr = (y != 0) ? y + " " + ((y == 1) ? Lang.getMessage("Time_Year") : Lang.getMessage("Time_Years"))
			: null;
	String r = (yStr == null ? "" : yStr + " ") + (mStr == null ? "" : mStr + " ")
		+ (wStr == null ? "" : wStr + " ") + (dStr == null ? "" : dStr + " ") + (hStr == null ? "" : hStr + " ")
		+ (minStr == null ? "" : minStr + " ") + (secStr == null ? "" : secStr + " ");
	return r.substring(0, r.length() - 1);
    }

    public static long getOnlineTime(ProxiedPlayer player) {
	return getOnlineTime(player.getUniqueId());
    }

    public static long getOnlineTime(UUID uuid) {
	long time = Main.getConfig().getInt(uuid.toString());
	if (onlineSince.containsKey(uuid)) {
	    time += (System.currentTimeMillis() - onlineSince.get(uuid)) / 1000;
	}
	return time;
    }

    public static long getOnlineTime(String player) {
	String uuid;
	long time = 0;
	ProxiedPlayer p = ProxyServer.getInstance().getPlayer(player);
	if (p == null) {
	    uuid = Names.getUUIDByName(player);
	} else {
	    uuid = p.getUniqueId().toString();
	    if (onlineSince.containsKey(UUID.fromString(uuid))) {
		time += (System.currentTimeMillis() - onlineSince.get(UUID.fromString(uuid))) / 1000;
	    }
	}
	if (uuid == null) {
	    return 0;
	}
	time += Main.getConfig().getInt(uuid);
	return time;
    }

    public static void saveOnlineTime(UUID uuid) {
	if (onlineSince.containsKey(uuid)) {
	    long time = 0;
	    try {
		time += Main.getConfig().getInt(uuid.toString());
	    } catch (Exception e) {
	    }
	    time += (System.currentTimeMillis() - onlineSince.get(uuid)) / 1000;
	    Main.getConfig().set(uuid.toString(), time);
	    Main.save();
	    onlineSince.remove(uuid);
	} else {
	    System.err.println("Unable to save onlinetime from player " + uuid.toString() + "!");
	}
    }

    public static void saveOnlineTime(String uuid) {
	saveOnlineTime(UUID.fromString(uuid));
    }
}

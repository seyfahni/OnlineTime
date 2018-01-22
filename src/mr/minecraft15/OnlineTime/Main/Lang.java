package mr.minecraft15.OnlineTime.Main;

import java.io.File;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

public class Lang {
	public static File file;
	public static Configuration cfg;
	static String lang;
	
	public static String getMessage(String message) {
		return ChatColor.translateAlternateColorCodes('&', cfg.getString("Lang." + lang + "." + message));
	}
}

package mr.minecraft15.OnlineTime.Main;

import java.io.File;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Names {
	public static File file;
	public static Configuration cfg;
	
	public static void save() {
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(cfg, file);
		} catch (Exception e) {
		}
	}
	
	public static String getUUIDByName(String name) {
		try {
			return cfg.getString(name);
		} catch(Exception e) {
			return null;
		}
	}
}

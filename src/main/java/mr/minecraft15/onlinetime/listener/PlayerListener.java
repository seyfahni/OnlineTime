package mr.minecraft15.onlinetime.listener;

import mr.minecraft15.onlinetime.Main;
import mr.minecraft15.onlinetime.Names;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerLogin(PostLoginEvent e) {
	ProxiedPlayer p = e.getPlayer();
	Names.cfg.set(p.getName(), p.getUniqueId().toString());
	Names.save();

	Main.onlineSince.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
	ProxiedPlayer p = e.getPlayer();
	Main.saveOnlineTime(p.getUniqueId());
    }
}

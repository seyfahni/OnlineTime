package mr.minecraft15.onlinetime.listener;

import mr.minecraft15.onlinetime.Main;
import mr.minecraft15.onlinetime.PlayerNameStorage;
import mr.minecraft15.onlinetime.StorageException;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Level;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final PlayerNameStorage nameStorage;

    public PlayerListener(Main plugin, PlayerNameStorage nameStorage) {
        this.plugin = plugin;
        this.nameStorage = nameStorage;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();
        try {
            nameStorage.setEntry(p.getUniqueId(), p.getName());
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.WARNING, "could not save player name and uuid", ex);
        }
        plugin.onlineSince.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        ProxiedPlayer p = e.getPlayer();
        plugin.saveOnlineTime(p.getUniqueId());
    }
}

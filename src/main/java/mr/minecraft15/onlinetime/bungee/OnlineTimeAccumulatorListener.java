package mr.minecraft15.onlinetime.bungee;

import mr.minecraft15.onlinetime.common.AccumulatingOnlineTimeStorage;
import mr.minecraft15.onlinetime.common.StorageException;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.logging.Level;

public class OnlineTimeAccumulatorListener implements Listener {

    private final Plugin plugin;
    private final AccumulatingOnlineTimeStorage timeStorage;

    public OnlineTimeAccumulatorListener(Plugin plugin, AccumulatingOnlineTimeStorage timeStorage) {
        this.plugin = plugin;
        this.timeStorage = timeStorage;
    }

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                timeStorage.registerOnlineTimeStart(uuid, now);
            } catch (StorageException ex) {
                plugin.getLogger().log(Level.WARNING, "could not start accumulating online time for player " + uuid, ex);
            }
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                timeStorage.saveOnlineTimeAfterDisconnect(uuid, now);
            } catch (StorageException ex) {
                plugin.getLogger().log(Level.WARNING, "error while stopping accumulation of online time for player " + uuid, ex);
            }
        });
    }
}

package mr.minecraft15.onlinetime.command;

import mr.minecraft15.onlinetime.Lang;
import mr.minecraft15.onlinetime.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class OnlineTimeCommand extends Command {

    private final Main plugin;
    private final Lang lang;
    private final String serverName;

    public OnlineTimeCommand(Main plugin, Lang lang, String serverName) {
        super("onlinetime", null, "onlinet", "otime", "ot");
        this.plugin = plugin;
        this.lang = lang;
        this.serverName = serverName;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String playerName;
            if (args.length == 1) {
                playerName = args[0];
            } else if (sender instanceof ProxiedPlayer){
                playerName = sender.getName();
            } else {
                sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("message.command.onlinetime.usage")).toComponent());
                return;
            }
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                long time = plugin.getOnlineTime(playerName);
                if (time == 0) {
                    sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("message.command.onlinetime.nofound"))
                            .replace("%player%", playerName, "%server%", serverName).toComponent());
                } else {
                    if (Objects.equals(sender.getName(), playerName)) {
                        sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("message.command.onlinetime.timeseen.self"))
                                .replace("%server%", serverName, "%time%", formatTime(time)).toComponent());
                    } else {
                        sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("message.command.onlinetime.timeseen.other"))
                                .replace("%player%", playerName, "%server%", serverName, "%time%", formatTime(time)).toComponent());
                    }
                }
            });
        } else {
            sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("message.command.onlinetime.usage")).toComponent());
        }
    }

    private String formatTime(long seconds) {
        long sec = seconds;

        long min = sec / 60;
        sec %= 60;

        long h = min / 60;
        min %= 60;

        long d = h / 24;
        h %= 24;

        long m = d / 30;
        d %= 30;

        long y = m / 12;
        m %= 12;

        long w = d / 7;
        d %= 7;

        String secStr = sec != 0 ? sec + " " + (sec == 1 ? lang.getMessage("unit.second.singular") : lang.getMessage("unit.second.plural")) : null;
        String minStr = min != 0 ? min + " " + (min == 1 ? lang.getMessage("unit.minute.singular") : lang.getMessage("unit.minute.plural")) : null;
        String hStr   = h   != 0 ? h   + " " + (h   == 1 ? lang.getMessage("unit.hour.singular")   : lang.getMessage("unit.hour.plural"))   : null;
        String dStr   = d   != 0 ? d   + " " + (d   == 1 ? lang.getMessage("unit.day.singular")    : lang.getMessage("unit.day.plural"))    : null;
        String wStr   = w   != 0 ? w   + " " + (w   == 1 ? lang.getMessage("unit.week.singular")   : lang.getMessage("unit.week.plural"))   : null;
        String mStr   = m   != 0 ? m   + " " + (m   == 1 ? lang.getMessage("unit.month.singular")  : lang.getMessage("unit.month.plural"))  : null;
        String yStr   = y   != 0 ? y   + " " + (y   == 1 ? lang.getMessage("unit.year.singular")   : lang.getMessage("unit.year.plural"))   : null;

        String r = (yStr   == null ? "" : yStr   + " ")
                + (mStr   == null ? "" : mStr   + " ")
                + (wStr   == null ? "" : wStr   + " ")
                + (dStr   == null ? "" : dStr   + " ")
                + (hStr   == null ? "" : hStr   + " ")
                + (minStr == null ? "" : minStr + " ")
                + (secStr == null ? "" : secStr + " ");
        return r.substring(0, r.length() - 1);
    }
}

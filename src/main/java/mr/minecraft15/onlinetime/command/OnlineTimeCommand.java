package mr.minecraft15.onlinetime.command;

import mr.minecraft15.onlinetime.Lang;
import mr.minecraft15.onlinetime.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

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
        if (args.length == 1) {
            long time = plugin.getOnlineTime(args[0]);
            if (time == 0) {
                sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("Not_Online"))
                        .replace("%player%", args[0], "%server%", serverName).toComponent());
            } else {
                sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("Command_OnlineTime"))
                        .replace("%player%", args[0], "%server%", serverName, "%time%", formatTime(time)).toComponent());
            }
        } else {
            sender.sendMessage(plugin.getFormattedMessage(lang.getMessage("Command_Usage")).toComponent());
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

        String secStr = sec != 0 ? sec + " " + (sec == 1 ? lang.getMessage("Time_Second") : lang.getMessage("Time_Seconds")) : null;
        String minStr = min != 0 ? min + " " + (min == 1 ? lang.getMessage("Time_Minute") : lang.getMessage("Time_Minutes")) : null;
        String hStr   = h   != 0 ? h   + " " + (h   == 1 ? lang.getMessage("Time_Hour")   : lang.getMessage("Time_Hours"))   : null;
        String dStr   = d   != 0 ? d   + " " + (d   == 1 ? lang.getMessage("Time_Day")    : lang.getMessage("Time_Days"))    : null;
        String wStr   = w   != 0 ? w   + " " + (w   == 1 ? lang.getMessage("Time_Week")   : lang.getMessage("Time_Weeks"))   : null;
        String mStr   = m   != 0 ? m   + " " + (m   == 1 ? lang.getMessage("Time_Month")  : lang.getMessage("Time_Months"))  : null;
        String yStr   = y   != 0 ? y   + " " + (y   == 1 ? lang.getMessage("Time_Year")   : lang.getMessage("Time_Years"))   : null;

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

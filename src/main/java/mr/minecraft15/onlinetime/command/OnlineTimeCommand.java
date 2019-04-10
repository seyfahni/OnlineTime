package mr.minecraft15.onlinetime.command;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.Lang;
import mr.minecraft15.onlinetime.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnlineTimeCommand extends Command {

    private static final Pattern UUID_PATTERN = Pattern.compile("([0-9a-f]{8})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})", Pattern.CASE_INSENSITIVE);

    private final Main plugin;
    private final Lang lang;
    private final BaseComponent[] serverName;

    public OnlineTimeCommand(Main plugin, Lang lang, BaseComponent[] serverName) {
        super("onlinetime", null, "onlinet", "otime", "ot");
        this.plugin = plugin;
        this.lang = lang;
        this.serverName = serverName;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onlinetime.see")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                final String playerName;
                final UUID uuid;
                if (args.length == 1) {
                    Matcher uuidMatcher = UUID_PATTERN.matcher(args[0]);
                    if (uuidMatcher.matches()) {
                        uuid = UUID.fromString(uuidMatcher.group(1) + "-" + uuidMatcher.group(2) + "-" + uuidMatcher.group(3) + "-" + uuidMatcher.group(4) + "-" + uuidMatcher.group(5));
                        playerName = plugin.getPlayerName(uuid);
                    } else {
                        playerName = args[0];
                        uuid = plugin.getPlayerUuid(playerName);
                    }
                } else if (sender instanceof ProxiedPlayer){
                    playerName = sender.getName();
                    uuid = ((ProxiedPlayer) sender).getUniqueId();
                } else {
                    printUtilityMessage(sender, "message.command.onlinetime.usage");
                    return;
                }
                long time;
                if (uuid != null) {
                    time = plugin.getOnlineTime(uuid);
                } else {
                    time = plugin.getOnlineTime(playerName);
                }
                String playerRepresentation = playerName != null ? playerName : uuid.toString();
                if (time == 0) {
                    sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetime.notfound"))
                            .replace("server", serverName)
                            .replace("player", playerRepresentation)
                            .toComponent()).toComponent());
                } else {
                    if (Objects.equals(sender.getName(), playerName)) {
                        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetime.timeseen.self"))
                                .replace("server", serverName)
                                .replace("time", formatTime(time))
                                .toComponent()).toComponent());
                    } else if(sender.hasPermission("onlinetime.see.other")) {
                        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetime.timeseen.other"))
                                .replace("server", serverName)
                                .replace("player", playerRepresentation, "time", formatTime(time))
                                .toComponent()).toComponent());
                    } else {
                        printUtilityMessage(sender, "message.nopermission");
                    }
                }
            });
        } else {
            printUtilityMessage(sender, "message.command.onlinetime.usage");
        }
    }

    private void printUtilityMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage(messageKey)).toComponent()).toComponent());
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

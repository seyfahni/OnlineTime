package mr.minecraft15.onlinetime.command;

import mr.minecraft15.onlinetime.Lang;
import mr.minecraft15.onlinetime.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class OnlineTime extends Command {
    public OnlineTime() {
	super("onlinetime", null, new String[] { "onlinet", "otime", "ot" });
    }

    @Override
    public void execute(CommandSender s, String[] a) {
	if (a.length == 0) {
	    s.sendMessage(Main.getPrefix() + Lang.getMessage("Command_Usage"));
	} else {
	    long time = Main.getOnlineTime(a[0]);
	    if (time == 0) {
		s.sendMessage(Main.getPrefix() + Lang.getMessage("Not_Online"));
	    } else {
		s.sendMessage(Main.getPrefix() + Lang.getMessage("Command_OnlineTime").replace("%player%", a[0])
			.replace("%time%", Main.getTime(time)));
	    }
	}
    }
}

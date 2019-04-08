package mr.minecraft15.onlinetime.command;

import de.themoep.minedown.MineDown;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class TestMineDownCommand extends Command {

    public TestMineDownCommand() {
        super("minedown", null, "md");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(new MineDown(String.join(" ", args)).toComponent());
    }
}

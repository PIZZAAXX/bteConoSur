package pizzaaxx.bteconosur.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;

public class HeightCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public HeightCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;

        p.sendMessage(plugin.getPrefix() + "Tu altitud actual es §a" + p.getLocation().getBlockY() + "§f.");

        return true;
    }
}

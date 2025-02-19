package pizzaaxx.bteconosur.WorldEdit.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;

import java.sql.SQLException;

public class IncrementCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public IncrementCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 1) {
            p.sendMessage(plugin.getWorldEdit().getPrefix() + "Introduce una cantidad.");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[0]);
            try {
                plugin.getPlayerRegistry().get(p.getUniqueId()).getWorldEditManager().setIncrement(amount);
                p.sendMessage(plugin.getWorldEdit().getPrefix() + "Incremento establecido en §a" + amount + "§f.");
            } catch (SQLException e) {
                p.sendMessage(plugin.getWorldEdit().getPrefix() + "§cHa ocurrido un error.");
            }
        } catch (NumberFormatException e) {
            p.sendMessage(plugin.getWorldEdit().getPrefix() + "Introduce un número válido.");
        }

        return true;
    }
}

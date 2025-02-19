package pizzaaxx.bteconosur.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.net.MalformedURLException;
import java.net.URL;

public class StreamingCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public StreamingCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        if (args.length == 0) {
            p.sendMessage(plugin.getPrefix() + "Introduce un enlace.");
            return true;
        }

        try {
            new URL(args[0]);
        } catch (MalformedURLException e) {
            p.sendMessage(plugin.getPrefix() + "Introduce un enlace válido.");
            return true;
        }

        return true;
    }
}

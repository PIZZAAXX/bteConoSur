package pizzaaxx.bteconosur.Commands.Managing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class ReloadPlayerCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public ReloadPlayerCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(plugin.getPrefix() + "Este comando solo puede ser ejecutado en la consola.");
            return true;
        }

        ConsoleCommandSender console = (ConsoleCommandSender) sender;

        if (args.length < 1) {
            console.sendMessage(plugin.getPrefix() + "Introduce un nombre.");
            return true;
        }

        try {
            ServerPlayer s = plugin.getPlayerRegistry().get(args[0]);
            UUID uuid = s.getUUID();
            plugin.getPlayerRegistry().unload(uuid);
            plugin.getPlayerRegistry().load(uuid);
            console.sendMessage(plugin.getPrefix() + "Jugador recargado.");
        } catch (SQLException | IOException e) {
            console.sendMessage(plugin.getPrefix() + "Database error.");
        }

        return true;
    }
}

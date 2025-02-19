package pizzaaxx.bteconosur.Commands.Managing;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import pizzaaxx.bteconosur.BTEConoSur;

import java.io.File;
import java.util.UUID;

public class DeletePlayerDataCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public DeletePlayerDataCommand(BTEConoSur plugin) {
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
            console.sendMessage(plugin.getPrefix() + "Introduce una UUID.");
            return true;
        }

        String uuid = args[0];
        if (!uuid.matches("[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")) {
            console.sendMessage(plugin.getPrefix() + "Introduce una UUID válida.");
            return true;
        }

        UUID playerUUID = UUID.fromString(uuid);
        if (Bukkit.getOfflinePlayer(playerUUID).isOnline()) {
            console.sendMessage(plugin.getPrefix() + "No puedes eliminar la información de un jugador online.");
            return true;
        }

        File playerDataFile = new File(plugin.getWorld().getWorldFolder(), "playerdata/" + uuid + ".dat");
        if (!playerDataFile.exists()) {
            console.sendMessage(plugin.getPrefix() + "El archivo de este jugador no existe.");
            return true;
        }

        if (playerDataFile.delete()) {
            console.sendMessage(plugin.getPrefix() + "Archivo eliminado correctamente.");
        } else {
            console.sendMessage(plugin.getPrefix() + "No se ha podido eliminar el archivo.");
        }
        return true;
    }
}

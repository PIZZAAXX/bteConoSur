package pizzaaxx.bteconosur.Commands.Managing;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DonatorCommand implements CommandExecutor, TabCompleter {

    private final BTEConoSur plugin;

    public DonatorCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (!(sender instanceof ConsoleCommandSender)) {

            if (!(sender instanceof Player)) {
                return true;
            }

            Player p = (Player) sender;
            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(p.getUniqueId());

            if (!serverPlayer.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
                p.sendMessage(plugin.getPrefix() + "No puedes hacer esto.");
                return true;
            }

        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getPrefix() + "Introduce el nombre de un jugador.");
            return true;
        }

        try {
            ServerPlayer s = plugin.getPlayerRegistry().get(args[0]);

            if (s == null) {
                sender.sendMessage(plugin.getPrefix() + "El jugador introducido no existe.");
                return true;
            }

            if (s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.DONADOR)) {
                s.removeSecondaryRole(ServerPlayer.SecondaryRoles.DONADOR);
                s.sendNotification(
                        plugin.getPrefix() + "Has perdido el rol de §dDonador§f.",
                        "**[ROLES]** » Has perdido el rol de **Donador**."
                );
                sender.sendMessage(plugin.getPrefix() + "Has quitado el rol de §dDonador§f de §a" + s.getName() + "§f.");
            } else {
                s.addSecondaryRole(ServerPlayer.SecondaryRoles.DONADOR);
                s.sendNotification(
                        plugin.getPrefix() + "¡Has conseguido el rol de §dDonador§f!",
                        "**[ROLES]** » ¡Has conseguido el rol de **Donador**!"
                );
                sender.sendMessage(plugin.getPrefix() + "Has dado el rol de §dDonador§f a §a" + s.getName() + "§f.");
            }


        } catch (SQLException | IOException e) {
            sender.sendMessage(plugin.getPrefix() + "Ha ocurrido un error en la base de datos.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        List<String> finalCompletions = new ArrayList<>();
        for (String completion : completions) {
            if (completion.startsWith(args[args.length - 1])) {
                finalCompletions.add(completion);
            }
        }
        Collections.sort(finalCompletions);
        return finalCompletions;
    }
}

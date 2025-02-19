package pizzaaxx.bteconosur.WorldEdit.Presets;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Player.Managers.WorldEditManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.sql.SQLException;
import java.util.*;

public class PresetsCommand implements CommandExecutor, Prefixable, TabCompleter {

    private final BTEConoSur plugin;

    public PresetsCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());
        WorldEditManager manager = s.getWorldEditManager();

        if (args.length < 1) {
            p.sendMessage(this.getPrefix() + "Introduce un subcomando.");
            return true;
        }

        switch (args[0]) {
            case "set": {
                if (args.length < 2) {
                    p.sendMessage(getPrefix() + "Introduce un nombre para el preset.");
                    return true;
                }

                String name = args[1];

                if (!name.matches("[a-zA-Z_]{1,32}")) {
                    p.sendMessage(getPrefix() + "Introduce un nombre válido para el preset.");
                    return true;
                }

                if (args.length < 3) {
                    p.sendMessage(getPrefix() + "Introduce el texto del preset.");
                    return true;
                }

                String value = args[2];

                try {
                    manager.setPreset(name, value);
                    p.sendMessage(getPrefix() + "Preset §a" + name + "§f guardado.");
                } catch (SQLException e) {
                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                    return true;
                }
                break;
            }
            case "delete": {
                if (args.length < 2) {
                    p.sendMessage(getPrefix() + "Introduce un nombre para el preset.");
                    return true;
                }

                String name = args[1];

                if (!manager.existsPreset(name)) {
                    p.sendMessage(getPrefix() + "El preset introducido no existe.");
                    return true;
                }

                try {
                    manager.deletePreset(name);
                    p.sendMessage(getPrefix() + "Preset §a" + name + "§f eliminado.");
                } catch (SQLException e) {
                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                    return true;
                }
                break;
            }
            case "list": {

                Map<String, String> presets = manager.getPresets();

                if (presets.isEmpty()) {
                    p.sendMessage(getPrefix() + "No tienes ningún preset guardado.");
                    return true;
                }

                p.sendMessage(" ");
                p.sendMessage("§8>-------[ §7PRESETS §8]-------<");
                for (String name : presets.keySet()) {
                    p.sendMessage("§7• §a" + name + "§7 - §e" + presets.get(name));
                }
                p.sendMessage("§8>------------------------<");
                p.sendMessage(" ");

                break;
            }
        }

        return true;
    }

    @Override
    public String getPrefix() {
        return "§f[§7PRESETS§f] §7>> §f";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(
                    Arrays.asList(
                            "set", "delete", "list"
                    )
            );
        } else if (args.length == 2 && args[0].equals("delete")) {
            Player player = (Player) sender;
            ServerPlayer s = plugin.getPlayerRegistry().get(player.getUniqueId());
            WorldEditManager manager = s.getWorldEditManager();
            completions.addAll(manager.getPresets().keySet());
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

package pizzaaxx.bteconosur.Scoreboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.ScoreboardManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScoreboardCommand implements CommandExecutor, TabCompleter {

    private final BTEConoSur plugin;

    public ScoreboardCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());
        ScoreboardManager manager = s.getScoreboardManager();

        if (args.length == 0) {
            try {
                if (manager.setHidden(!manager.isHidden())) {
                    p.sendActionBar("§7Scoreboard oculto");
                } else {
                    p.sendActionBar("§7Scoreboard visible");
                }
            } catch (SQLException e) {
                p.sendActionBar(plugin.getPrefix() + "Ha ocurrido un error en la base de datos.");
            }
            return true;
        }

        switch (args[0]) {
            case "me":
            case "server":
            case "project":
            case "top":
            case "city": {
                Class<? extends ScoreboardDisplay> displayClass = plugin.getScoreboardHandler().getClass(args[0]);
                ScoreboardDisplay display = plugin.getScoreboardHandler().getDisplay(displayClass, s, p.getLocation());
                try {
                    if (manager.isAuto()) {
                        manager.setAuto(false);
                        p.sendActionBar("§7Scoreboard automático desactivado");
                    }
                    manager.setDisplay(display);
                } catch (SQLException e) {
                    p.sendActionBar(plugin.getPrefix() + "Ha ocurrido un error en la base de datos.");
                }
                break;
            }
            case "auto": {
                try {
                    if (manager.setAuto(!manager.isAuto())) {
                        p.sendActionBar("§7Scoreboard automático activado");
                    } else {
                        p.sendActionBar("§7Scoreboard automático desactivado");
                    }
                } catch (SQLException e) {
                    p.sendActionBar(plugin.getPrefix() + "Ha ocurrido un error en la base de datos.");
                }
                break;
            }
            default: {
                p.sendMessage(plugin.getPrefix() + "Introduce un subcomando válido.");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(
                    Arrays.asList(
                            "me", "server", "project", "top", "city", "auto"
                    )
            );
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

package pizzaaxx.bteconosur.Commands.Custom;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;
import static pizzaaxx.bteconosur.Utils.StringUtils.UPPER_CASE;

public class CustomCommandsManager implements CommandExecutor {

    private final BTEConoSur plugin;
    private final Map<String, Action> actions = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();

    public CustomCommandsManager(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public String get(Action action) {

        String id = StringUtils.generateCode(12, actions.keySet(), LOWER_CASE, UPPER_CASE);

        actions.put(id, action);
        this.scheduleDeletion(id);
        return id;
    }

    private void scheduleDeletion(String id) {
        deletionCache.put(id, System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (deletionCache.containsKey(id) && System.currentTimeMillis() - deletionCache.get(id) > 550000) {
                    deletionCache.remove(id);
                    actions.remove(id);
                }
            }
        }.runTaskLaterAsynchronously(plugin, 12000);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        if (args.length > 0) {
            Action action = actions.get(args[0]);

            if (action != null) {
                action.exec(
                        new CustomCommandEvent(
                                (Player) sender
                        )
                );
            }
        }

        return true;
    }
}

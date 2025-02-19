package pizzaaxx.bteconosur.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.ScoreboardManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.sql.SQLException;

public class EntityClickEvent implements Listener {

    private final BTEConoSur plugin;

    public EntityClickEvent(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEntityEvent event) {

        if (event.getRightClicked() instanceof Player) {

            Player target = (Player) event.getRightClicked();

            ServerPlayer s = plugin.getPlayerRegistry().get(target.getUniqueId());

            ScoreboardManager manager = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId()).getScoreboardManager();

            try {
                manager.setAuto(false);
                event.getPlayer().sendActionBar("§7Scoreboard automático desactivado");
                manager.setDisplay(s);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }
}

package pizzaaxx.bteconosur.Events;

import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public class TeleportEvent implements Listener {

    @EventHandler
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        event.getPlayer().playSound(event.getTo(), Sound.ENTITY_ENDERMEN_TELEPORT, 1, 1);
    }

}

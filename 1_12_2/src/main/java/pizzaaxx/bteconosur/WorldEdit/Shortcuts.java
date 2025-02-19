package pizzaaxx.bteconosur.WorldEdit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

public class Shortcuts implements Listener {

    private final BTEConoSur plugin;

    public Shortcuts(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent event) {
        Player p = event.getPlayer();
        int increment = plugin.getPlayerRegistry().get(p.getUniqueId()).getWorldEditManager().getIncrement();
        if (event.getItem() != null && event.getItem().getType() == Material.WOOD_AXE) {
            switch (event.getAction()) {
                case LEFT_CLICK_AIR:
                    if (p.isSneaking()) {
                        plugin.getSelUndoRedoCommand().onShortcutBefore(p);
                        p.performCommand("/shift " + increment);
                        plugin.getSelUndoRedoCommand().onShortcutAfter(p);
                    } else {
                        plugin.getSelUndoRedoCommand().onShortcutBefore(p);
                        p.performCommand("/expand " + increment);
                        plugin.getSelUndoRedoCommand().onShortcutAfter(p);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    plugin.getSelUndoRedoCommand().onShortcutBefore(p);
                    p.performCommand("/contract " + increment);
                    plugin.getSelUndoRedoCommand().onShortcutAfter(p);
            }
        }
    }
}

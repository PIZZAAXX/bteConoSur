package pizzaaxx.bteconosur.Commands.Custom;

import org.bukkit.entity.Player;

public class CustomCommandEvent {

    private final Player player;

    public CustomCommandEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}

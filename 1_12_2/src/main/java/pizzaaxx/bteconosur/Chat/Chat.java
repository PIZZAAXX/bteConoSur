package pizzaaxx.bteconosur.Chat;

import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public interface Chat {

    boolean isUnloadable();
    String getID();

    String getDisplayName();

    String getEmoji();

    boolean acceptsPlayer(UUID uuid);

    Set<UUID> getPlayers();

    void addPlayer(UUID uuid);

    void removePlayer(UUID uuid);

    void sendMessageFromOther(Chat originChat, UUID uuid, String message);

    void sendMessage(UUID uuid, String message);

    void broadcast(String message, boolean ignoreHidden);

    ItemStack getHead();

}

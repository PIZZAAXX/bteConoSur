package pizzaaxx.bteconosur.Tablist;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.io.IOException;
import java.sql.SQLException;

public class TablistExpansion extends PlaceholderExpansion {

    private final BTEConoSur plugin;

    public TablistExpansion(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bteconosur";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PIZZAAXX";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        if (params.equalsIgnoreCase("tabPrefix")) {

            ServerPlayer s = plugin.getPlayerRegistry().get(player.getUniqueId());
            return "§" + s.getPriority() + "" + s.getChatManager().getCountryTabPrefix() + " " + s.getTablistPrefixHolder().getTablistPrefix() + " " + s.getChatManager().getTabColor();

        }

        if (params.startsWith("top")) {
            Country country = plugin.getCountryManager().get(params.replace("top", "").toLowerCase());
            try {
                return plugin.getPlayerRegistry().get(country.getTopPlayers().get(0)).getName();
            } catch (SQLException | IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean persist() {
        return true;
    }
}

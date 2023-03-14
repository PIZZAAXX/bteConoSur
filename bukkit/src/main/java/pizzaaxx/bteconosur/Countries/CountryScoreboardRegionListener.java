package pizzaaxx.bteconosur.Countries;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Player.Managers.ScoreboardManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Regions.RegionEnterEvent;
import pizzaaxx.bteconosur.Regions.RegionLeaveEvent;
import pizzaaxx.bteconosur.Regions.RegionListener;

import java.sql.SQLException;

public class CountryScoreboardRegionListener extends RegionListener {

    private final BTEConoSur plugin;

    public CountryScoreboardRegionListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onRegionEnter(@NotNull RegionEnterEvent event) {
        ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
        ScoreboardManager manager = s.getScoreboardManager();
        if (manager.getDisplayClass() == Country.class) {
            try {
                manager.setDisplay(plugin.getScoreboardHandler().getDisplay(Country.class, s, event.getTo()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRegionLeave(@NotNull RegionLeaveEvent event) {
        ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
        ScoreboardManager manager = s.getScoreboardManager();
        if (manager.getDisplayClass() == Country.class) {
            try {
                manager.setDisplay(plugin.getScoreboardHandler().getDisplay(Country.class, s, event.getTo()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}

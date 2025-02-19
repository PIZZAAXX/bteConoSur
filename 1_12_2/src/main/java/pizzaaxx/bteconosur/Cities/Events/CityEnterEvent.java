package pizzaaxx.bteconosur.Cities.Events;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Regions.RegionEnterEvent;
import pizzaaxx.bteconosur.Regions.RegionListener;

public class CityEnterEvent extends RegionListener {

    private final BTEConoSur plugin;

    public CityEnterEvent(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onRegionEnter(@NotNull RegionEnterEvent event) {
        City city = plugin.getCityManager().get(
                event.getRegionID().split("_")[1]
        );
        event.getPlayer().sendActionBar("§7¡Has entrado a " + city.getDisplayName() + "!");
    }

}

package pizzaaxx.bteconosur.Cities.Actions;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.SQLException;
import java.util.List;

public class SetUrbanAreaCityAction {

    private final BTEConoSur plugin;
    private final String name;
    private final List<BlockVector2D> points;


    public SetUrbanAreaCityAction(BTEConoSur plugin, String name, List<BlockVector2D> points) {
        this.plugin = plugin;
        this.name = name;
        this.points = points;
    }

    public void execute() throws CityActionException, SQLException {
        if (!plugin.getCityManager().exists(name)) {
            throw new CityActionException();
        }

        ProtectedRegion urbanRegion = new ProtectedPolygonalRegion(
                "city_" + name + "_urban",
                points,
                -100,
                8000
        );
        plugin.getRegionManager().addRegion(urbanRegion);

        plugin.getSqlManager().update(
                "cities",
                new SQLValuesSet(
                        new SQLValue(
                                "urban_area",
                                true
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", name
                        )
                )
        ).execute();

        plugin.getCityManager().reloadCity(name);
    }
}
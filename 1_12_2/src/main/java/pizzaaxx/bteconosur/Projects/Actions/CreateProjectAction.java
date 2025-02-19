package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Cities.CityActionException;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.SatMapHandler;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class CreateProjectAction {

    private final BTEConoSur plugin;

    private final Country country;
    private final ProjectType type;
    private final int points;

    private final List<BlockVector2D> region;


    public CreateProjectAction(BTEConoSur plugin, Country country, ProjectType type, int points, List<BlockVector2D> region) {
        this.plugin = plugin;
        this.country = country;
        this.type = type;
        this.points = points;
        this.region = region;
    }

    public Project exec() throws SQLException, CityActionException, IOException {
        String id = StringUtils.generateCode(6, plugin.getProjectRegistry().getIds(), LOWER_CASE);

        ProtectedPolygonalRegion protectedPolygonalRegion = new ProtectedPolygonalRegion("project_" + id, region, -100, 8000);
        protectedPolygonalRegion.setFlag(DefaultFlag.BUILD, StateFlag.State.ALLOW);
        protectedPolygonalRegion.setFlag(DefaultFlag.BUILD.getRegionGroupFlag(), RegionGroup.MEMBERS);
        protectedPolygonalRegion.setPriority(1);

        FlagRegistry registry = plugin.getWorldGuard().getFlagRegistry();

        protectedPolygonalRegion.setFlag((StateFlag) registry.get("worldedit"), StateFlag.State.ALLOW);
        protectedPolygonalRegion.setFlag(registry.get("worldedit").getRegionGroupFlag(), RegionGroup.MEMBERS);

        Set<City> cities = plugin.getCityManager().getCitiesAt(protectedPolygonalRegion, country);

        plugin.getSqlManager().insert(
                "projects",
                new SQLValuesSet(
                        new SQLValue(
                                "id", id
                        ),
                        new SQLValue(
                                "country", country.getName()
                        ),
                        new SQLValue(
                                "cities", cities
                        ),
                        new SQLValue(
                                "pending", null
                        ),
                        new SQLValue(
                                "type", type.getName()
                        ),
                        new SQLValue(
                                "points", points
                        )
                )
        ).execute();

        for (City city : cities) {
            plugin.getScoreboardHandler().update(city);
        }

        plugin.getRegionManager().addRegion(protectedPolygonalRegion);

        plugin.getProjectRegistry().registerID(id);

        List<Coords2D> coords = new ArrayList<>();
        for (BlockVector2D vector2D : region) {
            coords.add(new Coords2D(plugin, vector2D));
        }

        InputStream is = plugin.getSatMapHandler().getMapStream(
                new SatMapHandler.SatMapPolygon(
                        plugin,
                        region,
                        "3068ff"
                )
        );

        File file = new File(plugin.getDataFolder(), "projects/images/" + id + ".png");
        file.createNewFile();
        Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        plugin.getTerramapHandler().drawPolygon(coords, new Color(78, 255, 71), id);

        country.getLogsChannel().sendMessage(":clipboard: Proyecto de tipo `" + type.getDisplayName() + "` y puntaje `" + points + "` creado con la ID `" + id + "`.").queue();

        return plugin.getProjectRegistry().get(id);
    }
}

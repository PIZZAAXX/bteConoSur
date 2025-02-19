package pizzaaxx.bteconosur.Cities;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CityManager {

    private final Map<String, City> citiesCache = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();
    private final Set<String> names = new HashSet<>();
    public final Map<String, String> displayNames = new HashMap<>();

    public final Map<String, ProtectedRegion> regions = new HashMap<>();

    private final BTEConoSur plugin;

    public CityManager(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException, IOException {
        ResultSet set = plugin.getSqlManager().select(
                "cities",
                new SQLColumnSet(
                        "name",
                        "display_name"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            String name = set.getString("name");

            File coordsFile = new File(plugin.getDataFolder(), "cities/" + name + ".json");

            if (!coordsFile.exists()) {
                continue;
            }

            JsonNode node = plugin.getJSONMapper().readTree(coordsFile);

            String type = node.path("type").asText();

            List<BlockVector2D> regionPoints = new ArrayList<>();

            for (JsonNode coordsArray : node.path("coordinates")) {
                int n1, n2;
                if (type.equals("geographic")) {
                    Coords2D coord = new Coords2D(plugin, coordsArray.get(1).asDouble(), coordsArray.get(0).asDouble());
                    n1 = (int) Math.floor(coord.getX());
                    n2 = (int) Math.floor(coord.getZ());
                } else {
                    n1 = coordsArray.get(0).asInt();
                    n2 = coordsArray.get(1).asInt();
                }
                regionPoints.add(new BlockVector2D(n1, n2));
            }

            this.regions.put(
                    name,
                    new ProtectedPolygonalRegion(
                            "city_" + name,
                            regionPoints,
                            -100,
                            8000
                    )
            );

            names.add(name);
            displayNames.put(set.getString("name"), set.getString("display_name"));

        }
    }


    public boolean exists(String name) {
        return names.contains(name);
    }

    public boolean isLoaded(String name) {
        return citiesCache.containsKey(name);
    }

    public void load(String name) throws SQLException, IOException {
        citiesCache.put(name, new City(plugin, name));
        this.scheduleDeletion(name);
    }

    public void unload(String name) {
        citiesCache.remove(name);
        deletionCache.remove(name);
    }

    public City get(String name) {
        if (!this.isLoaded(name)) {
            try {
                this.load(name);
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.scheduleDeletion(name);
        return citiesCache.get(name);
    }

    public City getCityAt(Location loc) {
        for (ProtectedRegion region : plugin.getApplicableRegions(loc)) {
            if (region.getId().startsWith("city_")) {
                String name = region.getId().replace("city_", "").replace("_urban", "");
                return this.get(name);
            }
        }
        return null;
    }

    public Set<City> getCitiesAt(@NotNull ProtectedRegion region, Country country) {
        Set<City> cities = new HashSet<>();
        for (ProtectedRegion r : region.getIntersectingRegions(regions.values())) {
            String cityName = r.getId().replace("city_", "");
            City city = this.get(cityName);
            if (city.getCountry().equals(country)) {
                cities.add(city);
            }
        }
        return cities;
    }

    public Collection<String> getNames() {
        return names;
    }

    public String getDisplayName(String name) {
        return displayNames.get(name);
    }

    public void registerName(String name, String displayName, @NotNull JsonNode node) {
        names.add(name);
        displayNames.put(name, displayName);

        String type = node.path("type").asText();

        List<BlockVector2D> regionPoints = new ArrayList<>();

        for (JsonNode coordsArray : node.path("coordinates")) {
            int n1, n2;
            if (type.equals("geographic")) {
                Coords2D coord = new Coords2D(plugin, coordsArray.get(1).asDouble(), coordsArray.get(0).asDouble());
                n1 = (int) Math.floor(coord.getX());
                n2 = (int) Math.floor(coord.getZ());
            } else {
                n1 = coordsArray.get(0).asInt();
                n2 = coordsArray.get(1).asInt();
            }
            regionPoints.add(new BlockVector2D(n1, n2));
        }

        this.regions.put(
                name,
                new ProtectedPolygonalRegion(
                        "city_" + name,
                        regionPoints,
                        -100,
                        8000
                )
        );
    }

    public void unregisterName(String name) {
        names.remove(name);
        deletionCache.remove(name);
        citiesCache.remove(name);
    }

    public void reloadCity(String name) {
        if (this.isLoaded(name)) {
            unload(name);
            try {
                load(name);
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void scheduleDeletion(String name) {
        if (this.isLoaded(name)) {
            deletionCache.put(name, System.currentTimeMillis());
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (deletionCache.containsKey(name) && System.currentTimeMillis() - deletionCache.get(name) > 550000) {
                        unload(name);
                    }
                }
            };
            runnable.runTaskLaterAsynchronously(plugin, 12000);
        }
    }

    public List<String> getCloseMatches(@NotNull String input, int limit) {

        List<Map.Entry<String, Integer>> entries = new ArrayList<>();

        String i = input.toLowerCase();
        for (String name : displayNames.keySet()) {

            int distance = plugin.getFuzzyMatcher().getDistance(i, displayNames.get(name).toLowerCase());

            if (distance == 0) {
                return new ArrayList<>(Collections.singleton("name"));
            }

            if (distance <= limit) {
                entries.add(
                        new AbstractMap.SimpleEntry<>(
                                name,
                                distance
                        )
                );
            }
        }

        entries.sort(Map.Entry.comparingByValue());

        return entries.stream().map(Map.Entry::getKey).collect(Collectors.toList());

    }
}

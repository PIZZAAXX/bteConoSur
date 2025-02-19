package pizzaaxx.bteconosur.Cities;

import clipper2.Clipper;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLJSONArrayCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLNullCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.JSONParsable;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardDisplay;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class City implements JSONParsable, ScoreboardDisplay {

    private final BTEConoSur plugin;
    private final String name;
    private final String displayName;
    private final Country country;
    private final ProtectedRegion region;
    private ProtectedRegion urbanRegion;
    private int finishedArea;

    public City(@NotNull BTEConoSur plugin, String name) throws SQLException, IOException {
        this.plugin = plugin;
        ResultSet set = plugin.getSqlManager().select(
                "cities",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", name
                        )
                )
        ).retrieve();

        if (set.next()) {
            this.name = set.getString("name");
            this.displayName = set.getString("display_name");
            this.country = plugin.getCountryManager().get(set.getString("country"));
            this.finishedArea = set.getInt("finished_area");
        } else {
            throw new IllegalArgumentException();
        }

        File coordsFile = new File(plugin.getDataFolder(), "cities/" + name + ".json");

        if (!coordsFile.exists()) {
            throw new IllegalArgumentException();
        }

        JsonNode node = plugin.getJSONMapper().readTree(coordsFile);

        String type = node.path("type").asText();

        {

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

            this.region = new ProtectedPolygonalRegion(
                    "city_" + name,
                    regionPoints,
                    -100,
                    8000
            );
        }

        if (node.has("urban")) {

            List<BlockVector2D> urbanPoints = new ArrayList<>();

            for (JsonNode coordsArray : node.path("urban")) {
                int n1, n2;
                if (type.equals("geographic")) {
                    Coords2D coord = new Coords2D(plugin, coordsArray.get(1).asDouble(), coordsArray.get(0).asDouble());
                    n1 = (int) Math.floor(coord.getX());
                    n2 = (int) Math.floor(coord.getZ());
                } else {
                    n1 = coordsArray.get(0).asInt();
                    n2 = coordsArray.get(1).asInt();
                }
                urbanPoints.add(new BlockVector2D(n1, n2));
            }

            this.urbanRegion = new ProtectedPolygonalRegion(
                    "city_" + name + "_urban",
                    urbanPoints,
                    -100,
                    8000
            );
        }
    }

    public BTEConoSur getPlugin() {
        return plugin;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Country getCountry() {
        return country;
    }

    public int getAvailableProjectsAmount() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "COUNT(id) as count"
                ),
                new SQLANDConditionSet(
                        new SQLJSONArrayCondition(
                                "cities", this.name
                        ),
                        new SQLNullCondition(
                                "owner", true
                        )
                )
        ).retrieve();

        set.next();
        return set.getInt("count");
    }

    public int getClaimedProjectsAmount() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "COUNT(id) as count"
                ),
                new SQLANDConditionSet(
                        new SQLJSONArrayCondition(
                                "cities", this.name
                        ),
                        new SQLNullCondition(
                                "owner", false
                        )
                )
        ).retrieve();

        set.next();
        return set.getInt("count");
    }

    public int getFinishedProjectsAmount() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "finished_projects",
                new SQLColumnSet(
                        "COUNT(id) as count"
                ),
                new SQLANDConditionSet(
                        new SQLJSONArrayCondition(
                                "cities", this.name
                        )
                )
        ).retrieve();

        set.next();
        return set.getInt("count");
    }

    public List<String> getPosts() throws SQLException {
        List<String> ids = new ArrayList<>();
        ResultSet set = plugin.getSqlManager().select(
                "posts",
                new SQLColumnSet(
                        "id"
                ),
                new SQLANDConditionSet(
                        new SQLJSONArrayCondition(
                                "cities", this.name
                        )
                )
        ).retrieve();

        while (set.next()) {
            ids.add(set.getString("id"));
        }

        return ids;
    }

    public boolean hasUrbanArea() {
        return urbanRegion != null;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public ProtectedRegion getUrbanRegion() {
        return urbanRegion;
    }

    public int getFinishedArea() {
        return finishedArea;
    }

    public double getTotalArea() {
        Path64 polygon = new Path64();

        List<BlockVector2D> vectors = region.getPoints();

        vectors.forEach(vector -> {
            Point64 point = new Point64();
            point.x = (long) vector.getX();
            point.y = (long) vector.getZ();
            polygon.add(point);
        });

        BlockVector2D vector = vectors.get(0);
        Point64 point = new Point64();
        point.x = (long) vector.getX();
        point.y = (long) vector.getZ();
        polygon.add(point);

        return Clipper.Area(polygon);
    }

    public void updateFinishedArea() throws SQLException, JsonProcessingException {

        ResultSet set = plugin.getSqlManager().select(
                "finished_projects",
                new SQLColumnSet(
                        "region_points"
                ),
                new SQLANDConditionSet(
                        new SQLJSONArrayCondition(
                                "cities", this.name
                        )
                )
        ).retrieve();

        Paths64 polygons = new Paths64();
        while (set.next()) {

            Path64 polygon = new Path64();

            List<BlockVector2D> vectors = new ArrayList<>();
            List<Object> coordsRaw = plugin.getJSONMapper().readValue(set.getString("region_points"), ArrayList.class);
            for (Object obj : coordsRaw) {
                Map<String, Double> coords = (Map<String, Double>) obj;
                vectors.add(new BlockVector2D(coords.get("x"), coords.get("z")));
            }

            vectors.forEach(vector -> {
                Point64 point = new Point64();
                point.x = (long) vector.getX();
                point.y = (long) vector.getZ();
                polygon.add(point);
            });

            BlockVector2D vector = vectors.get(0);
            Point64 point = new Point64();
            point.x = (long) vector.getX();
            point.y = (long) vector.getZ();
            polygon.add(point);

            polygons.add(polygon);
        }
        Paths64 result = Clipper.Union(polygons, FillRule.EvenOdd);

        Paths64 cityPoly = new Paths64();
        Path64 poly = new Path64();
        for (BlockVector2D vector : this.region.getPoints()) {
            Point64 point = new Point64();
            point.x = (long) vector.getX();
            point.y = (long) vector.getZ();
            poly.add(point);
        }

        BlockVector2D vector = this.getRegion().getPoints().get(0);
        Point64 point = new Point64();
        point.x = (long) vector.getX();
        point.y = (long) vector.getZ();
        poly.add(point);
        cityPoly.add(poly);

        Paths64 finalResult = Clipper.Intersect(result, cityPoly, FillRule.NonZero);

        int area = (int) Clipper.Area(finalResult);

        plugin.getSqlManager().update(
                "cities",
                new SQLValuesSet(
                        new SQLValue(
                                "finished_area", area
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", this.name
                        )
                )
        ).execute();

        plugin.getScoreboardHandler().update(this);

        this.finishedArea = area;
    }


    @Override
    public String getJSON(boolean insideJSON) {
        return (insideJSON?"\"":"'") + this.name + (insideJSON?"\"":"'");
    }

    @Override
    public boolean equals(Object obj) {

        if (getClass() != obj.getClass()) {
            return false;
        }

        City city = (City) obj;
        return this.name.equals(city.name);
    }

    @Override
    public String getScoreboardTitle() {
        return "§a§l" + this.displayName;
    }

    @Override
    public List<String> getScoreboardLines() {

        List<String> lines = new ArrayList<>();

        double finishedArea = this.finishedArea / 1000000.0;
        double totalArea = this.getTotalArea() / 1000000.0;
        double percentage = (finishedArea / totalArea) * 100;

        DecimalFormat format = new DecimalFormat("#.##");

        lines.add("§fÁrea terminada:§7 " + format.format(finishedArea) + "km² (" + format.format(Math.min(percentage, 100)) + "%)");

        try {
            lines.add("§fProy. terminados: §7" + this.getFinishedProjectsAmount());

            lines.add("§fProy. activos: §7" + this.getClaimedProjectsAmount());

            lines.add("§fProy. disponibles: §7" + this.getAvailableProjectsAmount());
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        return lines;
    }

    @Override
    public String getScoreboardType() {
        return "city";
    }

    @Override
    public String getScoreboardID() {
        return "city_" + name;
    }
}

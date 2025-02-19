package pizzaaxx.bteconosur.Utils;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.util.net.HttpRequest;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Configuration.Configuration;
import pizzaaxx.bteconosur.Geo.Coords2D;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SatMapHandler {

    public static class SatMapPolygon {
        private final List<Coords2D> points;
        private final String color;

        public SatMapPolygon(BTEConoSur plugin, @NotNull List<BlockVector2D> points, String color) {
            this.points = new ArrayList<>();
            for (BlockVector2D vector : points) {
                this.points.add(new Coords2D(plugin, vector));
            }
            this.color = color;
        }

        public SatMapPolygon(BTEConoSur plugin, @NotNull List<BlockVector2D> points) {
            this.points = new ArrayList<>();
            for (BlockVector2D vector : points) {
                this.points.add(new Coords2D(plugin, vector));
            }
            this.color = "5882fa";
        }

        public List<Coords2D> getPoints() {
            return points;
        }

        public String getColor() {
            return color;
        }
    }

    private final BTEConoSur plugin;
    private final String key;

    public String getKey() {
        return key;
    }

    public SatMapHandler(BTEConoSur plugin) {
        this.plugin = plugin;
        this.key = new Configuration(plugin, "config").getString("mapquestKey");
    }

    public String getMap(SatMapPolygon... polygons) {
        return this.getMap(Arrays.asList(polygons));
    }

    public String getMap(@NotNull Iterable<SatMapPolygon> polygons) {
        StringBuilder builder = new StringBuilder();
        builder.append("https://www.mapquestapi.com/staticmap/v5/map?type=sat&size=1920,1080&imagetype=png");
        builder.append("&key=").append(key);
        for (SatMapPolygon polygon : polygons) {
            StringBuilder shapeBuilder = new StringBuilder("&shape=");
            for (Coords2D coord : polygon.points) {
                shapeBuilder.append(coord.getLat()).append(",").append(coord.getLon()).append("|");
            }
            shapeBuilder.append("|").append(polygon.points.get(0).getLat()).append(",").append(polygon.points.get(0).getLon());
            shapeBuilder.append("|fill:").append(polygon.getColor()).append("50");
            shapeBuilder.append("|border:").append(polygon.getColor());
            builder.append(shapeBuilder);
        }
        return builder.toString();
    }

    public InputStream getMapStream(SatMapPolygon... polygons) throws IOException {
        return this.getMapStream(Arrays.asList(polygons));
    }

    public InputStream getMapStream(@NotNull Iterable<SatMapPolygon> polygons) throws IOException {
        URL url = new URL(this.getMap(polygons));
        return HttpRequest.get(url).execute().getInputStream();
    }
}

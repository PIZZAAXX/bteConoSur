package pizzaaxx.bteconosur.SQL.Entities;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.SQL.JSONParsable;
import pizzaaxx.bteconosur.SQL.SQLParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SQLPolygon implements JSONParsable {

    private final List<Double[]> coordinates;

    public SQLPolygon(List<Double[]> coordinates) {
        this.coordinates = coordinates;
    }

    @NotNull
    @Contract("_ -> new")
    public static SQLPolygon getFromPolygonRegion(@NotNull ProtectedPolygonalRegion region) {
        List<Double[]> coordinates = new ArrayList<>();
        for (BlockVector2D vector : region.getPoints()) {
            coordinates.add(new Double[] {
                    vector.getX(),
                    vector.getZ()
            });
        }
        return new SQLPolygon(coordinates);
    }

    @NotNull
    @Contract("_ -> new")
    public static SQLPolygon getFromVectors(@NotNull List<BlockVector2D> vectors) {
        List<Double[]> coordinates = new ArrayList<>();
        for (BlockVector2D vector : vectors) {
            coordinates.add(new Double[] {
                    vector.getX(),
                    vector.getZ()
            });
        }
        return new SQLPolygon(coordinates);
    }


    @Override
    public String getJSON(boolean insideJSON) {

        if (insideJSON) {
            return SQLParser.getString(coordinates);
        } else {
            return "PolygonFromText('POLYGON((" + coordinates.stream().map(coord -> coord[0] + " " + coord[1]).collect(Collectors.joining(",")) + "," + coordinates.get(0)[0] + " " + coordinates.get(0)[1] + "))')";
        }

    }
}

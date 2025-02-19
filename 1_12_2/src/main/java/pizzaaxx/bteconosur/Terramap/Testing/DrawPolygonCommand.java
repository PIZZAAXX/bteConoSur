package pizzaaxx.bteconosur.Terramap.Testing;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Terramap.TerramapHandler;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class DrawPolygonCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public DrawPolygonCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (args.length > 0) {
            if (args[0].equals("delete")) {
                if (args.length > 1) {
                    try {
                        plugin.getTerramapHandler().deletePolygon(args[1]);
                    } catch (IOException e) {
                        sender.sendMessage("Error deleting polygon.");
                    }
                }
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;

        Region region;
        try {
            region = plugin.getWorldEdit().getSelection(p);
        } catch (IncompleteRegionException e) {
            return true;
        }

        if (!(region instanceof Polygonal2DRegion)) {
            return true;
        }

        Polygonal2DRegion polyRegion = (Polygonal2DRegion) region;

        TerramapHandler handler = plugin.getTerramapHandler();

        List<Coords2D> coordinates = new ArrayList<>();
        for (BlockVector2D v : polyRegion.getPoints()) {
            coordinates.add(new Coords2D(plugin, v));
        }

        try {
            handler.drawPolygon(
                    coordinates,
                    new Color(51, 60, 232),
                    "zdwpelkb"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}

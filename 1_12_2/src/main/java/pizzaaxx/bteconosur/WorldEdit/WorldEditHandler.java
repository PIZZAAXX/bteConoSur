package pizzaaxx.bteconosur.WorldEdit;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.RegionSelection;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorldEditHandler implements Prefixable {

    private final BTEConoSur plugin;
    private final WorldEdit worldEdit = WorldEdit.getInstance();

    public WorldEdit getWorldEdit() {
        return worldEdit;
    }

    private final WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

    public WorldEditHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPrefix() {
        return "§f[§bWorldEdit§f] §7>> §f";
    }

    public LocalSession getLocalSession(Player player) {
        com.sk89q.worldedit.entity.Player actor = this.getWEPlayer(player);

        SessionManager manager = worldEdit.getSessionManager();
        return manager.get(actor);
    }

    public com.sk89q.worldedit.entity.Player getWEPlayer(Player player) {
        return new BukkitPlayer(worldEditPlugin, worldEditPlugin.getServerInterface(), player);
    }

    public Region getSelection(Player player) throws IncompleteRegionException {
        com.sk89q.worldedit.entity.Player actor = this.getWEPlayer(player);

        SessionManager manager = worldEdit.getSessionManager();
        LocalSession localSession = manager.get(actor);
        World selectionWorld = localSession.getSelectionWorld();

        return localSession.getSelection(selectionWorld);
    }

    public Region getIncompleteSelection(Player player){
        com.sk89q.worldedit.entity.Player actor = this.getWEPlayer(player);

        SessionManager manager = worldEdit.getSessionManager();
        LocalSession localSession = manager.get(actor);
        World selectionWorld = localSession.getSelectionWorld();

        return localSession.getRegionSelector(selectionWorld).getIncompleteRegion();
    }

    public List<BlockVector2D> getSelectionPoints(Player player) throws IncompleteRegionException {
        Region selection = this.getSelection(player);

        if (selection instanceof Polygonal2DRegion) {
            Polygonal2DRegion polyRegion = (Polygonal2DRegion) selection;
            return polyRegion.getPoints();
        } else if (selection instanceof CuboidRegion) {
            CuboidRegion cuboidRegion = (CuboidRegion) selection;
            double maxX = cuboidRegion.getMaximumPoint().getX();
            double maxZ = cuboidRegion.getMaximumPoint().getZ();
            double minX = cuboidRegion.getMinimumPoint().getX();
            double minZ = cuboidRegion.getMinimumPoint().getZ();

            List<BlockVector2D> result = new ArrayList<>();

            result.add(new BlockVector2D(maxX, maxZ));
            result.add(new BlockVector2D(maxX, minZ));
            result.add(new BlockVector2D(minX, minZ));
            result.add(new BlockVector2D(minX, maxZ));

            return result;
        }
        throw new IncompleteRegionException();
    }

    public void setSelection(Player player, RegionSelection selection) {
        worldEditPlugin.setSelection(player, selection);
    }

    public Pattern getPattern(Player player, String input) throws InputParseException {
        ParserContext context = new ParserContext();
        com.sk89q.worldedit.entity.Player actor = this.getWEPlayer(player);
        context.setActor(actor);
        Extent extent = actor.getExtent();
        if (extent instanceof World) {
            context.setWorld((World) extent);
        }
        context.setSession(this.getLocalSession(player));

        return worldEdit.getPatternFactory().parseFromInput(input, context);
    }

    public Set<Vector> getBlocksInLine(@NotNull Vector pos1, @NotNull Vector pos2) {
        Set<Vector> vset = new HashSet<>();
        boolean notdrawn = true;

        int x1 = pos1.getBlockX(), y1 = pos1.getBlockY(), z1 = pos1.getBlockZ();
        int x2 = pos2.getBlockX(), y2 = pos2.getBlockY(), z2 = pos2.getBlockZ();
        int tipx = x1, tipy = y1, tipz = z1;
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);

        if (dx + dy + dz == 0) {
            vset.add(new Vector(tipx, tipy, tipz));
            notdrawn = false;
        }

        int max = Math.max(Math.max(dx, dy), dz);
        if (max == dx && notdrawn) {
            for (int domstep = 0; domstep <= dx; domstep++) {
                tipx = x1 + domstep * (x2 - x1 > 0 ? 1 : -1);
                tipy = (int) Math.round(y1 + domstep * ((double) dy) / ((double) dx) * (y2 - y1 > 0 ? 1 : -1));
                tipz = (int) Math.round(z1 + domstep * ((double) dz) / ((double) dx) * (z2 - z1 > 0 ? 1 : -1));

                vset.add(new Vector(tipx, tipy, tipz));
            }
            notdrawn = false;
        }

        if (max == dy && notdrawn) {
            for (int domstep = 0; domstep <= dy; domstep++) {
                tipy = y1 + domstep * (y2 - y1 > 0 ? 1 : -1);
                tipx = (int) Math.round(x1 + domstep * ((double) dx) / ((double) dy) * (x2 - x1 > 0 ? 1 : -1));
                tipz = (int) Math.round(z1 + domstep * ((double) dz) / ((double) dy) * (z2 - z1 > 0 ? 1 : -1));

                vset.add(new Vector(tipx, tipy, tipz));
            }
            notdrawn = false;
        }

        if (max == dz && notdrawn) {
            for (int domstep = 0; domstep <= dz; domstep++) {
                tipz = z1 + domstep * (z2 - z1 > 0 ? 1 : -1);
                tipy = (int) Math.round(y1 + domstep * ((double) dy) / ((double) dz) * (y2 - y1 > 0 ? 1 : -1));
                tipx = (int) Math.round(x1 + domstep * ((double) dx) / ((double) dz) * (x2 - x1 > 0 ? 1 : -1));

                vset.add(new Vector(tipx, tipy, tipz));
            }
        }
        return vset;
    }

}

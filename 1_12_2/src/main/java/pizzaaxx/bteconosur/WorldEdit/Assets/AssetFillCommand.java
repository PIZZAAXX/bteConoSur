package pizzaaxx.bteconosur.WorldEdit.Assets;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.WorldEditManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AssetFillCommand implements CommandExecutor {

    private final BTEConoSur plugin;
    private final String prefix;

    public AssetFillCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getWorldEdit().getPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        if (args.length < 1) {
            p.sendMessage(prefix + "Introduce un grupo de §oassets§r.");
            return true;
        }

        WorldEditManager worldEditManager = s.getWorldEditManager();

        AssetHolder holder;
        String name = args[0];

        if (worldEditManager.existsAssetGroup(name)) {
            holder = worldEditManager.getAssetGroup(name);
        } else if (plugin.getAssetsRegistry().exists(name)) {
            Asset asset = plugin.getAssetsRegistry().get(name);

            if (!asset.isAutoRotate()) {
                p.sendMessage(prefix + "El asset introducido no tiene rotación automática.");
                return true;
            }

            holder = asset;
        } else {
            p.sendMessage(prefix + "El nombre introducido no corresponde a ningún grupo ni ID de §oasset§r.");
            return true;
        }

        if (args.length < 2) {
            p.sendMessage(prefix + "Introduce una distancia entre los §oassets§r.");
            return true;
        }

        int distance;
        try {
            distance = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            p.sendMessage(prefix + "Introduce una distancia válida.");
            return true;
        }

        if (distance <= 0) {
            p.sendMessage(prefix + "Introduce una distancia mayor o igual a 1.");
            return true;
        }

        Region region;
        try {
            region = plugin.getWorldEdit().getSelection(p);
        } catch (IncompleteRegionException e) {
            p.sendMessage(prefix + "Selecciona un area poligonal.");
            return true;
        }

        if (!(region instanceof Polygonal2DRegion)) {
            p.sendMessage(prefix + "Selecciona un area poligonal.");
            return true;
        }

        Polygonal2DRegion polyRegion = (Polygonal2DRegion) region;
        Polygonal2DRegion finalRegion = new Polygonal2DRegion(plugin.getWorldEditWorld(), polyRegion.getPoints(), 100, 100);

        int k = 30;

        List<BlockVector2D> points = finalRegion.getPoints();

        // FINDS MAX AND MIN POINTS OF SELECTION
        int maxX = points.get(0).getBlockX();
        int minX = points.get(0).getBlockX();
        int maxZ = points.get(0).getBlockZ();
        int minZ = points.get(0).getBlockZ();

        for (BlockVector2D vector2D : points) {
            if (vector2D.getBlockX() > maxX) {
                maxX = vector2D.getBlockX();
            }
            if (vector2D.getBlockX() < minX) {
                minX = vector2D.getBlockX();
            }
            if (vector2D.getBlockZ() > maxZ) {
                maxZ = vector2D.getBlockZ();
            }
            if (vector2D.getBlockZ() < minZ) {
                minZ = vector2D.getBlockZ();
            }
        }

        double cellSize = distance / Math.sqrt(2);

        int gridWidth = (int) Math.ceil((maxX - minX) / cellSize);
        int gridHeight = (int) Math.ceil((maxZ - minZ) / cellSize);

        List<Vector2D> activeSamples = new ArrayList<>();
        Vector2D[][] grid = new Vector2D[gridWidth][gridHeight];

        Vector2D x0 = finalRegion.iterator().next().toVector2D().toBlockVector2D();
        activeSamples.add(x0);

        try {
            int gridX = this.getGridX(x0, minX, maxX, gridWidth);
            int gridZ = this.getGridZ(x0, minZ, maxZ, gridHeight);

            grid[gridX][gridZ] = x0;

            Random random = new Random();
            while (!activeSamples.isEmpty()) {
                Vector2D x1 = activeSamples.get(random.nextInt(activeSamples.size()));

                boolean found = false;
                for (int tries = 0; tries < k; tries++) {
                    double theta = random.nextInt(360);
                    double radius = random.nextInt(distance) + distance;

                    double newX = x1.getX() + (radius * Math.cos(Math.toRadians(theta)));
                    double newZ = x1.getZ() + (radius * Math.sin(Math.toRadians(theta)));
                    Vector2D x2 = new Vector2D(newX, newZ);

                    int gridX2 = this.getGridX(x2, minX, maxX, gridWidth);
                    int gridZ2 = this.getGridZ(x2, minZ, maxZ, gridHeight);

                    if (gridX2 < 0 || gridX2 >= gridWidth || gridZ2 < 0 || gridZ2 >= gridHeight) {
                        continue;
                    }

                    int xMin = Math.max(gridX2 - 1, 0);
                    int xMax = Math.min(gridX2 + 1, gridWidth - 1);
                    int zMin = Math.max(gridZ2 - 1, 0);
                    int zMax = Math.min(gridZ2 + 1, gridHeight - 1);

                    boolean valid = true;
                    mainLoop:
                    for (int x = xMin; x <=xMax; x++) {
                        for (int z = zMin; z <= zMax; z++) {
                            if (grid[x][z] != null) {
                                if (grid[x][z].distance(x2) < radius) {
                                    valid = false;
                                    break mainLoop;
                                }
                            }
                        }
                    }

                    if (valid) {
                        activeSamples.add(x2);
                        grid[gridX2][gridZ2] = x2;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    activeSamples.remove(x1);
                }
            }

            LocalSession localSession = plugin.getWorldEdit().getLocalSession(p);
            EditSession editSession = plugin.getWorldEdit().getWorldEdit().getEditSessionFactory().getEditSession(plugin.getWorldEditWorld(), localSession.getBlockChangeLimit());
            Mask mask = localSession.getMask();

            for (Vector2D[] vectors : grid) {
                if (vectors == null) {
                    continue;
                }
                for (Vector2D vector : vectors) {
                    if (vector == null) {
                        continue;
                    }
                    if (s.canBuild(new Location(plugin.getWorld(), vector.getBlockX(), 100, vector.getBlockZ()))) {
                        if (finalRegion.contains(vector.toVector(100))) {
                            int y = plugin.getWorld().getHighestBlockYAt(vector.getBlockX(), vector.getBlockZ());

                            if (mask != null && !mask.test(vector.toVector(y - 1))) {
                                continue;
                            }

                            if (y >= polyRegion.getMinimumY() && y <= polyRegion.getMaximumY() + 1) {
                                Asset asset = holder.select();

                                try {
                                    asset.paste(
                                            p,
                                            vector.toVector(y),
                                            random.nextInt(4) * 90,
                                            editSession
                                    );
                                } catch (MaxChangedBlocksException e) {
                                    localSession.remember(editSession);
                                }
                            }
                        }
                    }
                }
            }

            if (editSession.getBlockChangeCount() > 0) {
                localSession.remember(editSession);
            }
            p.sendMessage(prefix + "Operación completada. §7Bloques afectados: " + editSession.getBlockChangeCount());

        } catch (Exception e) {
            e.printStackTrace();
            p.sendMessage(prefix + "Ha ocurrido un error. Por favor selecciona el área nuevamente e inténtalo de nuevo.");
        }
        return true;
    }

    private int getGridX(@NotNull Vector2D vector, int minX, int maxX, int gridWidth) {
        double gridX = NumberUtils.getInNewRange(
                minX,
                maxX,
                0,
                gridWidth,
                vector.getBlockX()
        );
        return (int) Math.floor(gridX);
    }

    private int getGridZ(@NotNull Vector2D vector, int minZ, int maxZ, int gridHeight) {
        double gridZ = NumberUtils.getInNewRange(
                minZ,
                maxZ,
                0,
                gridHeight,
                vector.getBlockZ()
        );
        return (int) Math.floor(gridZ);
    }
}

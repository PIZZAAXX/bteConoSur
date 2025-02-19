package pizzaaxx.bteconosur.WorldEdit.Commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.util.*;

public class TerraformCommand implements CommandExecutor, TabCompleter {

    private final BTEConoSur plugin;
    private final String prefix;

    public TerraformCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getWorldEdit().getPrefix();
    }

    private final Map<UUID, Polygonal2DRegion> borderRegions = new HashMap<>();
    private final Map<UUID, Collection<Vector>> heightPoints = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        if (args.length < 1) {
            p.sendMessage(prefix + "Introduce un subcomando o un patrón de bloques.");
            return true;
        }

        switch (args[0]) {
            case "border": {

                Region region;
                try {
                    region = plugin.getWorldEdit().getSelection(p);
                } catch (IncompleteRegionException e) {
                    p.sendMessage(prefix + "Selecciona un área poligonal.");
                    return true;
                }

                if (!(region instanceof Polygonal2DRegion)) {
                    p.sendMessage(prefix + "Selecciona un área poligonal.");
                    return true;
                }

                Polygonal2DRegion polyRegion = (Polygonal2DRegion) region;

                Polygonal2DRegion finalRegion = new Polygonal2DRegion(
                        plugin.getWorldEditWorld(),
                        polyRegion.getPoints(),
                        100,
                        100
                );

                this.borderRegions.put(p.getUniqueId(), finalRegion);
                p.sendMessage(prefix + "Bordes guardados.");
                break;
            }
            case "height": {

                Region region = plugin.getWorldEdit().getIncompleteSelection(p);

                if (!(region instanceof ConvexPolyhedralRegion)) {
                    p.sendMessage(prefix + "Selecciona un área §oconvex§f.");
                    return true;
                }

                ConvexPolyhedralRegion convexRegion = (ConvexPolyhedralRegion) region;

                if (convexRegion.getVertices().size() < 2) {
                    p.sendMessage(prefix + "Selecciona al menos 2 puntos de altura.");
                    return true;
                }

                this.heightPoints.put(p.getUniqueId(), convexRegion.getVertices());
                p.sendMessage(prefix + "Alturas guardadas.");
                break;
            }
            case "desel": {
                this.borderRegions.remove(p.getUniqueId());
                this.heightPoints.remove(p.getUniqueId());
                p.sendMessage(prefix + "Selección eliminada.");
                break;
            }
            default: {

                if (!this.borderRegions.containsKey(p.getUniqueId())) {
                    p.sendMessage(prefix + "Selecciona los bordes de la región.");
                    return true;
                }

                if (!this.heightPoints.containsKey(p.getUniqueId())) {
                    p.sendMessage(prefix + "Selecciona al menos una altura dentro de la región.");
                    return true;
                }

                LocalSession localSession = plugin.getWorldEdit().getLocalSession(p);
                EditSession editSession = plugin.getWorldEdit().getWorldEdit().getEditSessionFactory().getEditSession(plugin.getWorldEditWorld(), localSession.getBlockChangeLimit());

                Mask mask = localSession.getMask();

                Pattern pattern;
                try {
                    pattern = plugin.getWorldEdit().getPattern(p, args[0]);
                } catch (InputParseException e) {
                    p.sendMessage(prefix + "Introduce un patrón de bloques válido.");
                    return true;
                }

                int steep = 2;

                Collection<Vector> heightPoints = this.heightPoints.get(p.getUniqueId());
                final Map<Integer, Map<Integer, Double>> calculated = new HashMap<>();
                for (BlockVector vctr : this.borderRegions.get(p.getUniqueId())) {
                    Vector vector = new Vector(vctr.getX(), 100, vctr.getZ());

                    double v;
                    if (calculated.containsKey(vector.getBlockX()) && calculated.get(vector.getBlockX()).containsKey(vector.getBlockZ())) {
                        v = calculated.get(vector.getBlockX()).get(vector.getBlockZ());
                    } else {
                        double num = 0;
                        double den = 0;

                        for (Vector point : heightPoints) {
                            Vector newPoint = new Vector(point).setY(100);
                            double w = 1/Math.pow(vector.distance(newPoint), steep);
                            num = num + (w * (point.getY() + 0.5));
                            den = den + w;
                        }

                        v = num/den;
                        Map<Integer, Double> temp = calculated.getOrDefault(vector.getBlockX(), new HashMap<>());
                        temp.put(vector.getBlockZ(), v);
                        calculated.put(vector.getBlockX(), temp);
                    }

                    double vMin;

                    // MINS
                    List<Double> possibleMins = new ArrayList<>();

                    for (double x = vector.getX() - 1; x <= vector.getX() + 1; x++) {
                        for (double z = vector.getZ() - 1; z <= vector.getZ() + 1; z++) {
                            Vector newVector = new Vector(x, 100, z);
                            double v2;
                            if (calculated.containsKey(newVector.getBlockX()) && calculated.get(newVector.getBlockX()).containsKey(vector.getBlockZ())) {
                                v2 = calculated.get(newVector.getBlockX()).get(vector.getBlockZ());
                            } else {
                                double num = 0;
                                double den = 0;

                                for (Vector point : heightPoints) {
                                    Vector newPoint = new Vector(point).setY(100);
                                    double w = 1/Math.pow(newVector.distance(newPoint), steep);
                                    num = num + (w * (point.getY() + 0.5));
                                    den = den + w;
                                }

                                v2 = num/den;
                                Map<Integer, Double> temp = calculated.getOrDefault(newVector.getBlockX(), new HashMap<>());
                                temp.put(newVector.getBlockZ(), v2);
                                calculated.put(newVector.getBlockX(), temp);
                            }
                            possibleMins.add(v2);
                        }
                    }
                    vMin = Collections.min(possibleMins);

                    for (double y = vMin; y <= v; y++) {
                        Vector finalVector = new Vector(vector.getX(), y, vector.getZ());

                        if (s.canBuild(new Location(plugin.getWorld(), vector.getX(), y, vector.getZ()))) {
                            if (mask != null && !mask.test(finalVector)) {
                                continue;
                            }
                            try {
                                editSession.setBlock(finalVector, pattern.apply(finalVector));
                            } catch (MaxChangedBlocksException e) {
                                localSession.remember(editSession);
                                p.sendMessage(prefix + "Límite de bloques alcanzado.");
                                return true;
                            }
                        }
                    }
                }
                localSession.remember(editSession);
                p.sendMessage(prefix + "Operación completada. §7Bloques afectados: " + editSession.getBlockChangeCount());
                this.borderRegions.remove(p.getUniqueId());
                this.heightPoints.remove(p.getUniqueId());
                break;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(
                    Arrays.asList(
                            "border", "height", "desel"
                    )
            );
        }

        List<String> finalCompletions = new ArrayList<>();
        for (String completion : completions) {
            if (completion.startsWith(args[args.length - 1])) {
                finalCompletions.add(completion);
            }
        }
        Collections.sort(finalCompletions);
        return finalCompletions;
    }
}

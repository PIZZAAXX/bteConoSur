package pizzaaxx.bteconosur.WorldEdit.Commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DivideCommand implements CommandExecutor {

    private final String prefix;

    private final BTEConoSur plugin;

    public DivideCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getWorldEdit().getPrefix();
    }

    private static class VectorDistanceComparator implements Comparator<Vector> {

        private final Vector origin;

        private VectorDistanceComparator(Vector origin) {
            this.origin = origin;
        }

        @Override
        public int compare(Vector o1, Vector o2) {
            return Double.compare(origin.distance(o1), origin.distance(o2));
        }
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
            p.sendMessage(prefix + "Introduce una cantidad de divisiones.");
            return true;
        }

        int divisions;
        try {
            divisions = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            p.sendMessage(prefix + "Introduce un número válido.");
            return true;
        }

        if (divisions <= 0) {
            p.sendMessage(prefix + "Introduce un número igual o mayor a 1.");
            return true;
        }

        Region region;
        try {
            region = plugin.getWorldEdit().getSelection(p);
        } catch (IncompleteRegionException e) {
            p.sendMessage(prefix + "Selecciona un área cúbica.");
            return true;
        }

        if (!(region instanceof CuboidRegion)) {
            p.sendMessage(prefix + "Selecciona un área cúbica.");
            return true;
        }

        CuboidRegion cuboidRegion = (CuboidRegion) region;

        Set<Vector> points = plugin.getWorldEdit().getBlocksInLine(cuboidRegion.getPos1(), cuboidRegion.getPos2());
        int blockAmount = points.size();

        if (divisions > blockAmount) {
            p.sendMessage(prefix + "El número de divisiones es mayor que el número de bloques en la línea.");
            return true;
        }

        List<Integer> blocksPerSection = new ArrayList<>();

        int floor = Math.floorDiv(blockAmount, divisions);

        for (int i = 0; i < divisions; i++) {
            blocksPerSection.add(floor);
        }

        int missing = blockAmount - (floor * divisions);

        for (int i = 0; i < missing; i++) {
            int index;
            if (i % 2 == 0) {
                index = Math.floorDiv(i, 2);
            } else {
                index = divisions - 1 - Math.floorDiv(i, 2);
            }
            int section = blocksPerSection.get(index);
            section++;
            blocksPerSection.set(index, section);
        }

        LocalSession localSession = plugin.getWorldEdit().getLocalSession(p);
        EditSession editSession = plugin.getWorldEdit().getWorldEdit().getEditSessionFactory().getEditSession(plugin.getWorldEditWorld(), localSession.getBlockChangeLimit());

        Mask mask = localSession.getMask();

        List<Vector> orderedPoints = new ArrayList<>(points);
        orderedPoints.sort(new VectorDistanceComparator(cuboidRegion.getPos1()));

        int evenOrOddCounter = 0;
        int counter = 0;
        for (Integer amount : blocksPerSection) {
            for (int i = 0; i < amount; i++) {
                Vector point = orderedPoints.get(counter);

                if (!s.canBuild(
                        new Location(
                                plugin.getWorld(),
                                point.getX(),
                                point.getY(),
                                point.getZ()
                        )
                )) {
                    continue;
                }
                if (mask != null && !(mask.test(point))) {
                    continue;
                }
                try {
                    editSession.setBlock(point, new BaseBlock((evenOrOddCounter % 2 == 0 ? 41 : 57)));
                } catch (MaxChangedBlocksException e) {
                    localSession.remember(editSession);
                    p.sendMessage(prefix + "Límite de bloques alcanzado.");
                    return true;
                }
                counter++;
            }
            evenOrOddCounter++;
        }
        localSession.remember(editSession);
        p.sendMessage(prefix + "Division creada. §7Bloques afectados: " + editSession.getBlockChangeCount());
        return true;
    }
}

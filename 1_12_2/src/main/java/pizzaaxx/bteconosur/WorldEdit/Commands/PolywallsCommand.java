package pizzaaxx.bteconosur.WorldEdit.Commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PolywallsCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public PolywallsCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        try {

            if (args.length < 1) {
                p.sendMessage(plugin.getWorldEdit().getPrefix() + "Introduce un patrón de bloques.");
                return true;
            }

            LocalSession localSession = plugin.getWorldEdit().getLocalSession(p);
            Mask mask = localSession.getMask();

            Pattern pattern = plugin.getWorldEdit().getPattern(p, args[0]);


            List<BlockVector2D> points = plugin.getWorldEdit().getSelectionPoints(p);
            int minY = plugin.getWorldEdit().getSelection(p).getMinimumPoint().getBlockY();
            int maxY = plugin.getWorldEdit().getSelection(p).getMaximumPoint().getBlockY();

            List<BlockVector2D> finalPoints = new ArrayList<>(points);

            boolean skipLast = true;
            if (args.length == 1 || !(args[1].equals("-last") || args[1].equals("-l"))) {
                skipLast = false;
                finalPoints.add(points.get(0));
            }


            EditSession editSession = plugin.getWorldEdit().getWorldEdit().getEditSessionFactory().getEditSession(plugin.getWorldEditWorld(), localSession.getBlockChangeLimit());

            for (int i = minY; i <= maxY; i++) {
                for (int j = 0; j < (skipLast ? points.size() - 1 : points.size()); j++) {
                    BlockVector2D pos1 = finalPoints.get(j);
                    BlockVector2D pos2 = finalPoints.get(j + 1);

                    for (Vector point : plugin.getWorldEdit().getBlocksInLine(pos1.toVector(i), pos2.toVector(i))) {
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
                        editSession.setBlock(point, pattern.apply(point));
                    }
                }
            }

            localSession.remember(editSession);

            p.sendMessage(plugin.getWorldEdit().getPrefix() + "Paredes creadas. §7Bloques afectados: " + editSession.getBlockChangeCount());

        } catch (IncompleteRegionException e) {
            p.sendMessage(plugin.getWorldEdit().getPrefix() + "Este comando solo se puede usar con selecciones cúbicas o poligonales.");
        } catch (InputParseException e) {
            p.sendMessage(plugin.getWorldEdit().getPrefix() + "Introduce un patrón de bloques válido.");
        } catch (MaxChangedBlocksException e) {
            p.sendMessage(plugin.getWorldEdit().getPrefix() + "Límite de cambio de bloques alcanzado.");
        }

        return true;
    }
}

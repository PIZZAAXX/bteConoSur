package pizzaaxx.bteconosur.WorldEdit.Selection;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.*;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.EllipsoidRegionSelector;
import com.sk89q.worldedit.regions.selector.ExtendingCuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.world.World;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

import java.util.*;

public class SelUndoRedoCommand implements CommandExecutor, Listener {

    private final BTEConoSur plugin;

    public SelUndoRedoCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, List<RegionSelector>> undoSteps = new HashMap<>();
    private final Map<UUID, List<RegionSelector>> redoSteps = new HashMap<>();
    private final Map<UUID, Long> lastQuit = new HashMap<>();
    private final Map<UUID, RegionSelector> pendingCommands = new HashMap<>();
    private final Map<UUID, RegionSelector> pendingInteractions = new HashMap<>();
    private final Map<UUID, RegionSelector> pendingShortcuts = new HashMap<>();

    public void onShortcutBefore(Player player) {

        RegionSelector region = plugin.getWorldEdit().getLocalSession(player).getRegionSelector(plugin.getWorldEditWorld());

        pendingShortcuts.put(player.getUniqueId(), cloneSelector(region));

    }

    public void onShortcutAfter(@NotNull Player player) {

        RegionSelector beforeRegion = pendingShortcuts.get(player.getUniqueId());

        RegionSelector afterRegion = plugin.getWorldEdit().getLocalSession(player).getRegionSelector(plugin.getWorldEditWorld());

        if (!compareRegionSelectors(beforeRegion, afterRegion)) {

            addUndoStep(player, cloneSelector(beforeRegion));

        }

        pendingShortcuts.remove(player.getUniqueId());


    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractBefore(@NotNull PlayerInteractEvent event) {

        if (event.getItem() != null && event.getItem().getType() == Material.WOOD_AXE && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            RegionSelector region = plugin.getWorldEdit().getLocalSession(event.getPlayer()).getRegionSelector(plugin.getWorldEditWorld());

            pendingInteractions.put(event.getPlayer().getUniqueId(), cloneSelector(region));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAfter(@NotNull PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.WOOD_AXE && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            RegionSelector beforeRegion = pendingInteractions.get(event.getPlayer().getUniqueId());

            RegionSelector afterRegion = plugin.getWorldEdit().getLocalSession(event.getPlayer()).getRegionSelector(plugin.getWorldEditWorld());

            if (!compareRegionSelectors(beforeRegion, afterRegion)) {

                addUndoStep(event.getPlayer(), cloneSelector(beforeRegion));

            }

            pendingInteractions.remove(event.getPlayer().getUniqueId());

        }
    }

    private final List<String> COMMANDS = Arrays.asList(
            "//sel", "//desel", "//deselect", "//pos1", "//pos2", "//hpos1", "//hpos2", "//chunk", "//expand", "//contract", "//outset", "//inset", "//shift"
    );

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandBefore(@NotNull PlayerCommandPreprocessEvent event) {

        if (COMMANDS.contains(event.getMessage().split(" ")[0])) {

            RegionSelector region = plugin.getWorldEdit().getLocalSession(event.getPlayer()).getRegionSelector(plugin.getWorldEditWorld());

            pendingCommands.put(event.getPlayer().getUniqueId(), cloneSelector(region));
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandAfter(@NotNull PlayerCommandPreprocessEvent event) {

        if (COMMANDS.contains(event.getMessage().split(" ")[0])) {

            RegionSelector beforeRegion = pendingCommands.get(event.getPlayer().getUniqueId());

            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    RegionSelector afterRegion = plugin.getWorldEdit().getLocalSession(event.getPlayer()).getRegionSelector(plugin.getWorldEditWorld());

                    // STOPS HERE
                    if (!compareRegionSelectors(beforeRegion, afterRegion)) {

                        addUndoStep(event.getPlayer(), cloneSelector(beforeRegion));

                    }

                }
            };

            pendingCommands.remove(event.getPlayer().getUniqueId());

            runnable.runTaskLaterAsynchronously(plugin, 5);

        }

    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        lastQuit.put(uuid, System.currentTimeMillis());

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {

                if (System.currentTimeMillis() - lastQuit.get(uuid) >= 550000) {
                    lastQuit.remove(uuid);
                    undoSteps.remove(uuid);
                    redoSteps.remove(uuid);
                }

            }
        };

        runnable.runTaskLaterAsynchronously(plugin, 12000);

    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        World world = plugin.getWorldEditWorld();

        if (command.getName().equals("/selundo")) {

            if (!undoSteps.containsKey(p.getUniqueId()) || undoSteps.get(p.getUniqueId()).isEmpty()) {

                p.sendMessage("§dNo hay nada que deshacer.");
                return true;

            }

            com.sk89q.worldedit.entity.Player actor = plugin.getWorldEdit().getWEPlayer(p);

            List<RegionSelector> steps = undoSteps.get(p.getUniqueId());
            RegionSelector selector = steps.get(steps.size() - 1);

            LocalSession localSession = plugin.getWorldEdit().getLocalSession(p);

            List<RegionSelector> redoRegions = redoSteps.getOrDefault(p.getUniqueId(), new ArrayList<>());
            redoRegions.add(cloneSelector(localSession.getRegionSelector(world)));
            redoSteps.put(p.getUniqueId(), redoRegions);

            localSession.setRegionSelector(plugin.getWorldEditWorld(), selector);
            localSession.dispatchCUISelection(actor);

            steps.remove(steps.size() - 1);
            undoSteps.put(p.getUniqueId(), steps);

            p.sendMessage("§dÚltima selección deshecha.");
        }

        if (command.getName().equals("/selredo")) {

            if (!redoSteps.containsKey(p.getUniqueId()) || redoSteps.get(p.getUniqueId()).isEmpty()) {

                p.sendMessage("§dNo hay nada que rehacer.");
                return true;

            }

            com.sk89q.worldedit.entity.Player actor = plugin.getWorldEdit().getWEPlayer(p);

            List<RegionSelector> steps = redoSteps.get(p.getUniqueId());
            RegionSelector selector = steps.get(steps.size() - 1);

            LocalSession localSession = plugin.getWorldEdit().getLocalSession(p);

            List<RegionSelector> undoRegions = undoSteps.getOrDefault(p.getUniqueId(), new ArrayList<>());
            undoRegions.add(cloneSelector(localSession.getRegionSelector(world)));
            undoSteps.put(p.getUniqueId(), undoRegions);

            localSession.setRegionSelector(plugin.getWorldEditWorld(), selector);
            localSession.dispatchCUISelection(actor);

            steps.remove(steps.size() - 1);
            redoSteps.put(p.getUniqueId(), steps);

            p.sendMessage("§dÚltima selección rehecha.");
        }

        return true;
    }

    public RegionSelector cloneSelector(RegionSelector selector) {

        if (selector instanceof CuboidRegion) {

            return new CuboidRegionSelector(selector);

        } else if (selector instanceof Polygonal2DRegionSelector) {

            return new Polygonal2DRegionSelector(selector);

        } else if (selector instanceof SphereRegionSelector) {

            return new SphereRegionSelector(selector);

        } else if (selector instanceof EllipsoidRegionSelector) {

            return new EllipsoidRegionSelector(selector);

        } else if (selector instanceof CylinderRegionSelector) {

            return new CylinderRegionSelector(selector);

        } else if (selector instanceof ExtendingCuboidRegionSelector) {

            return new ExtendingCuboidRegionSelector(selector);

        } else {

            return new ConvexPolyhedralRegionSelector(selector);

        }

    }

    public boolean compareRegionSelectors(RegionSelector r1, RegionSelector r2) {

        if (r1 == r2) {
            return true;
        }

        if (r1 instanceof CuboidRegionSelector && r2 instanceof  CuboidRegionSelector) {

            CuboidRegion c1 = ((CuboidRegionSelector) r1).getIncompleteRegion();
            CuboidRegion c2 = ((CuboidRegionSelector) r2).getIncompleteRegion();

            boolean equal1 = false;
            boolean equal2 = false;

            if (c1.getPos1() == null && c2.getPos1() == null) {
                equal1 = true;
            } else if (c1.getPos1() != null && c2.getPos1() != null) {
                equal1 = c1.getPos1().equals(c2.getPos1());
            }

            if (c1.getPos2() == null && c2.getPos2() == null) {
                equal2 = true;
            } else if (c1.getPos2() != null && c2.getPos2() != null) {
                equal2 = c1.getPos2().equals(c2.getPos2());
            }

            return (equal1 && equal2);

        } else if (r1 instanceof Polygonal2DRegionSelector && r2 instanceof Polygonal2DRegionSelector) {

            Polygonal2DRegion p1 = ((Polygonal2DRegionSelector) r1).getIncompleteRegion();
            Polygonal2DRegion p2 = ((Polygonal2DRegionSelector) r2).getIncompleteRegion();

            if (p1.getMaximumY() != p2.getMaximumY() || p1.getMinimumY() != p2.getMinimumY() || p1.getPoints().size() != p2.getPoints().size()) {
                return false;
            }

            for (int i = 0; i < p1.getPoints().size(); i++) {

                BlockVector2D v1 = p1.getPoints().get(i);
                BlockVector2D v2 = p2.getPoints().get(i);

                if (!v1.equals(v2)) {

                    return false;

                }

            }

            return true;
        } else if (r1 instanceof EllipsoidRegionSelector && r2 instanceof EllipsoidRegionSelector) {

            EllipsoidRegion e1 = ((EllipsoidRegionSelector) r1).getIncompleteRegion();
            EllipsoidRegion e2 = ((EllipsoidRegionSelector) r2).getIncompleteRegion();

            boolean equalCenter = false;
            boolean equalRadius = false;

            if (e1.getCenter() == null && e2.getCenter() == null) {
                equalCenter = true;
            } else if (e1.getCenter() != null && e2.getCenter() != null) {
                equalCenter = e1.getCenter().equals(e2.getCenter());
            }

            if (e1.getRadius() == null && e2.getRadius() == null) {
                equalRadius = true;
            } else if (e1.getRadius() != null && e2.getRadius() != null) {
                equalRadius = e1.getRadius().equals(e2.getRadius());
            }

            return (equalCenter && equalRadius);

        } else if (r1 instanceof CylinderRegionSelector && r2 instanceof CylinderRegionSelector) {

            CylinderRegion c1 = ((CylinderRegionSelector) r1).getIncompleteRegion();
            CylinderRegion c2 = ((CylinderRegionSelector) r2).getIncompleteRegion();

            if (c1.getMaximumY() != c2.getMaximumY() || c1.getMinimumY() != c2.getMinimumY()) {
                return false;
            }

            boolean equalCenter = false;
            boolean equalRadius = false;

            if (c1.getCenter() == null && c2.getCenter() == null) {
                equalCenter = true;
            } else if (c1.getCenter() != null && c2.getCenter() != null) {
                equalCenter = c1.getCenter().equals(c2.getCenter());
            }

            if (c1.getRadius() == null && c2.getRadius() == null) {
                equalRadius = true;
            } else if (c1.getRadius() != null && c2.getRadius() != null) {
                equalRadius = c1.getRadius().equals(c2.getRadius());
            }

            return (equalCenter && equalRadius);

        } else if (r1 instanceof ConvexPolyhedralRegionSelector && r2 instanceof ConvexPolyhedralRegionSelector) {

            ConvexPolyhedralRegion c1 = (ConvexPolyhedralRegion) r1.getIncompleteRegion();
            ConvexPolyhedralRegion c2 = (ConvexPolyhedralRegion) r2.getIncompleteRegion();

            if (c1.getVertices().size() != c2.getVertices().size()) {
                return false;
            }

            List<Vector> p1 = new ArrayList<>(c1.getVertices());
            List<Vector> p2 = new ArrayList<>(c2.getVertices());

            for (int i = 0; i < c1.getVertices().size(); i++) {

                if (!p1.get(i).equals(p2.get(i))) {

                    return false;

                }

            }

            return true;


        }

        return false;
    }

    public void addUndoStep(@NotNull Player player, RegionSelector selector) {


        List<RegionSelector> steps = undoSteps.getOrDefault(player.getUniqueId(), new ArrayList<>());

        if (steps.isEmpty() || !compareRegionSelectors(steps.get(steps.size() - 1), selector)) {

            steps.add(selector);

            List<RegionSelector> finalSteps;

            if (steps.size() > 15) {

                finalSteps = steps.subList(1, 16);

            } else {

                finalSteps = steps;

            }

            undoSteps.put(player.getUniqueId(), finalSteps);

            redoSteps.remove(player.getUniqueId());
        }


    }
}

package pizzaaxx.bteconosur.Scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.Managers.ScoreboardManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;

import java.sql.SQLException;
import java.util.*;

public class ScoreboardHandler {

    private final List<Class<? extends ScoreboardDisplay>> AUTO_ORDER = Arrays.asList(
            BTEConoSur.class, Country.class, City.class, Project.class, ServerPlayer.class
    );

    private final BTEConoSur plugin;
    private final Set<UUID> automaticPlayers = new HashSet<>();
    private final Map<Class<? extends ScoreboardDisplay>, Set<UUID>> displays = new HashMap<>();

    public ScoreboardHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() {

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : automaticPlayers) {
                    ServerPlayer s = plugin.getPlayerRegistry().get(uuid);
                    int currentIndex = (AUTO_ORDER.contains(s.getScoreboardManager().getDisplayClass()) ? AUTO_ORDER.indexOf(s.getScoreboardManager().getDisplayClass()) : -1);
                    int targetIndex = (currentIndex >= AUTO_ORDER.size() - 1 ? 0 : currentIndex + 1);
                    try {
                        s.getScoreboardManager().setDisplay(getDisplay(AUTO_ORDER.get(targetIndex), s, Bukkit.getPlayer(uuid).getLocation()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        runnable.runTaskTimer(plugin, 200, 200);

    }

    public void registerAuto(UUID uuid) {
        automaticPlayers.add(uuid);
    }

    public void unregisterAuto(UUID uuid) {
        automaticPlayers.remove(uuid);
    }

    public void registerDisplay(UUID uuid, @NotNull ScoreboardDisplay display) {

        Set<UUID> uuids = displays.getOrDefault(display.getClass(), new HashSet<>());
        uuids.add(uuid);
        displays.put(display.getClass(), uuids);

    }

    public void unregisterDisplay(UUID uuid) {
        for (Class<? extends ScoreboardDisplay> clazz : displays.keySet()) {
            Set<UUID> uuids = displays.get(clazz);
            if (uuids.contains(uuid)) {
                uuids.remove(uuid);
                displays.put(clazz, uuids);
            }
        }
    }

    @Contract(pure = true)
    public Class<? extends ScoreboardDisplay> getClass(@NotNull String input) {

        switch (input) {
            case "me":
                return ServerPlayer.class;
            case "project":
                return Project.class;
            case "city":
                return City.class;
            case "top":
                return Country.class;
            default:
                return BTEConoSur.class;
        }
    }

    public void update(@NotNull ScoreboardDisplay display) throws SQLException {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());
            ScoreboardManager manager = s.getScoreboardManager();
            if (manager.getDisplay().getClass() == display.getClass()) {
                if (manager.getDisplay().getScoreboardID().equals(display.getScoreboardID())) {
                    manager.setDisplay(display);
                }
            }
        }
    }

    @NotNull
    public ScoreboardDisplay getDisplay(Class<? extends ScoreboardDisplay> clazz, @NotNull ServerPlayer s, Location loc) {

        if (clazz == Country.class) {
            Country country = plugin.getCountryManager().getCountryAt(loc);
            if (country != null) {
                return country;
            }
            return new NotFoundDisplay(
                    Country.class,
                    "top",
                    "§4§lTop",
                    "§cNo estás dentro de un país."
            );
        } else if (clazz == City.class) {

            City city = plugin.getCityManager().getCityAt(loc);
            if (city != null) {
                return city;
            }
            return new NotFoundDisplay(
                    City.class,
                    "city",
                    "§4§lCiudad",
                    "§cNo estás dentro de una ciudad."
            );
        } else if (clazz == Project.class) {

            List<String> ids = plugin.getProjectRegistry().getProjectsAt(loc);
            Collections.sort(ids);

            if (!ids.isEmpty()) {
                return plugin.getProjectRegistry().get(ids.get(0));
            }
            return new NotFoundDisplay(
                    Project.class,
                    "project",
                    "§4§lProyecto",
                    "§cNo estás dentro de un proyecto."
            );

        } else if (clazz == ServerPlayer.class) {
            return s;
        }

        return plugin;
    }
}

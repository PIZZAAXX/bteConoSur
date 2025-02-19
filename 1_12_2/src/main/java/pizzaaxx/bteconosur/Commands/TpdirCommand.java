package pizzaaxx.bteconosur.Commands;

import com.sk89q.worldedit.Vector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Inventory.InventoryGUI;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TpdirCommand implements CommandExecutor, Prefixable {

    private final BTEConoSur plugin;

    public TpdirCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 1) {
            p.sendMessage(this.getPrefix() + "Introduce una direcci´ón.");
            return true;
        }

        String dir = String.join("+", args);

        try {
            List<Map<String, Object>> options = plugin.getJSONMapper().readValue(new URL("https://nominatim.openstreetmap.org/search?q=" + dir + "&format=json"), ArrayList.class);

            List<String> nameOptions = new ArrayList<>();
            List<Coords2D> coordOptions = new ArrayList<>();
            List<Location> locations = new ArrayList<>();

            for (Map<String, Object> option : options) {
                Coords2D coords = new Coords2D(
                        plugin,
                        Double.parseDouble(option.get("lat").toString()),
                        Double.parseDouble(option.get("lon").toString())
                );

                if (plugin.getCountryManager().isInsideCountry(coords.toHighestLocation())) {
                    boolean near = false;
                    Location h = coords.toHighestLocation();
                    for (Location loc : locations) {
                        int xDif = Math.abs(loc.getBlockX() - h.getBlockX());
                        int zDif = Math.abs(loc.getBlockZ() - h.getBlockZ());

                        if (xDif > 100 || zDif > 100) {
                            continue;
                        }

                        if (Math.pow(xDif, 2) + Math.pow(zDif, 2) < 10000) {
                            near = true;
                            break;
                        }
                    }

                    if (!near) {
                        nameOptions.add(option.get("display_name").toString());
                        coordOptions.add(coords);
                        locations.add(h);
                    }
                }
            }

            int optionsSize = nameOptions.size();

            if (optionsSize > 1) {
                int rows = Math.floorDiv(optionsSize, 9) + 3;

                InventoryGUI gui = new InventoryGUI(
                        rows,
                        "Selecciona una opción"
                );

                for (int i = 0; i < (rows - 2) * 7; i++) {
                    int realSlot = i + (2 * Math.floorDiv(i, 7)) + 1 + 9;
                    if (i < optionsSize) {
                        gui.setItem(
                                new ItemBuilder(Material.MAP, 1, 1)
                                        .name("§a§n" + nameOptions.get(i))
                                        .lore("Haz click para teletransportarte.")
                                        .build(),
                                realSlot
                        );
                        int finalI = i;
                        gui.setLCAction(
                                event -> {
                                    event.getPlayer().closeInventory();
                                    Coords2D coord = coordOptions.get(finalI);
                                    plugin.getWorldEditWorld().checkLoadedChunk(new Vector(
                                            coord.getX(),
                                            0,
                                            coord.getZ()
                                    ));
                                    Location loc = coord.toHighestLocation();
                                    if (loc.getY() == 0) {
                                        loc.add(0, 100, 0);
                                    }
                                    event.getPlayer().teleport(loc);
                                    event.getPlayer().sendMessage(this.getPrefix() + "Teletransportándote a §a" + nameOptions.get(finalI).split(",")[0] + "§f.");
                                },
                                realSlot
                        );
                    } else {
                        gui.setEmptySlot(realSlot);
                    }
                }
                plugin.getInventoryHandler().open(p, gui);
            } else if (optionsSize == 1) {
                Coords2D coord = coordOptions.get(0);
                Location loc = coord.toHighestLocation();
                plugin.getWorldEditWorld().checkLoadedChunk(new Vector(
                        coord.getX(),
                        0,
                        coord.getZ()
                ));
                if (loc.getY() == 0) {
                    loc.add(0, 100, 0);
                }
                p.teleport(loc);
                p.sendMessage(this.getPrefix() + "Teletransportándote a §a" + nameOptions.get(0).split(",")[0] + "§f.");
            } else {
                p.sendMessage(this.getPrefix() + "No se han encontrado lugares dentro del Cono Sur con ese nombre.");
            }


        } catch (IOException e) {
            p.sendMessage(this.getPrefix() + "Ha ocurrido un error.");
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public String getPrefix() {
        return StringUtils.getGenericPrefix("2", "TPDIR");
    }
}

package pizzaaxx.bteconosur.Cities.Commands;

import clipper2.Clipper;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.SQLParser;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class CitiesCommand implements CommandExecutor, Prefixable {

    private final BTEConoSur plugin;

    public CitiesCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        // /city create cl puertoVaras Puerto Varas ✔️
        // /city setUrban cl puertoVaras
        // /city deleteUrban cl puertoVaras

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        if (!s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
            p.sendMessage("Solo administradores pueden manejar las ciudades.");
            return true;
        }

        if (args.length < 1) {
            p.sendMessage(this.getPrefix() + "Introduce un subcomando.");
            return true;
        }

        switch (args[0]) {
            case "create": {
                if (args.length < 2) {
                    p.sendMessage(this.getPrefix() + "Introduce el código de un país.");
                    return true;
                }

                String countryAbbreviation = args[1];
                if (!plugin.getCountryManager().exists(countryAbbreviation)) {
                    p.sendMessage(this.getPrefix() + "El pais introducido no existe.");
                    return true;
                }

                Country country = plugin.getCountryManager().get(countryAbbreviation);

                if (!s.getProjectManager().hasAdminPermission(country)) {
                    p.sendMessage("No puedes manejar ciudades de este país.");
                    return true;
                }

                if (args.length < 3) {
                    p.sendMessage(this.getPrefix() + "Introduce un nombre.");
                    return true;
                }

                String name = args[2];

                if (!name.matches("[a-z]{1,32}")) {
                    p.sendMessage(this.getPrefix() + "Introduce un nombre válido. Solo minúsculas, sin espacios ni guiones bajos.");
                    return true;
                }

                if (plugin.getCityManager().exists(name)) {
                    p.sendMessage(this.getPrefix() + "El nombre usado ya está en uso.");
                    return true;
                }

                if (args.length < 4) {
                    p.sendMessage(this.getPrefix() + "Introduce un nombre para mostrar.");
                    return true;
                }

                String displayName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                if (!displayName.matches("[A-Za-zÁÉÍÓÚáéíóúÑñ\\-_., \\d]{1,32}")) {
                    p.sendMessage("Nombre a mostrar inválido.");
                    return true;
                }

                List<BlockVector2D> points;
                try {
                    points = plugin.getWorldEdit().getSelectionPoints(p);
                } catch (IncompleteRegionException e) {
                    p.sendMessage(this.getPrefix() + "Selecciona un área cúbica o poligonal completa.");
                    return true;
                }

                Map<String, Object> json = new HashMap<>();
                json.put("type", "minecraft");

                List<List<Integer>> coordinates = new ArrayList<>();
                for (BlockVector2D vector : points) {
                    List<Integer> point = new ArrayList<>();
                    point.add(vector.getBlockX());
                    point.add(vector.getBlockZ());
                    coordinates.add(point);
                }
                json.put("coordinates", coordinates);

                File target = new File(plugin.getDataFolder(), "cities/" + name + ".json");
                try {
                    if (target.createNewFile()) {

                        FileWriter writer = new FileWriter(target);
                        writer.write(SQLParser.getString(json, true));
                        writer.close();

                    } else {
                        p.sendMessage("Ha ocurrido un error.");
                    }
                } catch (IOException e) {
                    p.sendMessage("Ha ocurrido un error.");
                }

                try {
                    plugin.getSqlManager().insert(
                            "cities",
                            new SQLValuesSet(
                                    new SQLValue(
                                            "name", name
                                    ),
                                    new SQLValue(
                                            "display_name", displayName
                                    ),
                                    new SQLValue(
                                            "country", country
                                    )
                            )
                    ).execute();
                } catch (SQLException e) {
                    target.delete();
                    p.sendMessage("Ha ocurrido un error.");
                }
                break;
            }
            case "setUrban": {
                if (args.length < 2) {
                    p.sendMessage(this.getPrefix() + "Introduce el código de un país.");
                    return true;
                }

                String countryAbbreviation = args[1];
                if (!plugin.getCountryManager().exists(countryAbbreviation)) {
                    p.sendMessage(this.getPrefix() + "El pais introducido no existe.");
                    return true;
                }

                Country country = plugin.getCountryManager().get(countryAbbreviation);

                if (!s.getProjectManager().hasAdminPermission(country)) {
                    p.sendMessage("No puedes manejar ciudades de este país.");
                    return true;
                }

                if (args.length < 3) {
                    p.sendMessage(this.getPrefix() + "Introduce un nombre.");
                    return true;
                }

                String name = args[2];

                if (!name.matches("[a-z]{1,32}")) {
                    p.sendMessage(this.getPrefix() + "Introduce un nombre válido. Solo minúsculas, sin espacios ni guiones bajos.");
                    return true;
                }

                if (!plugin.getCityManager().exists(name)) {
                    p.sendMessage(this.getPrefix() + "La ciudad introducida no existe.");
                    return true;
                }

                City city = plugin.getCityManager().get(name);
                assert city != null;

                Region selection;
                try {
                    selection = plugin.getWorldEdit().getSelection(p);
                } catch (IncompleteRegionException e) {
                    p.sendMessage(this.getPrefix() + "Selecciona una región poligonal válida.");
                    return true;
                }

                if (!(selection instanceof Polygonal2DRegion)) {
                    p.sendMessage(this.getPrefix() + "Selecciona una región poligonal válida.");
                    return true;
                }

                Polygonal2DRegion polygonal2DRegion = (Polygonal2DRegion) selection;

                Paths64 cityPoly = new Paths64();
                {
                    Path64 poly = new Path64();
                    for (BlockVector2D vector : city.getRegion().getPoints()) {
                        Point64 point = new Point64();
                        point.x = (long) vector.getX();
                        point.y = (long) vector.getZ();
                        poly.add(point);
                    }

                    BlockVector2D firstVector = city.getRegion().getPoints().get(0);
                    Point64 point = new Point64();
                    point.x = (long) firstVector.getX();
                    point.y = (long) firstVector.getZ();
                    poly.add(point);
                    cityPoly.add(poly);
                }

                Paths64 urbanPoly = new Paths64();
                {
                    Path64 poly = new Path64();
                    for (BlockVector2D vector : polygonal2DRegion.getPoints()) {
                        Point64 point = new Point64();
                        point.x = (long) vector.getX();
                        point.y = (long) vector.getZ();
                        poly.add(point);
                    }

                    BlockVector2D vector = polygonal2DRegion.getPoints().get(0);
                    Point64 point = new Point64();
                    point.x = (long) vector.getX();
                    point.y = (long) vector.getZ();
                    poly.add(point);
                    urbanPoly.add(poly);
                }

                Paths64 result = Clipper.Intersect(cityPoly, urbanPoly, FillRule.EvenOdd);

                File target = new File(plugin.getDataFolder(), "cities/" + name + ".json");
                if (!target.exists()) {
                    p.sendMessage(getPrefix() + "Ha ocurrido un error.");
                    return true;
                }
                try {
                    JsonNode node = plugin.getJSONMapper().readTree(target);

                    boolean geographical = node.path("type").asText().equals("geographic");

                    ObjectNode objNode = plugin.getJSONMapper().createObjectNode();
                    objNode.put("type", node.path("type").asText());
                    objNode.set("coordinates", node.path("coordinates"));

                    ArrayNode urbanNode = plugin.getJSONMapper().createArrayNode();
                    for (Point64 point : result.get(0)) {
                        ArrayNode pointNode = plugin.getJSONMapper().createArrayNode();
                        if (geographical) {
                            Coords2D coords = new Coords2D(
                                    plugin,
                                    new Location(
                                            plugin.getWorld(),
                                            point.x,
                                            100,
                                            point.y
                                    )
                            );
                            pointNode.add(coords.getLon());
                            pointNode.add(coords.getLat());
                        } else {
                            pointNode.add((double) point.x);
                            pointNode.add((double) point.y);
                        }
                        urbanNode.add(pointNode);
                    }
                    objNode.set("urban", urbanNode);

                    ObjectWriter writer = plugin.getJSONMapper().writer(new DefaultPrettyPrinter());
                    writer.writeValue(target, objNode);

                    plugin.getCityManager().reloadCity(name);

                    p.sendMessage(getPrefix() + "Zona urbana establecida.");

                } catch (IOException e) {
                    p.sendMessage(getPrefix() + "Ha ocurrido un error.");
                    return true;
                }

            }
        }
        return true;
    }

    @Override
    public String getPrefix() {
        return "§f[§9CIUDADES§f] §7>> §f";
    }
}

package pizzaaxx.bteconosur.Discord.SlashCommands;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Discord.FuzzyMatching.FuzzyMatcher;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.Utils.NumberUtils;
import pizzaaxx.bteconosur.Utils.WebMercatorUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression.Order.ASC;

public class CityFuzzyListener implements FuzzyMatcher {

    private final BTEConoSur plugin;

    public CityFuzzyListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void respond(MessageReceivedEvent event, @NotNull String input) throws SQLException, IOException {

        int length = input.length();

        ResultSet set = plugin.getSqlManager().select(
                "cities",
                new SQLColumnSet("name", "levenshtein('" + input + "',display_name) AS distance"),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "levenshtein('" + input + "',display_name)", "<=", (length == 1 ? 1 : (length == 2 || length == 3 ? 2 : (length == 4 || length == 5 ? 3 : (length == 6 || length == 7 ? 4 : 5))))
                        )
                ),
                new SQLOrderSet(
                        new SQLOrderExpression(
                                "distance", ASC
                        )
                )
        ).addText(" LIMIT 25").retrieve();

        List<String> names = new ArrayList<>();
        List<String> zeros = new ArrayList<>();
        while (set.next()) {
            if (set.getInt("distance") == 0) {
                zeros.add(set.getString("name"));
            }

            names.add(set.getString("name"));
        }
        if (!names.isEmpty()) {
            if (names.size() == 1) {
                this.respondCityEmbed(event, names.get(0));
            } else {
                if (zeros.isEmpty()) {
                    this.respondSelector(event, names);
                } else if (zeros.size() == 1) {
                    this.respondCityEmbed(event, zeros.get(0));
                } else {
                    this.respondSelector(event, zeros);
                }
            }
        }
    }

    public void respondSelector(@NotNull MessageReceivedEvent event, @NotNull List<String> names) {

        StringSelectMenu.Builder builder = StringSelectMenu.create("cityCommandSelector?user=" + event.getAuthor().getId());
        builder.setPlaceholder("Selecciona una ciudad");
        for (String name : names) {
            City city = plugin.getCityManager().get(name);
            builder.addOption(
                    city.getDisplayName(),
                    city.getName(),
                    city.getCountry().getEmoji()
            );
        }

        event.getMessage().reply("Se han encontrado **" + names.size() + "** opciones:").setComponents(
                ActionRow.of(
                        builder.build()
                ),
                ActionRow.of(
                        plugin.getDiscordHandler().getDeleteButton(event.getAuthor())
                )
        ).queue();
    }

    private void respondCityEmbed(MessageReceivedEvent event, String cityName) throws SQLException {

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.GREEN);

        City city = plugin.getCityManager().get(cityName);

        builder.setTitle(city.getDisplayName() + ", " + city.getCountry().getDisplayName());

        ProtectedPolygonalRegion region = (ProtectedPolygonalRegion) (city.hasUrbanArea() ? city.getUrbanRegion() : city.getRegion());

        double finishedArea = city.getFinishedArea() / 1000000.0;
        double totalArea = city.getTotalArea() / 1000000.0;
        double percentage = (finishedArea / totalArea) * 100;

        NumberFormat format = NumberFormat.getNumberInstance(Locale.GERMAN);

        builder.addField(
                ":straight_ruler: Área" + (city.hasUrbanArea() ? " urbana " : " ") + "terminada:",
                "`" + format.format(finishedArea) + " km² (" + format.format(Math.min(percentage, 100)) + "%)`",
                true
        );

        builder.addField(
                ":hammer_pick: Proy. en construcción",
                "`" + city.getClaimedProjectsAmount() + "`",
                true
        );

        builder.addField(
                ":white_check_mark: Proy. terminados",
                "`" + city.getFinishedProjectsAmount() + "`",
                true
        );

        builder.addField(
                ":art: Colores:",
                "```\uD83D\uDFE2 Disponible``````\uD83D\uDFE1 En construcción``````\uD83D\uDD35 Terminado```",
                false
        );

        FileUpload file;
        try {
            List<BlockVector2D> points = region.getPoints();
            List<Coords2D> coords = new ArrayList<>();
            points.forEach(point -> coords.add(new Coords2D(plugin, point)));

            double maxLon = coords.get(0).getLon();
            double minLon = coords.get(0).getLon();
            double maxLat = coords.get(0).getLat();
            double minLat = coords.get(0).getLat();

            for (Coords2D coord : coords) {

                if (maxLon < coord.getLon()) {
                    maxLon = coord.getLon();
                }

                if (minLon > coord.getLon()) {
                    minLon = coord.getLon();
                }

                if (maxLat < coord.getLat()) {
                    maxLat = coord.getLat();
                }

                if (minLat > coord.getLat()) {
                    minLat = coord.getLat();
                }

            }

            int zoom = 19;

            int maxTileX = Math.floorDiv((int) WebMercatorUtils.getXFromLongitude(maxLon, zoom), 256);
            int minTileX = Math.floorDiv((int) WebMercatorUtils.getXFromLongitude(minLon, zoom), 256);
            int maxTileY = Math.floorDiv((int) WebMercatorUtils.getYFromLatitude(maxLat, zoom), 256);
            int minTileY = Math.floorDiv((int) WebMercatorUtils.getYFromLatitude(minLat, zoom), 256);

            while (Math.abs(maxTileX - minTileX) > 10 || Math.abs(maxTileY - minTileY) > 10) {
                zoom--;

                maxTileX = Math.floorDiv((int) WebMercatorUtils.getXFromLongitude(maxLon, zoom), 256);
                minTileX = Math.floorDiv((int) WebMercatorUtils.getXFromLongitude(minLon, zoom), 256);
                maxTileY = Math.floorDiv((int) WebMercatorUtils.getYFromLatitude(minLat, zoom), 256);
                minTileY = Math.floorDiv((int) WebMercatorUtils.getYFromLatitude(maxLat, zoom), 256);

                if (zoom == 12) {
                    break;
                }
            }

            int xDif = Math.abs(maxTileX - minTileX);
            int yDif = Math.abs(maxTileY - minTileY);

            int xSize = 256 * (xDif + 1);
            int ySize = 256 * (yDif + 1);

            BufferedImage image = new BufferedImage(
                    xSize,
                    ySize,
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = image.createGraphics();

            double minImageLon = WebMercatorUtils.getLongitudeFromX(minTileX * 256, zoom);
            double maxImageLon = WebMercatorUtils.getLongitudeFromX((maxTileX + 1) * 256, zoom);
            double minImageLat = WebMercatorUtils.getLatitudeFromY(minTileY * 256, zoom);
            double maxImageLat = WebMercatorUtils.getLatitudeFromY((maxTileY + 1) * 256, zoom);

            GeneralPath clip = new GeneralPath();
            int counter = 0;
            for (Coords2D coord : coords) {

                double x = NumberUtils.getInNewRange(minImageLon, maxImageLon, 0, xSize, coord.getLon());
                double y = NumberUtils.getInNewRange(minImageLat, maxImageLat, 0, ySize, coord.getLat());

                if (counter == 0) {
                    clip.moveTo(x, y);
                } else {
                    clip.lineTo(x, y);
                }
                counter++;
            }
            clip.closePath();

            g.setClip(clip);

            for (int x = minTileX; x <= maxTileX; x++) {
                for (int y = minTileY; y <= maxTileY; y++) {

                    BufferedImage tile = plugin.getTerramapHandler().getTile(x, y, zoom);

                    g.drawImage(tile, (x - minTileX) * 256, (y - minTileY) * 256, null);

                }
            }

            g.dispose();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            file = FileUpload.fromData(is, "map.png");

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // IMAGE

        builder.setImage("attachment://map.png");

        event.getMessage().replyEmbeds(
                        builder.build()
                )
                .setContent(null)
                .addFiles(file)
                .addComponents(
                        ActionRow.of(
                                net.dv8tion.jda.api.interactions.components.buttons.Button.of(
                                        ButtonStyle.SUCCESS,
                                        "cityCommandViewPosts?name=" + cityName + "&user=" + event.getAuthor().getId(),
                                        "Ver publicaciones",
                                        Emoji.fromUnicode("U+1F4F8")
                                ),
                                net.dv8tion.jda.api.interactions.components.buttons.Button.of(
                                        ButtonStyle.PRIMARY,
                                        "cityCommandSearch?user=" + event.getAuthor().getId(),
                                        "Buscar otra ciudad",
                                        Emoji.fromUnicode("U+1F50E")
                                ),
                                plugin.getDiscordHandler().getDeleteButton(event.getAuthor())
                        )
                )
                .queue();
    }

    @Override
    public void onFuzzyMatch(@NotNull String message, String match, MessageReceivedEvent event) {
        String cityName = message.replace(match, "").replace("?","").replaceFirst(" ", "");
        try {
            this.respond(event, cityName);
        } catch (SQLException | IOException ignored) {}

    }

}

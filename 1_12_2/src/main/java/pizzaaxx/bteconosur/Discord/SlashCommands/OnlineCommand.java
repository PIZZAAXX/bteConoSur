package pizzaaxx.bteconosur.Discord.SlashCommands;

import com.sk89q.worldguard.util.net.HttpRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.ImageUtils;
import pizzaaxx.bteconosur.Utils.NumberUtils;
import pizzaaxx.bteconosur.Utils.WebMercatorUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OnlineCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public OnlineCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("online")) {
            try {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.GREEN);
                builder.setTitle("Jugadores online");
                builder.setDescription("Online: " + Bukkit.getOnlinePlayers().size());

                LinkedHashMap<Country, List<String>> namesMap = new LinkedHashMap<>();
                for (Country country : plugin.getCountryManager().getAllCountries()) {
                    namesMap.put(country, new ArrayList<>());
                }

                BufferedImage base = ImageIO.read(new File(plugin.getDataFolder(), "base_map.png"));

                Graphics2D g = base.createGraphics();

                double maxLat =  WebMercatorUtils.getLatitudeFromY(4*256, 3);
                double minLat =  WebMercatorUtils.getLatitudeFromY(5.5*256, 3);
                double minLon =  WebMercatorUtils.getLongitudeFromX(2*256, 3);
                double maxLon =  WebMercatorUtils.getLongitudeFromX(3*256, 3);

                for (Player player : Bukkit.getOnlinePlayers()) {

                    Coords2D coords = new Coords2D(plugin, player.getLocation());

                    int x = (int) (NumberUtils.getInNewRange(minLon, maxLon, 0, 255, coords.getLon()) - 8);
                    int y = (int) (NumberUtils.getInNewRange(maxLat, minLat, 0, 383, coords.getLat()) - 8);

                    if (x >= 0 && x <= 255 && y >= 0 & y <= 383) {
                        InputStream is = HttpRequest.get(new URL("https://mc-heads.net/avatar/" + player.getUniqueId().toString() + "/16")).execute().getInputStream();
                        BufferedImage head = ImageIO.read(is);

                        g.drawImage(head, x, y, null);
                    }


                    Country country = plugin.getCountryManager().getCountryAt(player.getLocation());

                    if (country != null) {
                        List<String> names = namesMap.get(country);
                        names.add(player.getName().replace("_", "\\_"));
                    }

                }

                InputStream is = ImageUtils.getStream(base);

                for (Map.Entry<Country, List<String>> entry : namesMap.entrySet()) {
                    builder.addField(
                            ":flag_" + entry.getKey().getAbbreviation() + ": " + entry.getKey().getDisplayName() + ":",
                            (entry.getValue().isEmpty() ? "N/A" : String.join(", ", entry.getValue())),
                            true
                    );
                }

                builder.setImage("attachment://map.png");

                event.replyEmbeds(builder.build()).setFiles(FileUpload.fromData(is, "map.png"))
                        .addActionRow(
                                plugin.getDiscordHandler().getDeleteButton(event.getUser())
                        )
                        .queue(
                                msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                        );

            } catch (IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error al cargar la imagen.");
            }
        }

    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash("online", "ve los jugadores online y su ubicación.")};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

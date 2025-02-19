package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.Managers.ProjectManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ScoreboardCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public ScoreboardCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "scoreboard",
                "Obtén una lista de los jugadores con el mayor puntaje de un país."
        )
                .setGuildOnly(true)};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("scoreboard")) {

            assert event.getGuild() != null;
            Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

            try {
                List<UUID> top = country.getTopPlayers();

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.GREEN);
                builder.setTitle("Jugadores con mayor puntaje de " + country.getDisplayName());
                builder.setThumbnail(country.getIconURL());

                int counter = 1;
                for (UUID uuid : top) {

                    String place;
                    if (counter == 1) {
                        place = ":first_place: ";
                    } else if (counter == 2) {
                        place = ":second_place: ";
                    } else if (counter == 3) {
                        place = ":third_place: ";
                    } else {
                        place = "#" + counter + " ";
                    }

                    ServerPlayer s = plugin.getPlayerRegistry().get(uuid);
                    ProjectManager manager = s.getProjectManager();

                    builder.addField(
                            place + s.getName(),
                            "**" + manager.getPoints(country) + "** puntos obtenidos\n**" + manager.getFinishedProjects(country) + "** proyectos terminados",
                            false
                    );

                    counter++;

                }

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("scoreboardCommand?user=" + event.getUser().getId());
                for (Country c : plugin.getCountryManager().getAllCountries()) {

                    menuBuilder.addOption(
                            c.getDisplayName(),
                            c.getName(),
                            c.getEmoji()
                    );

                }
                menuBuilder.setDefaultValues(country.getName());

                event.replyEmbeds(builder.build())
                        .addComponents(
                                ActionRow.of(
                                        menuBuilder.build()
                                )
                        ).queue(
                                msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                        );

            } catch (SQLException | IOException e) {
                e.printStackTrace();
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {

        assert event.getSelectMenu().getId() != null;
        if (event.getSelectMenu().getId().startsWith("scoreboardCommand")) {

            Map<String, String> query = StringUtils.getQuery(event.getSelectMenu().getId().split("\\?")[1]);
            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede seleccionar un país.");
                return;
            }

            Country country = plugin.getCountryManager().get(event.getValues().get(0));

            try {
                List<UUID> top = country.getTopPlayers();

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.GREEN);
                builder.setTitle("Jugadores con mayor puntaje de " + country.getDisplayName());
                builder.setThumbnail(country.getIconURL());

                int counter = 1;
                for (UUID uuid : top) {

                    String place;
                    if (counter == 1) {
                        place = ":first_place: ";
                    } else if (counter == 2) {
                        place = ":second_place: ";
                    } else if (counter == 3) {
                        place = ":third_place: ";
                    } else {
                        place = "#" + counter + " ";
                    }

                    ServerPlayer s = plugin.getPlayerRegistry().get(uuid);
                    ProjectManager manager = s.getProjectManager();

                    builder.addField(
                            place + s.getName(),
                            "**" + manager.getPoints(country) + "** puntos obtenidos\n**" + manager.getFinishedProjects(country) + "** proyectos terminados",
                            false
                    );

                    counter++;

                }

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("scoreboardCommand?user=" + event.getUser().getId());
                for (Country c : plugin.getCountryManager().getAllCountries()) {

                    menuBuilder.addOption(
                            c.getDisplayName(),
                            c.getName(),
                            c.getEmoji()
                    );

                }
                menuBuilder.setDefaultValues(country.getName());

                event.editMessageEmbeds(builder.build())
                        .setComponents(
                                ActionRow.of(
                                        menuBuilder.build()
                                )
                        ).queue();

            } catch (SQLException | IOException e) {
                e.printStackTrace();
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }

        }

    }
}
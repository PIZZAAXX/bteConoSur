package pizzaaxx.bteconosur.LegacyConversion;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Discord.SlashCommands.SlashCommandContainer;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Finished.FinishedProject;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Entities.SQLPolygon;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.CoordinatesUtils;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.SatMapHandler;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RegisterFinishedCommand extends ListenerAdapter implements CommandExecutor, SlashCommandContainer {

    private final BTEConoSur plugin;

    public RegisterFinishedCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        Polygonal2DRegion region;
        try {
            Region r = plugin.getWorldEdit().getSelection(p);
            if (!(r instanceof Polygonal2DRegion)) {
                p.sendMessage("Selecciona una región poligonal válida.");
                return true;
            }
            region = (Polygonal2DRegion) r;
        } catch (IncompleteRegionException e) {
            p.sendMessage("Selecciona una región poligonal válida.");
            return true;
        }

        Country c = plugin.getCountryManager().getCountryAt(CoordinatesUtils.blockVector2DtoLocation(plugin, region.getPoints().get(0)));

        if (c == null) {
            p.sendMessage("Estás fuera de un país.");
            return true;
        }

        if (!s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN) || !s.getProjectManager().hasAdminPermission(c)) {
            p.sendMessage("No puedes hacer esto.");
            return true;
        }

        if (args.length < 1) {
            p.sendMessage("Introduce un jugador.");
            return true;
        }

        String player = args[0];

        UUID owner;
        try {
            if (!plugin.getPlayerRegistry().hasPlayedBefore(player)) {
                p.sendMessage("El jugador no ha entrado antes al servidor.");
                return true;
            }
            owner = plugin.getPlayerRegistry().get(player).getUUID();
        } catch (SQLException | IOException e) {
            p.sendMessage("El jugador no ha entrado antes al servidor.");
            return true;
        }


        if (args.length < 2) {
            p.sendMessage("Introduce un tipo de proyecto.");
            return true;
        }

        ProjectType type = c.getProjectType(args[1]);

        if (type == null) {
            p.sendMessage("Tipo de proyecto inválido.");
            return true;
        }

        try {
            this.registerFinishedProject(region.getPoints(), owner, type);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            p.sendMessage("Ha ocurrido un error.");
        }

        return true;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("registerfinished")) {
            OptionMapping idMapping = event.getOption("id");
            assert idMapping != null;
            String id = idMapping.getAsString();

            assert event.getGuild() != null;
            Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Conecta tu cuenta.");
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            if (!s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN) || !s.getProjectManager().hasAdminPermission(country)) {
                DiscordUtils.respondError(event, "No puedes hacer esto.");
                return;
            }

            country.getLogsChannel().retrieveMessageById(id).queue(
                    message -> {

                        /*
                        :clipboard: **pablomp_** ha creado el proyecto `ttyxeq` con dificultad `FACIL` en las coordenadas:> -8287685 568 2845477> -8287679 569 2845563> -8287621 568 2845587> -8287627 568 2845471
                         */

                        String content = message.getContentRaw().replace("\n", "");
                        if (content.contains(" ha creado el proyecto ")) {

                            String name = content.split("\\*\\*")[1];

                            UUID owner;
                            try {
                                if (!plugin.getPlayerRegistry().hasPlayedBefore(name)) {
                                    DiscordUtils.respondError(event, "El jugador no ha entrado antes al servidor.");
                                    return;
                                }
                                owner = plugin.getPlayerRegistry().get(name).getUUID();
                            } catch (SQLException | IOException e) {
                                DiscordUtils.respondError(event, "El jugador no ha entrado antes al servidor.");
                                return;
                            }

                            String difficulty = content.split("`")[3];

                            String[] coordStrings = content.split("> ", 2)[1].split("> ");
                            List<BlockVector2D> vectors = new ArrayList<>();
                            for (String coordString : coordStrings) {
                                String[] coords = coordString.split(" ");
                                vectors.add(new BlockVector2D(Integer.parseInt(coords[0]), Integer.parseInt(coords[2])));
                            }

                            ProjectType type = country.getProjectType(country.getLegacyDifficulties().get((difficulty.equals("FACIL") ? 0 : (difficulty.equals("INTERMEDIO") ? 1 : 2))));

                            try {
                                event.deferReply(true).queue();
                                this.registerFinishedProject(vectors, owner, type);
                                event.getInteraction().getHook().editOriginalEmbeds(DiscordUtils.fastEmbed(Color.GREEN, "Proyecto registrado con éxito.")).queue();
                            } catch (SQLException | IOException e) {
                                DiscordUtils.respondError(event, "Ha ocurrido un error.");
                            }
                        }

                    },
                    throwable -> {
                        DiscordUtils.respondError(event, "No existe un mensaje con esa ID.");
                    }
            );
        }
    }

    public void registerFinishedProject(List<BlockVector2D> vectors, UUID owner, @NotNull ProjectType type) throws SQLException, IOException {

        String id = StringUtils.generateCode(8, plugin.getFinishedProjectsRegistry().getIds(), StringUtils.LOWER_CASE);

        ProtectedPolygonalRegion region = new ProtectedPolygonalRegion(
                "test",
                vectors,
                -100, 8000
        );

        Set<City> cities = plugin.getCityManager().getCitiesAt(region, type.getCountry());

        Date date = new Date(System.currentTimeMillis());

        plugin.getSqlManager().insert(
                "finished_projects",
                new SQLValuesSet(
                        new SQLValue(
                                "finished_date", date
                        ),
                        new SQLValue(
                                "id", id
                        ),
                        new SQLValue("name", id.toUpperCase()),
                        new SQLValue("country", type.getCountry()),
                        new SQLValue("cities", cities),
                        new SQLValue("type", type),
                        new SQLValue("points", type.getPointsOptions().get(0)),
                        new SQLValue("members", new HashSet<>()),
                        new SQLValue("owner", owner),
                        new SQLValue("tag", null),
                        new SQLValue("region_points", vectors),
                        new SQLValue("region", SQLPolygon.getFromVectors(vectors))
                )
        ).execute();
        plugin.getFinishedProjectsRegistry().registerID(id);

        FinishedProject project = plugin.getFinishedProjectsRegistry().get(id);

        plugin.getSqlManager().insert(
                "tour_displays",
                new SQLValuesSet(
                        new SQLValue("date", date),
                        new SQLValue("type", "finished_project"),
                        new SQLValue("id", id),
                        new SQLValue("cities", cities)
                )
        ).execute();

        ServerPlayer s = plugin.getPlayerRegistry().get(owner);
        s.getProjectManager().addFinished(type.getCountry(), type);

        plugin.getTerramapHandler().drawPolygon(CoordinatesUtils.getCoords2D(plugin, vectors), new Color(51, 60, 232), id);

        InputStream is = plugin.getSatMapHandler().getMapStream(
                new SatMapHandler.SatMapPolygon(
                        plugin,
                        region.getPoints(),
                        "3068ff"
                )
        );

        File file = new File(plugin.getDataFolder(), "projects/images/" + id + ".png");
        file.createNewFile();
        Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        {
            List<String> memberNames = new ArrayList<>();
            for (UUID memberUUID : project.getMembers()) {
                memberNames.add(plugin.getPlayerRegistry().get(memberUUID).getName().replace("_", "\\_"));
            }

            ForumChannel channel = project.getCountry().getProjectsForumChannel();
            Set<ForumTag> tags = new HashSet<>();
            tags.add(channel.getAvailableTagsByName("Terminado", true).get(0));
            if (project.getTag() != null) {
                tags.add(channel.getAvailableTagsByName(project.getTag().toString(), true).get(0));
            }
            tags.add(channel.getAvailableTagsByName(project.getType().getDisplayName(), true).get(0));

            project.getCountry().getProjectsForumChannel().createForumPost(
                            "Proyecto " + project.getId().toUpperCase() + (cities.isEmpty() ? "" : " - " + cities.stream().map(City::getDisplayName).collect(Collectors.joining(", "))),
                            MessageCreateData.fromContent(":speech_balloon: **Descripción:** N/A")
                    )
                    .setEmbeds(
                            new EmbedBuilder()
                                    .setColor(project.getType().getColor())
                                    .addField(
                                            ":crown: Líder:",
                                            plugin.getPlayerRegistry().get(project.getOwner()).getName(),
                                            true
                                    )
                                    .addField(
                                            ":busts_in_silhouette: Miembros:",
                                            (memberNames.isEmpty() ? "Sin miembros." : String.join(", ", memberNames)),
                                            true
                                    )
                                    .addField(
                                            ":game_die: Tipo:",
                                            project.getType().getDisplayName() + " (" + project.getPoints() + " puntos)",
                                            true
                                    )
                                    .build()
                    )
                    .setFiles(
                            FileUpload.fromData(
                                    plugin.getSatMapHandler().getMapStream(
                                            new SatMapHandler.SatMapPolygon(
                                                    plugin,
                                                    project.getRegionPoints()
                                            )
                                    ),
                                    "projectMap.png"
                            )
                    ).setTags(
                            tags
                    ).queue(
                            forumPost -> {
                                try {
                                    plugin.getSqlManager().insert(
                                            "posts",
                                            new SQLValuesSet(
                                                    new SQLValue("target_type", "finished_project"),
                                                    new SQLValue("target_id", project.getId()),
                                                    new SQLValue("channel_id", forumPost.getThreadChannel().getId()),
                                                    new SQLValue("members", project.getAllMembers()),
                                                    new SQLValue("country", project.getCountry()),
                                                    new SQLValue("cities", project.getCities()),
                                                    new SQLValue("name", "Proyecto" + project.getId().toUpperCase()),
                                                    new SQLValue("description", "N/A"),
                                                    new SQLValue("message_id", forumPost.getMessage().getId())
                                            )
                                    ).execute();
                                } catch (SQLException e) {
                                    forumPost.getThreadChannel().delete().queue();
                                }
                            }
                    );
        }

        for (City city : cities) {
            city.updateFinishedArea();
        }

    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "registerfinished",
                "Registra un proyecto finalizado anterior al cambio del nuevo sistema."
        ).addOption(
                OptionType.STRING,
                "id",
                "ID del mensaje de creación del proyecto.",
                true
        )};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

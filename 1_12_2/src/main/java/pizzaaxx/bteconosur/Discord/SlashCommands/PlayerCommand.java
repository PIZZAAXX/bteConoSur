package pizzaaxx.bteconosur.Discord.SlashCommands;

import com.sk89q.worldguard.util.net.HttpRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Player.Managers.ChatManager;
import pizzaaxx.bteconosur.Player.Managers.DiscordManager;
import pizzaaxx.bteconosur.Player.Managers.ProjectManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.SQLSelectors.CountrySQLSelector;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.*;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.Utils.DiscordUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression.Order.ASC;
import static pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression.Order.DESC;

public class PlayerCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public PlayerCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {

        if (event.getName().equals("Ver información")) {

            User user = event.getTarget();

            if (!plugin.getLinksRegistry().isLinked(user.getId())) {
                DiscordUtils.respondError(event, "El usuario introducido no tiene una cuenta de Minecraft conectada.");
                return;
            }

            ServerPlayer s  = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(user.getId()));

            this.respondPlayerInfoEmbed(s, event);
        }

        if (event.getName().equals("Ver proyectos")) {

            User user = event.getTarget();

            if (!plugin.getLinksRegistry().isLinked(user.getId())) {
                DiscordUtils.respondError(event, "El usuario introducido no tiene una cuenta de Minecraft conectada.");
                return;
            }

            ServerPlayer s  = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(user.getId()));

            this.respondPlayerProjectsEmbed(s, event, 1);
        }

    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("player")) {

            String subcommandName = event.getSubcommandName();

            if (subcommandName == null) {
                return;
            }

            ServerPlayer s;
            if (subcommandName.equals("name")) {

                OptionMapping nameMapping = event.getOption("nombre");
                assert nameMapping != null;
                String name = nameMapping.getAsString();

                try {
                    s = plugin.getPlayerRegistry().get(name);

                    if (s == null) {
                        DiscordUtils.respondError(event, "El jugador introducido jamás ha entrado al servidor o no existe.");
                        return;
                    }
                } catch (SQLException | IOException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                    return;
                }

            } else {

                OptionMapping userMapping = event.getOption("usuario");
                assert userMapping != null;
                User user = userMapping.getAsUser();

                if (!plugin.getLinksRegistry().isLinked(user.getId())) {
                    DiscordUtils.respondError(event, "El usuario introducido no tiene una cuenta de Minecraft conectada.");
                    return;
                }

                s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(user.getId()));

            }

            this.respondPlayerInfoEmbed(s, event);
        }
    }

    public void respondPlayerInfoEmbed(@NotNull ServerPlayer s, IReplyCallback event) {

        OfflinePlayer p = Bukkit.getOfflinePlayer(s.getUUID());

        try {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setThumbnail("https://mc-heads.net/head/" + s.getUUID());
            builder.setTitle(s.getName());

            builder.addField(
                    "Rango:",
                    s.getBuilderRank().getDiscordPrefix() + StringUtils.capitalize(s.getBuilderRank().toString().toLowerCase()),
                    true
            );

            if (!s.getSecondaryRoles().isEmpty()) {
                List<String> lines = new ArrayList<>();
                s.getSecondaryRoles().forEach(
                        role -> lines.add("• " + role.getDiscordPrefix() + role)
                );

                builder.addField(
                        "Roles:",
                        String.join("\n", lines),
                        true
                );
            } else {
                builder.addBlankField(true);
            }

            DiscordManager discordManager = s.getDiscordManager();

            if (discordManager.isLinked()) {
                builder.addField(
                        "Discord:",
                        "<@" + discordManager.getId() + ">",
                        true
                );
            } else {
                builder.addBlankField(true);
            }

            InputStream is = null;
            if (p.isOnline()) {

                Player player = Bukkit.getPlayer(p.getUniqueId());

                builder.setColor(Color.GREEN);
                builder.setDescription(":green_circle: Online");

                ChatManager chatManager = s.getChatManager();
                Chat chat = chatManager.getCurrentChat();
                builder.addField(
                        "Chat:",
                        chat.getEmoji() + " " + chat.getDisplayName(),
                        true
                );

                Location loc = player.getLocation();
                Coords2D coords = new Coords2D(plugin, loc);

                builder.addField(
                        "Coordenadas:",
                        ":round_pushpin: [" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "](https://www.google.com/maps/@" + coords.getLat() + "," + coords.getLon() + ",19z" + ")",
                        true
                );

                builder.addBlankField(false);

                URL baseURL = new URL("https://www.mapquestapi.com/staticmap/v5/map?key=" + plugin.getSatMapHandler().getKey() + "&size=1280,720&format=png&center=" + coords.getLat() + "," + coords.getLon() + "&zoom=18&type=sat");
                InputStream baseIS = HttpRequest.get(baseURL).execute().getInputStream();

                URL iconURL = new URL("https://crafatar.com/avatars/" + s.getUUID() + "?size=64");
                InputStream iconIS = HttpRequest.get(iconURL).execute().getInputStream();

                BufferedImage image = ImageIO.read(baseIS);
                BufferedImage icon = ImageIO.read(iconIS);

                Graphics2D g = image.createGraphics();
                g.drawImage(icon, 608, 328, null);

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(image, "png", os);
                is = new ByteArrayInputStream(os.toByteArray());

                builder.setImage("attachment://map.png");

            } else {

                builder.setColor(Color.RED);
                builder.setDescription(":red_circle: Offline");

                long lastDisconnected = s.getLastDisconnected() / 1000;

                builder.addField(
                        "Última vez conectado:",
                        ":timer: <t:" + lastDisconnected + ":R>",
                        false
                );

                builder.addBlankField(false);

            }

            ProjectManager projectManager = s.getProjectManager();
            for (Country country : plugin.getCountryManager().getAllCountries()) {
                int finishedProjects = projectManager.getFinishedProjects(country);

                int activeProjects = projectManager.getProjects(new CountrySQLSelector(country)).size();

                if (finishedProjects > 0 || activeProjects > 0) {
                    builder.addField(
                            ":flag_" + country.getAbbreviation() + ": " + country.getDisplayName(),
                            "• `" + projectManager.getPoints(country) + "` puntos obtenidos\n• `" + projectManager.getFinishedProjects(country) + "` proyectos terminados\n• `" + projectManager.getProjects(new CountrySQLSelector(country)).size() + "` proyectos activos",
                            true
                    );
                }
            }

            if (event instanceof SlashCommandInteractionEvent || event instanceof UserContextInteractionEvent) {

                ReplyCallbackAction action = event.replyEmbeds(builder.build())
                        .addComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "playerCommandViewProjects?user=" + event.getUser().getId() + "&uuid=" + s.getUUID().toString(),
                                                "Ver proyectos",
                                                Emoji.fromUnicode("U+1F5FA")
                                        ).withDisabled(projectManager.getAllProjectIDs().isEmpty()),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                )
                        );
                if (is != null) {
                    action.setFiles(
                            FileUpload.fromData(is, "map.png")
                    );
                }

                action.queue(
                        msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                );
            } else if (event instanceof IMessageEditCallback) {

                IMessageEditCallback editCallback = (IMessageEditCallback) event;

                MessageEditCallbackAction action = editCallback.editMessageEmbeds(builder.build())
                        .setComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "playerCommandViewProjects?user=" + event.getUser().getId() + "&uuid=" + s.getUUID().toString(),
                                                "Ver proyectos",
                                                Emoji.fromUnicode("U+1F5FA")
                                        ).withDisabled(projectManager.getAllProjectIDs().isEmpty()),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                )
                        );

                if (is != null) {
                    action.setFiles(
                            FileUpload.fromData(is, "map.png")
                    );
                }

                action.queue(
                        msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                );

            }

        } catch (Exception e) {
            e.printStackTrace();
            DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
        }

    }

    public void respondPlayerProjectsEmbed(ServerPlayer s, IReplyCallback event, int page) {

        try {
            ResultSet countSet = plugin.getSqlManager().select(
                    "projects",
                    new SQLColumnSet("COUNT(id) AS count"),
                    new SQLORConditionSet(
                            new SQLOperatorCondition(
                                    "owner", "=", s.getUUID()
                            ),
                            new SQLJSONArrayCondition(
                                    "members", s.getUUID()
                            )
                    )
            ).retrieve();

            countSet.next();
            int count = countSet.getInt("count");

            if (count == 0) {

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.RED);
                builder.setTitle(s.getName() + " no tiene proyectos.");

                if (event instanceof SlashCommandInteractionEvent || event instanceof UserContextInteractionEvent) {

                    event.replyEmbeds(builder.build())
                            .setFiles()
                            .addComponents(
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.SUCCESS,
                                                    "playerCommandViewInfo?user=" + event.getUser().getId() + "&uuid=" + s.getUUID().toString(),
                                                    "Ver información del jugador",
                                                    Emoji.fromUnicode("U+2139")
                                            ),
                                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                    )
                            ).queue(
                                    msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                            );


                } else if (event instanceof IMessageEditCallback) {

                    IMessageEditCallback editCallback = (IMessageEditCallback) event;

                    editCallback.editMessageEmbeds(builder.build())
                            .setFiles()
                            .setComponents(
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.SUCCESS,
                                                    "playerCommandViewInfo?user=" + event.getUser().getId() + "&uuid=" + s.getUUID().toString(),
                                                    "Ver información del jugador",
                                                    Emoji.fromUnicode("U+2139")
                                            ),
                                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                    )
                            ).queue(
                                    msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                            );
                }

                return;

            }

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Proyectos de " + s.getName());
            builder.setColor(Color.GREEN);

            ResultSet set = plugin.getSqlManager().select(
                    "projects",
                    new SQLColumnSet("id"),
                    new SQLORConditionSet(
                            new SQLOperatorCondition(
                                    "owner", "=", s.getUUID()
                            ),
                            new SQLJSONArrayCondition(
                                    "members", s.getUUID()
                            )
                    ),
                    new SQLOrderSet(
                            new SQLOrderExpression(
                                    "(owner = null)", DESC
                            ),
                            new SQLOrderExpression(
                                    "points", DESC
                            )
                    )
            ).addText(
                    " LIMIT 25 OFFSET " + ((page - 1) * 25)
            ).retrieve();

            while (set.next()) {
                Project project = plugin.getProjectRegistry().get(set.getString("id"));

                List<String> lines = new ArrayList<>();

                if (project.hasCustomName()) {
                    lines.add("> :hash: `" + project.getId() + "`");
                }

                lines.add("> :flag_" + project.getCountry().getAbbreviation() + ": " + project.getCountry().getDisplayName());

                lines.add(
                        "> :game_die: " + project.getType().getDisplayName() + " (" + project.getPoints() + " puntos)"
                );

                lines.add(
                        "> :crown: " + plugin.getPlayerRegistry().get(project.getOwner()).getName()
                );

                lines.add("> :busts_in_silhouette: " + (project.getMembers().isEmpty() ? "Sin miembros." : String.join(", ", plugin.getPlayerRegistry().getNames(project.getMembers()))));

                builder.addField(
                        "Proyecto " + project.getDisplayName(),
                        String.join("\n", lines),
                        true
                );
            }

            if (event instanceof SlashCommandInteractionEvent || event instanceof UserContextInteractionEvent) {

                event.replyEmbeds(builder.build())
                        .setFiles()
                        .addComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.PRIMARY,
                                                "playerCommandProjectsBack?user=" + event.getUser().getId() + "&page=" + page + "&uuid=" + s.getUUID().toString(),
                                                "Pág. anterior",
                                                Emoji.fromUnicode("U+2B05")
                                        ).withDisabled(page <= 1),
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "counter",
                                                page + "/" + (Math.floorDiv(count, 25) + 1)
                                        ).withDisabled(true),
                                        Button.of(
                                                ButtonStyle.PRIMARY,
                                                "playerCommandProjectsNext?user=" + event.getUser().getId() + "&page=" + page + "&uuid=" + s.getUUID().toString(),
                                                "Pág. siguiente",
                                                Emoji.fromUnicode("U+27A1")
                                        ).withDisabled(page >= ((Math.floorDiv(count, 25) + 1)))
                                ),
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "playerCommandViewInfo?user=" + event.getUser().getId() + "&uuid=" + s.getUUID().toString(),
                                                "Ver información del jugador",
                                                Emoji.fromUnicode("U+2139")
                                        ),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                )
                        ).queue(
                                msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                        );


            } else if (event instanceof IMessageEditCallback) {

                IMessageEditCallback editCallback = (IMessageEditCallback) event;

                editCallback.editMessageEmbeds(builder.build())
                        .setFiles()
                        .setComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.PRIMARY,
                                                "playerCommandProjectsBack?user=" + event.getUser().getId() + "&page=" + page + "&uuid=" + s.getUUID().toString(),
                                                "Pág. anterior",
                                                Emoji.fromUnicode("U+2B05")
                                        ).withDisabled(page <= 1),
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "counter",
                                                page + "/" + (Math.floorDiv(count, 25) + 1)
                                        ).withDisabled(true),
                                        Button.of(
                                                ButtonStyle.PRIMARY,
                                                "playerCommandProjectsNext?user=" + event.getUser().getId() + "&page=" + page + "&uuid=" + s.getUUID().toString(),
                                                "Pág. siguiente",
                                                Emoji.fromUnicode("U+27A1")
                                        ).withDisabled(page >= ((Math.floorDiv(count, 25) + 1)))
                                ),
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "playerCommandViewInfo?user=" + event.getUser().getId() + "&uuid=" + s.getUUID().toString(),
                                                "Ver información del jugador",
                                                Emoji.fromUnicode("U+2139")
                                        ),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                )
                        ).queue(
                                msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                        );
            }

        } catch (Exception e) {
            e.printStackTrace();
            DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
        }


    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();

        if (id == null) {
            return;
        }

        if (id.startsWith("playerCommand")) {

            Map<String, String> query = pizzaaxx.bteconosur.Utils.StringUtils.getQuery(id.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quién usó el comando puede usar los botones.");
                return;
            }

            UUID uuid = UUID.fromString(query.get("uuid"));
            ServerPlayer s = plugin.getPlayerRegistry().get(uuid);

            switch (id.split("\\?")[0]) {

                case "playerCommandViewProjects": {

                    this.respondPlayerProjectsEmbed(s, event, 1);

                    break;
                }
                case "playerCommandViewInfo": {

                    this.respondPlayerInfoEmbed(s, event);

                    break;
                }
                case "playerCommandProjectsBack": {

                    int currentPage = Integer.parseInt(query.get("page"));

                    this.respondPlayerProjectsEmbed(s, event, currentPage - 1);

                    break;
                }
                case "playerCommandProjectsNext": {
                    int currentPage = Integer.parseInt(query.get("page"));

                    this.respondPlayerProjectsEmbed(s, event, currentPage + 1);

                    break;
                }

            }

        }
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "player",
                "Obtén información sobre un jugador."
        ).setNameLocalization(
                DiscordLocale.SPANISH,
                "jugador"
        ).addSubcommands(
                new SubcommandData(
                        "name",
                        "Encuentra un jugador usando su nombre de Minecraft."
                ).setNameLocalization(
                        DiscordLocale.SPANISH,
                        "nombre"
                ).addOption(
                        OptionType.STRING,
                        "nombre",
                        "El nombre del jugador.",
                        true,
                        true
                ),
                new SubcommandData(
                        "user",
                        "Encuentra un jugador usando su usuario de Discord."
                ).setNameLocalization(
                        DiscordLocale.SPANISH,
                        "usuario"
                ).addOption(
                        OptionType.USER,
                        "usuario",
                        "El usuario del jugador.",
                        true
                )
        )};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("player")) {
            String subcommand = event.getSubcommandName();
            assert subcommand != null;
            if (subcommand.equals("name")) {
                String value = event.getFocusedOption().getValue();

                if (!value.matches("[A-Za-z\\d_]{1,100}")) {
                    event.replyChoiceStrings().queue();
                    return;
                }

                try {
                    ResultSet set = plugin.getSqlManager().select(
                            "players",
                            new SQLColumnSet(
                                    "name"
                            ),
                            new SQLANDConditionSet(
                                    new SQLLikeCondition("LOWER(name)", true, value.toLowerCase() + "%")
                            ),
                            new SQLOrderSet(
                                    new SQLOrderExpression(
                                            "name", ASC
                                    )
                            )
                    ).addText(" LIMIT 25").retrieve();

                    List<String> result = new ArrayList<>();
                    while (set.next()) {
                        result.add(set.getString("name"));
                    }
                    event.replyChoiceStrings(result).queue();

                } catch (SQLException e) {
                    event.replyChoiceStrings().queue();
                }

            }
        }
    }
}

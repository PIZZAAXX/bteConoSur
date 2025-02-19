package pizzaaxx.bteconosur.Discord.SlashCommands;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Discord.Showcase.ShowcaseContainer;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.SQLSelectors.CountrySQLSelector;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.*;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.Pair;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression.Order.ASC;

public class ImagesCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;
    private final Map<String, List<String>> queryCache = new HashMap<>();

    public ImagesCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("images")) {

            String subcommandName = event.getSubcommandName();
            assert subcommandName != null;
            switch (subcommandName) {
                case "city":
                    OptionMapping cityMapping = event.getOption("city");
                    assert cityMapping != null;
                    String cityName = cityMapping.getAsString();

                    try {
                        this.respondCity(event, cityName);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        DiscordUtils.respondError(event, "Ha ocurrido un error.");
                    }
                case "address":
                    OptionMapping addressMapping = event.getOption("address");
                    assert addressMapping != null;
                    String address = addressMapping.getAsString();

                    try {
                        this.respondAddress(event, address);
                    } catch (SQLException | IOException e) {
                        DiscordUtils.respondError(event, "Ha ocurrido un error.");
                    }
            }
        }
    }

    public void respondCity(IReplyCallback event, @NotNull String input) throws SQLException {

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
        if (names.isEmpty()) {
            DiscordUtils.respondError(event, "No se ha encontrado ninguna ciudad con ese nombre.");
        } else if (names.size() == 1) {
            this.respondCityImage(event, names.get(0), 0);
        } else {
            if (zeros.isEmpty()) {
                this.respondCitySelector(event, names);
            } else if (zeros.size() == 1) {
                this.respondCityImage(event, zeros.get(0), 0);
            } else {
                this.respondCitySelector(event, zeros);
            }
        }
    }

    public void respondCitySelector(@NotNull IReplyCallback event, @NotNull List<String> options) {

        StringSelectMenu.Builder menu = StringSelectMenu.create("imageCommandCitySelector?user=" + event.getUser().getId());
        menu.setPlaceholder("Se han encontrado " + options.size() + " opciones:");

        for (String name : options) {
            City city = plugin.getCityManager().get(name);

            menu.addOption(
                    city.getDisplayName(),
                    city.getName(),
                    city.getCountry().getEmoji()
            );
        }

        if (event instanceof SlashCommandInteractionEvent) {
            event.replyComponents(
                    ActionRow.of(
                            menu.build()
                    )
            ).queue();
        } else if (event instanceof IMessageEditCallback) {
            IMessageEditCallback editCallback = (IMessageEditCallback) event;
            editCallback.editComponents(
                    ActionRow.of(
                            menu.build()
                    )
            ).setReplace(true).queue();
        }
    }

    public void respondCityImage(IReplyCallback event, String cityName, int index) throws SQLException {

        if (event instanceof SlashCommandInteractionEvent) {
            event.deferReply().queue();
        } else if (event instanceof IMessageEditCallback) {
            ((IMessageEditCallback) event).deferEdit().queue();
        }

        if (!plugin.getCityManager().exists(cityName)) {
            DiscordUtils.respondError(event, "La ciudad no existe.");
            return;
        }

        City city = plugin.getCityManager().get(cityName);

        ResultSet set = plugin.getSqlManager().select(
                "showcases",
                new SQLColumnSet(
                        "id"
                ),
                new SQLANDConditionSet(
                        new SQLJSONArrayCondition("cities", cityName)
                )
        ).retrieve();

        List<String> showcaseIDs = new ArrayList<>();
        while (set.next()) {
            showcaseIDs.add(set.getString("id"));
        }

        if (showcaseIDs.isEmpty()) {
            event.getHook().editOriginalEmbeds(
                    DiscordUtils.fastEmbed(
                            Color.RED,
                            "No hay imágenes en esta ciudad."
                    )
            ).setActionRow(
                    Button.of(
                            ButtonStyle.PRIMARY,
                            "imagesCommandCitySearchNew?user=" + event.getUser().getId(),
                            "Buscar otra dirección",
                            Emoji.fromUnicode("U+1F50E")
                    )
            ).setReplace(true).queue();
            return;
        }

        int finalIndex = index < 0 ? showcaseIDs.size() - 1 : (index >= showcaseIDs.size() ? 0 : index);

        String showcaseId = showcaseIDs.get(finalIndex);

        ResultSet showcaseSet = plugin.getSqlManager().select(
                "showcases",
                new SQLColumnSet("*"),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", showcaseId
                        )
                )
        ).retrieve();

        showcaseSet.next();

        Country country = plugin.getCountryManager().get(showcaseSet.getString("country"));

        try {
            country.getShowcaseChannel().retrieveMessageById(showcaseSet.getString("message_id")).queue(
                    message -> {
                        try {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setColor(Color.GREEN);
                            DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                            builder.setFooter("Fecha: " + format.format(new Date(message.getTimeCreated().toInstant().toEpochMilli())));
                            builder.setImage(showcaseSet.getString("url"));
                            builder.setThumbnail(message.getAuthor().getAvatarUrl());
                            builder.addField(":camera_with_flash: Autor:", "<@" + message.getAuthor().getId() + ">", true);
                            builder.addField(
                                    ":heart: Reacciones:",
                                    Integer.toString(message.getReactions().stream().mapToInt(MessageReaction::getCount).sum()),
                                    true
                            );
                            builder.addField(":speech_balloon: Descripción:", message.getContentRaw().isEmpty() ? "N/A" : message.getContentRaw(), false);

                            builder.setAuthor(
                                    "Imágenes de " + city.getDisplayName()
                            );

                            String targetID = showcaseSet.getString("target_id");
                            switch (showcaseSet.getString("target_type")) {
                                case "project":
                                    builder.setTitle(
                                            "Proyecto " + plugin.getProjectRegistry().get(targetID).getDisplayName()
                                    );
                                    break;
                                case "finished":
                                    builder.setTitle(
                                            "Proyecto " + plugin.getFinishedProjectsRegistry().get(targetID).getDisplayName()
                                    );
                                    break;
                                case "event":
                                    builder.setTitle(
                                            "Evento " + plugin.getBuildEventsRegistry().get(targetID).getName()
                                    );
                                    break;
                            }

                            event.getHook().editOriginalEmbeds(
                                    builder.build()
                            ).setReplace(true).setComponents(
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.PRIMARY,
                                                    "imagesCommandCitySearchNew?user=" + event.getUser().getId(),
                                                    "Buscar otra ciudad",
                                                    Emoji.fromUnicode("U+1F50E")
                                            ),
                                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                    ),
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.SUCCESS,
                                                    "imagesCommandCitySkip?user=" + event.getUser().getId() + "&page=" + (finalIndex - 1) + "&city=" + cityName,
                                                    "Anterior",
                                                    Emoji.fromUnicode("U+2B05")
                                            ),
                                            Button.of(
                                                    ButtonStyle.SECONDARY,
                                                    "counter",
                                                    (finalIndex + 1) + "/" + showcaseIDs.size()
                                            ),
                                            Button.of(
                                                    ButtonStyle.SUCCESS,
                                                    "imagesCommandCitySkip?user=" + event.getUser().getId() + "&page=" + (finalIndex + 1) + "&city=" + cityName,
                                                    "Siguiente",
                                                    Emoji.fromUnicode("U+27A1")
                                            )
                                    )
                            ).queue();

                        } catch (SQLException e) {
                            event.getHook().editOriginalEmbeds(
                                    DiscordUtils.fastEmbed(
                                            Color.RED,
                                            "Ha ocurrido un error."
                                    )
                            ).setActionRow(
                                    Button.of(
                                            ButtonStyle.PRIMARY,
                                            "imagesCommandCitySearchNew?user=" + event.getUser().getId(),
                                            "Buscar otra dirección",
                                            Emoji.fromUnicode("U+1F50E")
                                    )
                            ).setReplace(true).queue();
                        }
                    }
            );
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                plugin.getSqlManager().delete(
                        "showcases",
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "message_id", "=", showcaseSet.getString("message_id")
                                )
                        )
                ).execute();
            }
            event.getHook().editOriginalEmbeds(
                    DiscordUtils.fastEmbed(
                            Color.RED,
                            "Ha ocurrido un error."
                    )
            ).setActionRow(
                    Button.of(
                            ButtonStyle.PRIMARY,
                            "imagesCommandCitySearchNew?user=" + event.getUser().getId(),
                            "Buscar otra dirección",
                            Emoji.fromUnicode("U+1F50E")
                    )
            ).setReplace(true).queue();
        }

    }

    public void respondAddress(@NotNull IReplyCallback event, @NotNull String address) throws IOException, SQLException {
        if (event instanceof SlashCommandInteractionEvent) {
            event.deferReply().queue();
        } else if (event instanceof IMessageEditCallback) {
            ((IMessageEditCallback) event).deferEdit().queue();
        }

        String dir = String.join("+", address.split(" "));

        URL url = new URL("https://nominatim.openstreetmap.org/search?q=" + dir + "&format=json");

        JsonNode responseNode = plugin.getJSONMapper().readTree(url);

        List<Pair<Pair<String, String>, Pair<Country, City>>> applicableNodes = new ArrayList<>();

        for (JsonNode optionNode : responseNode) {
            Location loc = new Coords2D(plugin, optionNode.path("lat").asDouble(), optionNode.path("lon").asDouble()).toHighestLocation();
            Country country = plugin.getCountryManager().getCountryAt(loc);

            if (country == null) {
                continue;
            }

            City city = plugin.getCityManager().getCityAt(loc);

            applicableNodes.add(
                    new Pair<>(
                            new Pair<>(
                                    optionNode.path("name").asText(),
                                    optionNode.path("osm_type").asText().substring(0,1).toUpperCase() + optionNode.path("osm_id").asText()
                            ),
                            new Pair<>(
                                    country,
                                    city
                            )
                    )
            );
        }
        if (applicableNodes.isEmpty()) {
            event.getHook().editOriginalEmbeds(DiscordUtils.fastEmbed(
                    Color.RED,
                    "No se han encontrado resultados dentro del Cono Sur."
            )).queue();
        } else if (applicableNodes.size() == 1) {
            respondAddressImage(event, applicableNodes.get(0).getKey().getValue(), 0);
        } else {
            respondAddressSelector(event, applicableNodes);
        }
    }

    public void respondAddressSelector(@NotNull IReplyCallback event, @NotNull List<Pair<Pair<String, String>, Pair<Country, City>>> options) {

        StringSelectMenu.Builder menu = StringSelectMenu.create("imagesCommandAddressSelector?user=" + event.getUser().getId());

        for (Pair<Pair<String, String>, Pair<Country, City>> option : options) {
            String name = option.getKey().getKey();
            String osmID = option.getKey().getValue();
            Country country = option.getValue().getKey();
            City city = option.getValue().getValue();

            if (city != null) {
                menu.addOption(
                        name,
                        osmID,
                        city.getDisplayName(),
                        country.getEmoji()
                );
            } else {
                menu.addOption(
                        name,
                        osmID,
                        country.getEmoji()
                );
            }
        }

        menu.setPlaceholder("Se han encontrado " + menu.getOptions().size() + " opciones:");

        event.getHook().editOriginalComponents(
                ActionRow.of(
                        menu.build()
                )
        ).setReplace(true).queue();
    }

    public void respondAddressImage(IReplyCallback event, String osmID, int index) throws IOException, SQLException {

        if (event instanceof SlashCommandInteractionEvent) {
            event.deferReply().queue();
        } else if (event instanceof IMessageEditCallback) {
            ((IMessageEditCallback) event).deferEdit().queue();
        }

        URL url = new URL("https://nominatim.openstreetmap.org/details.php?osmtype=" + osmID.charAt(0) + "&osmid=" + osmID.substring(1) + "&format=json");

        JsonNode responseNode = plugin.getJSONMapper().readTree(url);

        if (!queryCache.containsKey(osmID)) {


            JsonNode coordinatesNode = responseNode.path("centroid").path("coordinates");
            double lon = coordinatesNode.get(0).asDouble();
            double lat = coordinatesNode.get(1).asDouble();
            Coords2D coords = new Coords2D(plugin, lat, lon);


            Location loc = new Coords2D(plugin, lat, lon).toHighestLocation();
            List<String> projectIDs = plugin.getProjectRegistry().getProjectsAt(loc);

            List<String> finishedProjectIDs = new ArrayList<>();

            ResultSet finishedProjectsSet = plugin.getSqlManager().select(
                    "finished_projects",
                    new SQLColumnSet("id"),
                    new SQLANDConditionSet(
                            new SQLPointInPolygonCondition(
                                    coords.getX(),
                                    coords.getZ(),
                                    "region"
                            )
                    )
            ).retrieve();

            while (finishedProjectsSet.next()) {
                finishedProjectIDs.add(finishedProjectsSet.getString("id"));
            }

            List<String> eventIDs = new ArrayList<>();

            ResultSet eventsSet = plugin.getSqlManager().select(
                    "build_events",
                    new SQLColumnSet("id"),
                    new SQLANDConditionSet(
                            new SQLPointInPolygonCondition(
                                    coords.getX(),
                                    coords.getZ(),
                                    "region"
                            )
                    )
            ).retrieve();

            while (eventsSet.next()) {
                eventIDs.add(eventsSet.getString("id"));
            }

            ResultSet showcasesSet = plugin.getSqlManager().select(
                    "showcases",
                    new SQLColumnSet("id"),
                    new SQLORConditionSet(
                            new SQLANDConditionSet(
                                    new SQLOperatorCondition("target_type", "=", "project"),
                                    new SQLContainedCondition<>(
                                            "target_id",
                                            projectIDs,
                                            true
                                    )
                            ),
                            new SQLANDConditionSet(
                                    new SQLOperatorCondition("target_type", "=", "finished"),
                                    new SQLContainedCondition<>(
                                            "target_id",
                                            finishedProjectIDs,
                                            true
                                    )
                            ),
                            new SQLANDConditionSet(
                                    new SQLOperatorCondition("target_type", "=", "events"),
                                    new SQLContainedCondition<>(
                                            "target_id",
                                            eventIDs,
                                            true
                                    )
                            )
                    )
            ).retrieve();

            List<String> resultIDs = new ArrayList<>();
            while (showcasesSet.next()) {
                resultIDs.add(showcasesSet.getString("id"));
            }
            queryCache.put(osmID, resultIDs);
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    queryCache.remove(osmID);
                }
            };
            runnable.runTaskLaterAsynchronously(plugin, 12000);
        }

        if (queryCache.get(osmID).isEmpty()) {

            event.getHook().editOriginalEmbeds(
                    DiscordUtils.fastEmbed(
                            Color.RED,
                            "No se han encontrado imágenes de este lugar."
                    )
            ).setActionRow(
                    Button.of(
                            ButtonStyle.PRIMARY,
                            "imagesCommandAddressSearchNew?user=" + event.getUser().getId(),
                            "Buscar otra dirección",
                            Emoji.fromUnicode("U+1F50E")
                    )
            ).setReplace(true).queue();

            return;
        }


        List<String> showcaseIDs = queryCache.get(osmID);
        int finalIndex = index < 0 ? showcaseIDs.size() - 1 : (index >= showcaseIDs.size() ? 0 : index);
        String showcaseId = showcaseIDs.get(finalIndex);

        ResultSet showcaseSet = plugin.getSqlManager().select(
                "showcases",
                new SQLColumnSet("*"),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", showcaseId
                        )
                )
        ).retrieve();

        showcaseSet.next();

        Country country = plugin.getCountryManager().get(showcaseSet.getString("country"));

        try {
            country.getShowcaseChannel().retrieveMessageById(showcaseSet.getString("message_id")).queue(
                    message -> {
                        try {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setColor(Color.GREEN);
                            DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                            builder.setFooter("Fecha: " + format.format(new Date(message.getTimeCreated().toInstant().toEpochMilli())));
                            builder.setImage(showcaseSet.getString("url"));
                            builder.setThumbnail(message.getAuthor().getAvatarUrl());
                            builder.addField(":camera_with_flash: Autor:", "<@" + message.getAuthor().getId() + ">", true);
                            builder.addField(
                                    ":heart: Reacciones:",
                                    Integer.toString(message.getReactions().stream().mapToInt(MessageReaction::getCount).sum()),
                                    true
                            );
                            builder.addField(":speech_balloon: Descripción:", message.getContentRaw().isEmpty() ? "N/A" : message.getContentRaw(), false);

                            String locationName = responseNode.path("localname").asText();

                            builder.setAuthor(
                                    "Imágenes de " + locationName
                            );

                            String targetID = showcaseSet.getString("target_id");
                            switch (showcaseSet.getString("target_type")) {
                                case "project":
                                    builder.setTitle(
                                            "Proyecto " + plugin.getProjectRegistry().get(targetID).getDisplayName()
                                    );
                                    break;
                                case "finished":
                                    builder.setTitle(
                                            "Proyecto " + plugin.getFinishedProjectsRegistry().get(targetID).getDisplayName()
                                    );
                                    break;
                                case "event":
                                    builder.setTitle(
                                            "Evento " + plugin.getBuildEventsRegistry().get(targetID).getName()
                                    );
                                    break;
                            }

                            event.getHook().editOriginalEmbeds(
                                    builder.build()
                            ).setReplace(true).setComponents(
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.PRIMARY,
                                                    "imagesCommandAddressSearchNew?user=" + event.getUser().getId(),
                                                    "Buscar otra dirección",
                                                    Emoji.fromUnicode("U+1F50E")
                                            ),
                                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                    ),
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.SUCCESS,
                                                    "imagesCommandAddressSkip?user=" + event.getUser().getId() + "&page=" + (finalIndex - 1) + "&id=" + osmID,
                                                    "Anterior",
                                                    Emoji.fromUnicode("U+2B05")
                                            ),
                                            Button.of(
                                                    ButtonStyle.SECONDARY,
                                                    "counter",
                                                    (finalIndex + 1) + "/" + queryCache.get(osmID).size()
                                            ),
                                            Button.of(
                                                    ButtonStyle.SUCCESS,
                                                    "imagesCommandAddressSkip?user=" + event.getUser().getId() + "&page=" + (finalIndex + 1) + "&id=" + osmID,
                                                    "Siguiente",
                                                    Emoji.fromUnicode("U+27A1")
                                            )
                                    )
                            ).queue();

                        } catch (SQLException e) {
                            event.getHook().editOriginalEmbeds(
                                    DiscordUtils.fastEmbed(
                                            Color.RED,
                                            "Ha ocurrido un error."
                                    )
                            ).setActionRow(
                                    Button.of(
                                            ButtonStyle.PRIMARY,
                                            "imagesCommandAddressSearchNew?user=" + event.getUser().getId(),
                                            "Buscar otra dirección",
                                            Emoji.fromUnicode("U+1F50E")
                                    )
                            ).setReplace(true).queue();
                        }
                    }
            );
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                plugin.getSqlManager().delete(
                        "showcases",
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "message_id", "=", showcaseSet.getString("message_id")
                                )
                        )
                ).execute();
            }
            event.getHook().editOriginalEmbeds(
                    DiscordUtils.fastEmbed(
                            Color.RED,
                            "Ha ocurrido un error."
                    )
            ).setActionRow(
                    Button.of(
                            ButtonStyle.PRIMARY,
                            "imagesCommandAddressSearchNew?user=" + event.getUser().getId(),
                            "Buscar otra dirección",
                            Emoji.fromUnicode("U+1F50E")
                    )
            ).setReplace(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {

        String selectID = event.getSelectMenu().getId();
        assert selectID != null;
        if (selectID.startsWith("imagesCommandAddressSelector")) {
            Map<String, String> query = StringUtils.getQuery(selectID.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los menú.");
                return;
            }

            String address = event.getSelectedOptions().get(0).getValue();

            try {
                this.respondAddressImage(event, address, 0);
            } catch (SQLException | IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        } else if (selectID.startsWith("imagesCommandCitySelector")) {
            Map<String, String> query = StringUtils.getQuery(selectID.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los menú.");
                return;
            }

            String cityName = event.getSelectedOptions().get(0).getValue();

            try {
                this.respondAddressImage(event, cityName, 0);
            } catch (SQLException | IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        } else if (selectID.startsWith("showcaseSelect")) {

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Tu cuenta no está conectada.");
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            Map<String, String> query = StringUtils.getQuery(selectID.split("\\?")[1]);
            String messageID = query.get("id");
            String type = query.get("type");
            Country country = plugin.getCountryManager().get(query.get("country"));

            if (country == null) {
                return;
            }

            String targetID = event.getSelectedOptions().get(0).getValue();

            ShowcaseContainer container = ShowcaseContainer.getFromData(plugin, targetID, type);

            if (!container.isMember(s.getUUID())) {
                DiscordUtils.respondError(event, "Ya no eres miembro de este proyecto o evento.");
                return;
            }

            country.getShowcaseChannel().retrieveMessageById(messageID).queue(
                    message -> {

                        boolean isEdit;
                        try {
                            ResultSet set = plugin.getSqlManager().select(
                                    "showcases",
                                    new SQLColumnSet("COUNT(id) AS count"),
                                    new SQLANDConditionSet(
                                            new SQLOperatorCondition("message_id", "=", messageID)
                                    )
                            ).retrieve();

                            set.next();
                            isEdit = set.getInt("count") > 0;
                        } catch (SQLException e) {
                            DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                            return;
                        }

                        int counter = 0;
                        for (Message.Attachment attachment : message.getAttachments()) {
                            if (attachment.isImage()) {
                                try {
                                    if (isEdit) {
                                        plugin.getSqlManager().update(
                                                "showcases",
                                                new SQLValuesSet(
                                                        new SQLValue("target_type", type),
                                                        new SQLValue("target_id", targetID),
                                                        new SQLValue("cities", container.getCities())
                                                ),
                                                new SQLANDConditionSet(
                                                        new SQLOperatorCondition("message_id", "=", messageID),
                                                        new SQLOperatorCondition("url", "=", attachment.getUrl())
                                                )
                                        ).execute();
                                    } else {
                                        plugin.getSqlManager().insert(
                                                "showcases",
                                                new SQLValuesSet(
                                                        new SQLValue("message_id", messageID),
                                                        new SQLValue("target_type", type),
                                                        new SQLValue("target_id", targetID),
                                                        new SQLValue("cities", container.getCities()),
                                                        new SQLValue("country", country),
                                                        new SQLValue("url", attachment.getUrl())
                                                )
                                        ).execute();
                                    }
                                } catch (SQLException ignored) {
                                    ignored.printStackTrace();
                                }
                                counter++;
                            }
                        }
                        if (counter == 0) {
                            DiscordUtils.respondError(event, "El mensaje ya no tiene imágenes adjuntas.");
                        } else {
                            DiscordUtils.respondSuccess(event, "Selección guardada correctamente.");
                            event.getMessage().delete().queue();
                        }
                    },
                    throwable -> DiscordUtils.respondError(event, "No se ha podido encontrar el mensaje.")
            );
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        String buttonID = event.getButton().getId();
        assert buttonID != null;
        if (buttonID.startsWith("imagesCommandAddressSkip")) {

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            try {
                this.respondAddressImage(event, query.get("id"), Integer.parseInt(query.get("page")));
            } catch (SQLException | IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }

        } else if (buttonID.startsWith("imagesCommandAddressSearchNew")) {

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            event.replyModal(
                    Modal.create("imagesCommandAddressSearch", "Buscar nueva dirección")
                            .addActionRow(
                                    TextInput.create("address", "Dirección", TextInputStyle.SHORT).build()
                            )
                            .build()
            ).queue();
        } else if (buttonID.startsWith("imagesCommandCitySkip")) {

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            try {
                this.respondCityImage(event, query.get("city"), Integer.parseInt(query.get("page")));
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }

        } else if (buttonID.startsWith("imagesCommandCitySearchNew")) {

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            event.replyModal(
                    Modal.create("imagesCommandCitySearch", "Buscar nueva ciudad")
                            .addActionRow(
                                    TextInput.create("city", "Ciudad", TextInputStyle.SHORT).build()
                            )
                            .build()
            ).queue();
        } else if (buttonID.startsWith("showcaseReloadLink")) {
            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Tu cuenta no está conectada aún.");
                return;
            }

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);
            String messageID = query.get("id");
            Country country = plugin.getCountryManager().get(query.get("country"));

            if (country == null) {
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));


            boolean projects, finishedProjects, events;
            try {
                projects = !s.getProjectManager().getProjects(new CountrySQLSelector(country)).isEmpty();
                finishedProjects = s.getProjectManager().getFinishedProjects(country) > 0;

                ResultSet set = plugin.getSqlManager().select(
                        "build_events",
                        new SQLColumnSet("COUNT(id) as count"),
                        new SQLANDConditionSet(
                                new SQLJSONArrayCondition("members", s.getUUID())
                        )
                ).retrieve();

                set.next();
                events = set.getInt("count") > 0;

            } catch (SQLException ignored) {return;}

            event.editMessageEmbeds(
                            new EmbedBuilder()
                                    .setColor(Color.GREEN)
                                    .setDescription(
                                            "Has subido imágenes a <#" + event.getChannel().getId() + ">. Por favor, selecciona a que proyecto o evento pertenecen."
                                    ).build()
                    )
                    .setActionRow(
                            Button.of(
                                    ButtonStyle.SUCCESS,
                                    "showcaseSelectProjects?id=" + messageID + "&country=" + country.getName(),
                                    "Proyectos",
                                    Emoji.fromUnicode("U+2692")
                            ).withDisabled(!projects),
                            Button.of(
                                    ButtonStyle.SUCCESS,
                                    "showcaseSelectFinished?id=" + messageID + "&country=" + country.getName(),
                                    "P. terminados",
                                    Emoji.fromUnicode("U+2705")
                            ).withDisabled(!finishedProjects),
                            Button.of(
                                    ButtonStyle.SUCCESS,
                                    "showcaseSelectEvents?id=" + messageID + "&country=" + country.getName(),
                                    "Eventos",
                                    Emoji.fromUnicode("U+1F4C6")
                            ).withDisabled(!events)
                    ).queue();
        } else if (buttonID.startsWith("showcaseSelectProjects") || buttonID.startsWith("showcaseSelectFinished") || buttonID.startsWith("showcaseSelectEvents")) {

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Tu cuenta no está conectada.");
                return;
            }

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);
            String messageID = query.get("id");
            Country country = plugin.getCountryManager().get(query.get("country"));
            String type = (buttonID.startsWith("showcaseSelectProjects") ? "project" : (buttonID.startsWith("showcaseSelectFinished") ? "finished" : "event"));

            if (country == null) {
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            StringSelectMenu.Builder menu = StringSelectMenu.create("showcaseSelect?type=" + type + "&id=" + messageID + "&country=" + country.getName());

            List<String> ids;
            try {
                switch (type) {
                    case "project": {
                        ids = new ArrayList<>(s.getProjectManager().getProjects(new CountrySQLSelector(country)));
                        Collections.sort(ids);

                        menu.setPlaceholder("Tus proyectos en " + country.getDisplayName());
                        break;
                    }
                    case "finished": {
                        ResultSet set = plugin.getSqlManager().select(
                                "finished_projects",
                                new SQLColumnSet("id"),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition("country", "=", country.getName()),
                                        new SQLJSONArrayCondition("members", s.getUUID())
                                )
                        ).retrieve();

                        ids = new ArrayList<>();
                        while (set.next()) {
                            ids.add(set.getString("id"));
                        }

                        break;
                    }
                    default: {
                        ResultSet set = plugin.getSqlManager().select(
                                "build_events",
                                new SQLColumnSet("id"),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition("country", "=", country.getName()),
                                        new SQLJSONArrayCondition("members", s.getUUID())
                                )
                        ).retrieve();


                        ids = new ArrayList<>();
                        while (set.next()) {
                            ids.add(set.getString("id"));
                        }

                        break;
                    }
                }
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                return;
            }

            if (ids.isEmpty()) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }

            for (int i = 0; i < Math.min(25, ids.size()); i++) {
                String id = ids.get(i);
                ShowcaseContainer container = ShowcaseContainer.getFromData(plugin, id, type);

                if (container.getOptionDescription() == null) {
                    menu.addOption(
                            container.getOptionName(),
                            id
                    );
                } else {
                    menu.addOption(
                            container.getOptionName(),
                            id,
                            container.getOptionDescription()
                    );
                }
            }

            if (ids.size() > 25) {
                event.editComponents(
                        event.getMessage().getComponents().get(0),
                        ActionRow.of(menu.build()),
                        ActionRow.of(
                                Button.of(
                                        ButtonStyle.SECONDARY,
                                        "showcaseSkipSelector?type" + type + "&id=" + messageID + "&country=" + country.getName() + "&page=-1",
                                        "Anterior",
                                        Emoji.fromUnicode("U+2B05")
                                ).withDisabled(true),
                                Button.of(
                                        ButtonStyle.SECONDARY,
                                        "counter",
                                         "1/" + (int) Math.ceil((double) ids.size() /25)
                                ).withDisabled(true),
                                Button.of(
                                        ButtonStyle.SECONDARY,
                                        "showcaseSkipSelector?type=" + type + "&id=" + messageID + "&country=" + country.getName() + "&page=1",
                                        "Siguiente",
                                        Emoji.fromUnicode("U+27A1")
                                )
                        )
                ).queue();
            } else {
                event.editComponents(
                        event.getMessage().getComponents().get(0),
                        ActionRow.of(menu.build())
                ).queue();
            }
        } else if (buttonID.startsWith("showcaseSkipSelector")) {

            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);
            String messageID = query.get("id");
            Country country = plugin.getCountryManager().get(query.get("country"));
            String type = query.get("type");
            int page = Integer.parseInt(query.get("page"));

            if (country == null) {
                return;
            }

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Tu cuenta no está conectada.");
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            StringSelectMenu.Builder menu = StringSelectMenu.create("showcaseSelect?type=" + type + "&id=" + messageID + "&country=" + country.getName());

            List<String> ids;
            try {
                switch (type) {
                    case "project": {
                        ids = new ArrayList<>(s.getProjectManager().getProjects(new CountrySQLSelector(country)));
                        Collections.sort(ids);

                        menu.setPlaceholder("Tus proyectos en " + country.getDisplayName());
                        break;
                    }
                    case "finished": {
                        ResultSet set = plugin.getSqlManager().select(
                                "finished_projects",
                                new SQLColumnSet("id"),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition("country", "=", country.getName()),
                                        new SQLJSONArrayCondition("members", s.getUUID())
                                )
                        ).retrieve();

                        ids = new ArrayList<>();
                        while (set.next()) {
                            ids.add(set.getString("id"));
                        }
                        menu.setPlaceholder("Tus proyectos terminados en " + country.getDisplayName());


                        break;
                    }
                    default: {
                        ResultSet set = plugin.getSqlManager().select(
                                "build_events",
                                new SQLColumnSet("id"),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition("country", "=", country.getName()),
                                        new SQLJSONArrayCondition("members", s.getUUID())
                                )
                        ).retrieve();


                        ids = new ArrayList<>();
                        while (set.next()) {
                            ids.add(set.getString("id"));
                        }
                        menu.setPlaceholder("Tus eventos en " + country.getDisplayName());

                        break;
                    }
                }
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                return;
            }

            page = (page < 0 ? ids.size() - 1 : (page >= ids.size() ? 0 : page));
            for (int i = 25 * page; i < 25 * (page + 1); i++) {
                String id = ids.get(i);
                ShowcaseContainer container = ShowcaseContainer.getFromData(plugin, id, type);

                if (container.getOptionDescription() == null) {
                    menu.addOption(
                            container.getOptionName(),
                            id
                    );
                } else {
                    menu.addOption(
                            container.getOptionName(),
                            id,
                            container.getOptionDescription()
                    );
                }
            }

            event.editComponents(
                    event.getMessage().getComponents().get(0),
                    ActionRow.of(menu.build()),
                    ActionRow.of(
                            Button.of(
                                    ButtonStyle.SECONDARY,
                                    "showcaseSkipSelector?type" + type + "&id=" + messageID + "&country=" + country.getName() + "&page=" + (page - 1),
                                    "Anterior",
                                    Emoji.fromUnicode("U+2B05")
                            ).withDisabled(page == 0),
                            Button.of(
                                    ButtonStyle.SECONDARY,
                                    "counter",
                                    (page + 1) + "/" + (int) Math.ceil((double) ids.size() /25)
                            ).withDisabled(true),
                            Button.of(
                                    ButtonStyle.SECONDARY,
                                    "showcaseSkipSelector?type=" + type + "&id=" + messageID + "&country=" + country.getName() + "&page=" + (page + 1),
                                    "Siguiente",
                                    Emoji.fromUnicode("U+27A1")
                            ).withDisabled(page + 1 == ids.size())
                    )
            ).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {

        String modalID = event.getModalId();
        if (modalID.startsWith("imagesCommandAddressSearch")) {
            ModalMapping addressMapping = event.getValue("address");
            assert addressMapping != null;
            String address = addressMapping.getAsString();

            try {
                this.respondAddress(event, address);
            } catch (IOException | SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        } else if (modalID.startsWith("imagesCommandCitySearch")) {
            ModalMapping cityMapping = event.getValue("city");
            assert cityMapping != null;
            String cityName = cityMapping.getAsString();

            try {
                this.respondCity(event, cityName);
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        }

    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

        if (country == null) {
            return;
        }

        if (event.getChannel().getId().equals(country.getShowcaseChannelID())) {

            if (!plugin.getLinksRegistry().isLinked(event.getAuthor().getId())) {
                event.getAuthor().openPrivateChannel().queue(
                        privateChannel -> privateChannel.sendMessageEmbeds(
                                new EmbedBuilder()
                                        .setColor(Color.GREEN)
                                        .setDescription(
                                                "Has subido imagenes a <#" + event.getChannel().getId() + ">, pero no tienes una cuenta de Minecraft conectada. Conecta tu cuenta para que tus imágenes aparezcan en los comandos de Discord.\n"
                                                + "Para conectar tu cuenta, usa `/link` en Discord o en Minecraft.\n"
                                                + "Cuando la hayas conectado, usa el botón de abajo para recargar las opciones.\n\n"
                                                + "[Mensaje original](" + event.getMessage().getJumpUrl() + ")"
                                        ).build()
                        ).addActionRow(
                                Button.of(
                                        ButtonStyle.PRIMARY,
                                        "showcaseReloadLink?id=" + event.getMessageId() + "&country=" + country.getName(),
                                        "Recargar",
                                        Emoji.fromUnicode("U+1F504")
                                )
                        ).queue()
                );
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getAuthor().getId()));

            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            int imageCounter = 0;
            for (Message.Attachment attachment : attachments) {
                if (attachment.isImage()) {
                    imageCounter++;
                }
            }

            if (imageCounter == 0) {
                return;
            }

            boolean projects, finishedProjects, events;
            try {
                projects = !s.getProjectManager().getProjects(new CountrySQLSelector(country)).isEmpty();
                finishedProjects = s.getProjectManager().getFinishedProjects(country) > 0;

                ResultSet set = plugin.getSqlManager().select(
                        "build_events",
                        new SQLColumnSet("COUNT(id) as count"),
                        new SQLANDConditionSet(
                                new SQLJSONArrayCondition("members", s.getUUID())
                        )
                ).retrieve();

                set.next();
                events = set.getInt("count") > 0;

            } catch (SQLException ignored) {return;}
            event.getAuthor().openPrivateChannel().queue(
                    privateChannel -> privateChannel.sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setColor(Color.GREEN)
                                    .setDescription(
                                            "Has subido imágenes a <#" + event.getChannel().getId() + ">. Por favor, selecciona a que proyecto o evento pertenecen.\n\n"
                                            + "[Mensaje original](" + event.getMessage().getJumpUrl() + ")"
                                    ).build()
                    )
                            .addActionRow(
                                    Button.of(
                                            ButtonStyle.SUCCESS,
                                            "showcaseSelectProjects?id=" + event.getMessageId() + "&country=" + country.getName(),
                                            "Proyectos",
                                            Emoji.fromUnicode("U+2692")
                                    ).withDisabled(!projects),
                                    Button.of(
                                            ButtonStyle.SUCCESS,
                                            "showcaseSelectFinished?id=" + event.getMessageId() + "&country=" + country.getName(),
                                            "Proy. terminados",
                                            Emoji.fromUnicode("U+2705")
                                    ).withDisabled(!finishedProjects),
                                    Button.of(
                                            ButtonStyle.SUCCESS,
                                            "showcaseSelectEvents?id=" + event.getMessageId() + "&country=" + country.getName(),
                                            "Eventos",
                                            Emoji.fromUnicode("U+1F4C6")
                                    ).withDisabled(!events)
                            ).queue()
            );
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (event.getName().equals("Establecer proyecto o evento")) {

            if (!event.getTarget().getAuthor().getId().equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "No puedes hacer esto.");
                return;
            }

            assert event.getGuild() != null;
            assert event.getChannel() != null;
            Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

            if (country == null) {
                return;
            }

            if (event.getChannel().getId().equals(country.getShowcaseChannelID())) {

                if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                    event.getUser().openPrivateChannel().queue(
                            privateChannel -> privateChannel.sendMessageEmbeds(
                                    new EmbedBuilder()
                                            .setColor(Color.GREEN)
                                            .setDescription(
                                                    "Has subido imagenes a <#" + event.getChannel().getId() + ">, pero no tienes una cuenta de Minecraft conectada. Conecta tu cuenta para que tus imágenes aparezcan en los comandos de Discord.\n"
                                                            + "Para conectar tu cuenta, usa `/link` en Discord o en Minecraft.\n"
                                                            + "Cuando la hayas conectado, usa el botón de abajo para recargar las opciones.\n\n"
                                                            + "[Mensaje original](" + event.getTarget().getJumpUrl() + ")"
                                            ).build()
                            ).addActionRow(
                                    Button.of(
                                            ButtonStyle.PRIMARY,
                                            "showcaseReloadLink?id=" + event.getInteraction().getTarget().getId() + "&country=" + country.getName(),
                                            "Recargar",
                                            Emoji.fromUnicode("U+1F504")
                                    )
                            ).queue()
                    );
                }

                ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

                List<Message.Attachment> attachments = event.getInteraction().getTarget().getAttachments();
                int imageCounter = 0;
                for (Message.Attachment attachment : attachments) {
                    if (attachment.isImage()) {
                        imageCounter++;
                    }
                }

                if (imageCounter == 0) {
                    return;
                }

                boolean projects, finishedProjects, events;
                try {
                    projects = !s.getProjectManager().getProjects(new CountrySQLSelector(country)).isEmpty();
                    finishedProjects = s.getProjectManager().getFinishedProjects(country) > 0;

                    ResultSet set = plugin.getSqlManager().select(
                            "build_events",
                            new SQLColumnSet("COUNT(id) as count"),
                            new SQLANDConditionSet(
                                    new SQLJSONArrayCondition("members", s.getUUID())
                            )
                    ).retrieve();

                    set.next();
                    events = set.getInt("count") > 0;

                } catch (SQLException ignored) {return;}
                event.getUser().openPrivateChannel().queue(
                        privateChannel -> privateChannel.sendMessageEmbeds(
                                        new EmbedBuilder()
                                                .setColor(Color.GREEN)
                                                .setDescription(
                                                        "Has subido imágenes a <#" + event.getChannel().getId() + ">. Por favor, selecciona a que proyecto o evento pertenecen.\n\n"
                                                                + "[Mensaje original](" + event.getTarget().getJumpUrl() + ")"
                                                ).build()
                                )
                                .addActionRow(
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "showcaseSelectProjects?id=" + event.getInteraction().getTarget().getId() + "&country=" + country.getName(),
                                                "Proyectos",
                                                Emoji.fromUnicode("U+2692")
                                        ).withDisabled(!projects),
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "showcaseSelectFinished?id=" + event.getInteraction().getTarget().getId() + "&country=" + country.getName(),
                                                "Proy. terminados",
                                                Emoji.fromUnicode("U+2705")
                                        ).withDisabled(!finishedProjects),
                                        Button.of(
                                                ButtonStyle.SUCCESS,
                                                "showcaseSelectEvents?id=" + event.getInteraction().getTarget().getId() + "&country=" + country.getName(),
                                                "Eventos",
                                                Emoji.fromUnicode("U+1F4C6")
                                        ).withDisabled(!events)
                                ).queue()
                );
                DiscordUtils.respondSuccessEphemeral(event, "Mensaje enviado a tus mensajes directos.");
            }
        }
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {
                Commands.slash("images", "Encuentra imágenes de diferentes lugares.")
                        .setNameLocalization(DiscordLocale.SPANISH, "imágenes")
                        .addSubcommands(
                                new SubcommandData("city", "Ve todas las imágenes de una ciudad.")
                                        .setNameLocalization(DiscordLocale.SPANISH, "ciudad")
                                        .addOptions(
                                                new OptionData(
                                                        OptionType.STRING,
                                                        "city",
                                                        "Nombre de la ciudad.",
                                                        true
                                                ).setNameLocalization(
                                                        DiscordLocale.SPANISH, "ciudad"
                                                )
                                        ),
                                new SubcommandData("address", "Ve todas las imágenes cercanas a una dirección.")
                                        .setNameLocalization(DiscordLocale.SPANISH, "dirección")
                                        .addOptions(
                                                new OptionData(
                                                        OptionType.STRING,
                                                        "address",
                                                        "Dirección a buscar.",
                                                        true
                                                ).setNameLocalization(
                                                        DiscordLocale.SPANISH, "dirección"
                                                )
                                        )
                        ),
                Commands.message("Establecer proyecto o evento").setGuildOnly(true)
        };
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

        if (country == null) {
            return;
        }

        if (event.getChannel().getId().equals(country.getShowcaseChannelID())) {
            try {
                plugin.getSqlManager().delete(
                        "showcases",
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "message_id", "=", event.getMessageId()
                                )
                        )
                ).execute();
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

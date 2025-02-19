package pizzaaxx.bteconosur.Discord.SlashCommands;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.sk89q.worldguard.util.net.HttpRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.DiscordUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

public class ManageCityCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;
    private final JsonSchema schema;

    public ManageCityCommand(@NotNull BTEConoSur plugin) throws IOException {
        this.plugin = plugin;
        JsonNode schemaNode = plugin.getJSONMapper().readTree(new File(plugin.getDataFolder(), "city_schema.json"));
        schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getChannelType() != ChannelType.TEXT) {
            return;
        }

        if (event.getName().equals("managecity")) {

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Conecta tu cuenta para poder usar esto.");
                return;
            }

            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            if (!serverPlayer.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
                DiscordUtils.respondError(event, "Solo jugadores con rango **ADMIN** pueden usar esto.");
                return;
            }

            Guild guild = event.getGuild();
            assert guild != null;

            String subcommand = event.getSubcommandName();
            assert subcommand != null;

            if (subcommand.equals("create")) {

                OptionMapping fileMapping = event.getOption("archivo");
                assert fileMapping != null;
                Message.Attachment file = fileMapping.getAsAttachment();

                try {
                    URL url = new URL(file.getUrl());
                    InputStream is = HttpRequest.get(url).execute().getInputStream();
                    JsonNode node = plugin.getJSONMapper().readTree(is);

                    Set<ValidationMessage> errors = schema.validate(node);

                    if (!errors.isEmpty()) {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setColor(Color.RED);
                        builder.setTitle("Archivo JSON inválido.");
                        builder.setDescription(
                                "```" + errors.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("```\n```")) + "```"
                        );
                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                        return;
                    }

                    Country country = plugin.getCountryManager().guilds.get(guild.getId());

                    OptionMapping nameMapping = event.getOption("nombre");
                    assert nameMapping != null;
                    String name = nameMapping.getAsString();

                    if (!name.matches("[a-z]{1,32}")) {
                        DiscordUtils.respondError(event, "Nombre inválido.");
                        return;
                    }

                    if (plugin.getCityManager().exists(name)) {
                        DiscordUtils.respondError(event, "La ciudad introducida ya existe.");
                        return;
                    }

                    OptionMapping displayMapping = event.getOption("nombrevisible");
                    assert displayMapping != null;
                    String displayName = displayMapping.getAsString();

                    if (!displayName.matches("[A-Za-zÁÉÍÓÚáéíóúÑñäëïöüÄËÏÖÜ\\-_., \\d]{1,32}")) {
                        DiscordUtils.respondError(event, "Nombre visible inválido.");
                        return;
                    }

                    File target = new File(plugin.getDataFolder(), "cities/" + name + ".json");
                    if (target.createNewFile()) {
                        InputStream is2 = HttpRequest.get(url).execute().getInputStream();
                        Files.copy(is2, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        DiscordUtils.respondError(event, "Ha ocurrido un error.");
                        return;
                    }

                    try {
                        plugin.getSqlManager().insert(
                                "cities",
                                new SQLValuesSet(
                                        new SQLValue("name", name),
                                        new SQLValue("display_name", displayName),
                                        new SQLValue("country", country)
                                )
                        ).execute();
                    } catch (SQLException e) {
                        target.delete();
                        DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                        return;
                    }

                    plugin.getCityManager().registerName(name, displayName, node);

                    DiscordUtils.respondSuccessEphemeral(event, "Ciudad \"" + displayName + "\" creada exitosamente.");

                } catch (IOException e) {
                    e.printStackTrace();
                    DiscordUtils.respondError(event, "Ha ocurrido un error.");
                }

            } else if (subcommand.equals("edit")) {



            }

        }

        /* if (event.getName().equals("createcity")) {

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Conecta tu cuenta para poder usar esto.");
                return;
            }

            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            if (!serverPlayer.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
                DiscordUtils.respondError(event, "Solo jugadores con rango **ADMIN** pueden usar esto.");
                return;
            }

            OptionMapping fileMapping = event.getOption("archivo");
            assert fileMapping != null;
            Message.Attachment file = fileMapping.getAsAttachment();

            Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

            try {
                URL url = new URL(file.getUrl());
                Map<String, Object> map = plugin.getJSONMapper().readValue(HttpRequest.get(url).execute().getInputStream(), HashMap.class);

                List<String> created = new ArrayList<>();
                List<String> modified = new ArrayList<>();
                List<String> noChanges = new ArrayList<>();

                for (String name : map.keySet()) {

                    Map<String, Object> cityMap = (Map<String, Object>) map.get(name);
                    String displayName = cityMap.get("displayName").toString();
                    List<Object> coordinates = (List<Object>) cityMap.get("region");

                    List<BlockVector2D> vectors = new ArrayList<>();
                    for (Object obj : coordinates) {
                        List<Double> coords = (List<Double>) obj;

                        vectors.add(new Coords2D(plugin, coords.get(1), coords.get(0)).toBlockVector2D());
                    }

                    List<Object> urbanCoordinates = (List<Object>) cityMap.get("region");

                    List<BlockVector2D> urbanVectors;
                    if (urbanCoordinates == null) {
                        urbanVectors = null;
                    } else {
                        urbanVectors = new ArrayList<>();
                        for (Object obj : urbanCoordinates) {
                            List<Double> coords = (List<Double>) obj;

                            urbanVectors.add(new Coords2D(plugin, coords.get(1), coords.get(0)).toBlockVector2D());
                        }
                    }


                    if (plugin.getCityManager().exists(name)) {

                        boolean noChange = true;

                        City city = plugin.getCityManager().get(name);
                        if (!city.getDisplayName().equals(displayName)) {
                            noChange = false;
                            city.setDisplayName(displayName).execute();
                        }

                        ProtectedPolygonalRegion region = (ProtectedPolygonalRegion) city.getRegion();

                        if (!ListUtils.equals(region.getPoints(), vectors)) {
                            noChange = false;
                            ProtectedRegion r = new ProtectedPolygonalRegion(
                                    "city_" + city.getName(),
                                    vectors,
                                    -100,
                                    8000
                            );
                            plugin.getRegionManager().addRegion(r);
                        }

                        if (urbanVectors != null) {
                            if (!city.hasUrbanArea() || !ListUtils.equals(city.getUrbanRegion().getPoints(), urbanVectors)) {
                                noChange = false;
                                city.setUrbanArea(urbanVectors).execute();
                            }
                        } else {
                            if (city.hasUrbanArea()) {
                                noChange = false;
                                city.deleteUrbanArea().execute();
                            }
                        }

                        if (noChange) {
                            noChanges.add(displayName);
                        } else {
                            modified.add(displayName);
                        }

                    } else {

                        plugin.getCityManager().createCity(
                                name,
                                displayName,
                                country,
                                vectors
                        ).execute();

                        if (urbanCoordinates != null) {
                            City city = plugin.getCityManager().get(name);
                            city.setUrbanArea(urbanVectors).execute();
                        }

                        created.add(displayName);

                    }

                }

                event.replyEmbeds(
                        DiscordUtils.fastEmbed(
                                Color.GREEN,
                                "Ciudades creadas",
                                ":pushpin: **Ciudades creadas:** " + (created.isEmpty() ? "Ninguna." : String.join(", ", created)) +
                                        "\n:pencil2: **Ciudaded editadas:** " + (modified.isEmpty() ? "Ninguna." : String.join(", ", modified)) +
                                        "\n:book: **Ciudades sin modificaciones:** " + (noChanges.isEmpty() ? "Ninguna." : String.join(", ", noChanges))
                        )
                ).setEphemeral(true).queue();

            } catch (IOException | CityActionException | SQLException e) {
                e.printStackTrace();
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        } */
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "managecity",
                "Crea ciudades desde Discord"
        ).addSubcommands(
                new SubcommandData("create", "Crea una ciudad.")
                        .addOption(
                                OptionType.STRING,
                                "nombre",
                                "El nombre que se usará para identificar a la ciudad. No puede contener espacios ni mayúsculas.",
                                true
                        ).addOption(
                                OptionType.STRING,
                                "nombrevisible",
                                "El nombre que se mostrará a los usuarios.",
                                true
                        )
                        .addOption(
                                OptionType.ATTACHMENT,
                                "archivo",
                                "Un archivo JSON válido.",
                                true
                        ),
                new SubcommandData("edit", "Edita algún parámetro de una ciudad.")
                        .addOption(
                                OptionType.STRING,
                                "nombre",
                                "El nombre que se usará para identificar a la ciudad. No puede contener espacios ni mayúsculas.",
                                true
                        ).addOption(
                                OptionType.STRING,
                                "nombrevisible",
                                "El nombre que se mostrará a los usuarios."
                        )
                        .addOption(
                                OptionType.ATTACHMENT,
                                "archivo",
                                "Un archivo JSON válido."
                        )
        )};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

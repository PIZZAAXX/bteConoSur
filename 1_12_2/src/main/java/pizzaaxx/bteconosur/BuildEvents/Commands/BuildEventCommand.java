package pizzaaxx.bteconosur.BuildEvents.Commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.util.net.HttpRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.scheduledevent.update.ScheduledEventUpdateStatusEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.BuildEvents.BuildEvent;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Discord.SlashCommands.SlashCommandContainer;
import pizzaaxx.bteconosur.Inventory.CustomSlotsPaginatedGUI;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.CoordinatesUtils;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.RegionUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class BuildEventCommand extends ListenerAdapter implements SlashCommandContainer, CommandExecutor, Prefixable {

    private final BTEConoSur plugin;

    public BuildEventCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    // DISCORD
    // /buildevent edit name
    // /buildevent edit image
    // /buildevent edit description
    // /buildevent edit datefrom*
    // /buildevent edit minimumunlocked*
    // /buildevent post

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "buildevent",
                "Maneja eventos de construcción."
        )
                .addSubcommandGroups(
                        new SubcommandGroupData(
                                "edit",
                                "Edita datos de un evento."
                        )
                                .addSubcommands(
                                        new SubcommandData(
                                                "name",
                                                "Cambia el nombre de un evento."
                                        ).setNameLocalization(
                                                DiscordLocale.SPANISH, "nombre"
                                        ),
                                        new SubcommandData(
                                                "image",
                                                "Cambia la imagen de un evento."
                                        ).addOption(
                                                OptionType.ATTACHMENT,
                                                "imagen",
                                                "La nueva imagen del evento."
                                        ).setNameLocalization(
                                                DiscordLocale.SPANISH, "imagen"
                                        ),
                                        new SubcommandData(
                                                "description",
                                                "Cambia la descripción del evento."
                                        ).setNameLocalization(
                                                DiscordLocale.SPANISH, "descripción"
                                        ),
                                        new SubcommandData(
                                                "dates",
                                                "Cambia las fechas de inicio y término del evento."
                                        ).setNameLocalization(
                                                DiscordLocale.SPANISH, "fechas"
                                        ),
                                        new SubcommandData(
                                                "minimumunlocked",
                                                "Cambia el tipo de proyecto mínimo que debe un jugador tener desbloqueado para unirse."
                                        ).setNameLocalization(
                                                DiscordLocale.SPANISH, "mínimodesbloqueado"
                                        ),
                                        new SubcommandData(
                                                "points",
                                                "Cambia los puntos otorgados por el evento."
                                        ).setNameLocalization(
                                                DiscordLocale.SPANISH, "puntos"
                                        )
                                ).setNameLocalization(
                                        DiscordLocale.SPANISH, "editar"
                                )
                )
                .addSubcommands(
                        new SubcommandData(
                                "post",
                                "Publica un evento de construcción."
                        ).setNameLocalization(
                                DiscordLocale.SPANISH, "publicar"
                        )
                ).setGuildOnly(true)};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("buildevent")) {

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo administradores pueden usar este comando.");
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            if (!s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
                DiscordUtils.respondError(event, "Solo administradores pueden usar este comando.");
                return;
            }

            assert event.getGuild() != null;
            Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

            BuildEvent buildEvent = country.getBuildEvent();
            if (event.getSubcommandGroup() != null && event.getSubcommandGroup().equals("edit")) {

                if (buildEvent.getStatus() != BuildEvent.Status.EDITED && buildEvent.getStatus() != BuildEvent.Status.POSTED) {
                    return;
                }

                assert event.getSubcommandName() != null;
                switch (event.getSubcommandName()) {
                    case "name": {

                        Modal modal = Modal.create(
                                "buildEventEditName?country=" + country.getName(),
                                "Editar nombre"
                        ).addActionRows(
                                ActionRow.of(
                                        TextInput.create(
                                                        "name",
                                                        "Nombre",
                                                        TextInputStyle.SHORT
                                                )
                                                .setRequired(true)
                                                .setMaxLength(64)
                                                .setValue(buildEvent.getName())
                                                .setPlaceholder("Introduce el nombre del evento.")
                                                .build()
                                )
                        ).build();

                        event.replyModal(modal).queue();

                        break;
                    }
                    case "description": {

                        Modal modal = Modal.create(
                                "buildEventEditDescription?country=" + country.getName(),
                                "Editar descripción"
                        ).addActionRows(
                                ActionRow.of(
                                        TextInput.create(
                                                        "description",
                                                        "Descripción",
                                                        TextInputStyle.SHORT
                                                )
                                                .setRequired(true)
                                                .setMaxLength(1000)
                                                .setValue(buildEvent.getDescription())
                                                .setPlaceholder("Introduce la descripción del evento.")
                                                .build()
                                )
                        ).build();

                        event.replyModal(modal).queue();

                        break;
                    }
                    case "points": {
                        Modal modal = Modal.create(
                                "buildEventEditPoints?country=" + country.getName(),
                                "Editar puntaje otorgado"
                        ).addActionRows(
                                ActionRow.of(
                                        TextInput.create(
                                                        "points",
                                                        "Puntos",
                                                        TextInputStyle.SHORT
                                                )
                                                .setRequired(true)
                                                .setMaxLength(1000)
                                                .setValue(Integer.toString(buildEvent.getPointsGiven()))
                                                .setPlaceholder("Introduce el puntaje otorgado por el evento.")
                                                .build()
                                )
                        ).build();

                        event.replyModal(modal).queue();
                        break;
                    }
                    case "image": {

                        OptionMapping imageMapping = event.getOption("imagen");
                        InputStream is = null;
                        if (imageMapping != null) {
                            Message.Attachment image = imageMapping.getAsAttachment();
                            assert image.getFileExtension() != null;
                            if (!(image.getFileExtension().equals("png") || image.getFileExtension().equals("jpg") || image.getFileExtension().equals("jpeg"))) {
                                DiscordUtils.respondError(event, "Introduce un archivo de imagen válido.");
                                return;
                            }

                            try {
                                is = HttpRequest.get(new URL(image.getUrl())).execute().getInputStream();
                            } catch (IOException e) {
                                e.printStackTrace();
                                DiscordUtils.respondError(event, "Ha ocurrido un error de I/O.");
                                return;
                            }
                        }

                        try {
                            buildEvent.setImage(is);
                        } catch (IOException e) {
                            e.printStackTrace();
                            DiscordUtils.respondError(event, "Ha ocurrido un error de I/O.");
                            return;
                        }
                        DiscordUtils.respondSuccessEphemeral(event, "Imagen del evento modificada correctamente.");

                        break;
                    }
                    case "dates": {

                        if (buildEvent.getStatus() != BuildEvent.Status.EDITED) {
                            DiscordUtils.respondError(event, "No se pueden cambiar las fechas una vez el evento fue publicado.");
                            return;
                        }

                        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                        Date start = null;
                        if (buildEvent.getStart() != null) {
                            start = new Date(buildEvent.getStart());
                        }

                        Date end = null;
                        if (buildEvent.getEnd() != null) {
                            end = new Date(buildEvent.getEnd());
                        }

                        Modal modal = Modal.create(
                                "buildEventEditDates?country=" + country.getName(),
                                "Editar fechas"
                        ).addActionRows(
                                ActionRow.of(
                                        TextInput.create(
                                                        "start",
                                                        "Inicio (Formato: dd/MM/yyyy HH:mm:ss)",
                                                        TextInputStyle.SHORT
                                                )
                                                .setRequired(true)
                                                .setMaxLength(1000)
                                                .setValue((start != null ? format.format(start) : null))
                                                .setPlaceholder("Introduce la fecha de inicio del evento.")
                                                .build()
                                ),
                                ActionRow.of(
                                        TextInput.create(
                                                        "end",
                                                        "Término (Formato: dd/MM/yyyy HH:mm:ss)",
                                                        TextInputStyle.SHORT
                                                )
                                                .setRequired(true)
                                                .setMaxLength(1000)
                                                .setValue((end != null ? format.format(end) : null))
                                                .setPlaceholder("Introduce la fecha de término del evento.")
                                                .build()
                                )
                        ).build();

                        event.replyModal(modal).queue();

                        break;
                    }
                    case "minimumunlocked": {

                        if (buildEvent.getStatus() != BuildEvent.Status.EDITED) {
                            DiscordUtils.respondError(event, "No se puede cambiar el tipo mínimo desbloqueado una vez el evento fue publicado.");
                            return;
                        }

                        StringSelectMenu.Builder typeMenu = StringSelectMenu.create("buildEventEditMinimumType?country=" + country.getName() + "&user=" + event.getUser().getId());
                        typeMenu.addOption(
                                "Ninguno",
                                "null"
                        );
                        for (ProjectType type : country.getProjectTypes()) {
                            typeMenu.addOption(
                                    type.getDisplayName(),
                                    type.getName()
                            );
                        }
                        typeMenu.setPlaceholder("Selecciona un tipo de proyecto mínimo desbloqueado.");
                        if (buildEvent.getMinimumType() != null) {
                            typeMenu.setDefaultValues(buildEvent.getMinimumType().getName());
                        }

                        event.replyComponents(
                                ActionRow.of(
                                        typeMenu.build()
                                )
                        ).queue();

                        break;
                    }
                }
            }
            assert event.getSubcommandName() != null;
            if (event.getSubcommandName().equals("post")) {

                if (buildEvent.getStatus() != BuildEvent.Status.EDITED) {
                    DiscordUtils.respondError(event, "El evento ya fue publicado.");
                    return;
                }

                if (!buildEvent.canBePosted()) {
                    DiscordUtils.respondError(event, "El evento tiene datos faltantes.");
                    return;
                }

                EmbedBuilder builder = buildEvent.getEmbed();

                Button acceptButton = Button.of(
                        ButtonStyle.SUCCESS,
                        "buildEventPostAccept?country=" + country.getName() + "&user=" + event.getUser().getId(),
                        "Aceptar",
                        Emoji.fromCustom(
                                "approve",
                                959984723868913714L,
                                false
                        )
                );

                Button rejectButton = Button.of(
                        ButtonStyle.DANGER,
                        "buildEventPostReject?country=" + country.getName() + "&user=" + event.getUser().getId(),
                        "Rechazar",
                        Emoji.fromCustom(
                                "reject",
                                959984723789250620L,
                                false
                        )
                );

                event.deferReply().queue();

                if (buildEvent.getImage().exists()) {

                    builder.setImage("attachment://image.png");

                    event.getHook().editOriginal("Vista previa:")
                            .setEmbeds(builder.build())
                            .setComponents(
                                    ActionRow.of(
                                            acceptButton, rejectButton
                                    )
                            )
                            .setFiles(FileUpload.fromData(buildEvent.getImage(), "image.png"))
                            .queue();


                } else {
                    event.getHook().editOriginal("Vista previa:")
                            .setEmbeds(builder.build())
                            .setComponents(
                                    ActionRow.of(
                                            acceptButton, rejectButton
                                    )
                            )
                            .queue();
                }
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().startsWith("buildEventEdit")) {
            Map<String, String> query = StringUtils.getQuery(event.getModalId().split("\\?")[1]);

            Country country = plugin.getCountryManager().get(query.get("country"));

            BuildEvent buildEvent = country.getBuildEvent();

            try {

                switch (event.getModalId().replace("buildEventEdit", "").split("\\?")[0].toLowerCase()) {
                    case "name": {

                        ModalMapping nameMapping = event.getValue("name");
                        assert nameMapping != null;
                        String name = nameMapping.getAsString();

                        buildEvent.setName(name);

                        DiscordUtils.respondSuccessEphemeral(event, "Nombre del evento editado correctamente.");
                        break;
                    }
                    case "description": {

                        ModalMapping descriptionMapping = event.getValue("description");
                        assert descriptionMapping != null;
                        String description = descriptionMapping.getAsString();

                        buildEvent.setDescription(description);

                        DiscordUtils.respondSuccessEphemeral(event, "Descripción del evento editada correctamente.");

                        break;
                    }
                    case "points": {

                        ModalMapping pointsMapping = event.getValue("points");
                        assert pointsMapping != null;
                        int points;
                        try {
                            points = Integer.parseInt(pointsMapping.getAsString());
                        } catch (NumberFormatException e) {
                            DiscordUtils.respondError(event, "Introduce un número válido.");
                            return;
                        }

                        if (points < 0) {
                            DiscordUtils.respondError(event, "Introduce un número mayor o igual a cero.");
                            return;
                        }

                        buildEvent.setPointsGiven(points);

                        DiscordUtils.respondSuccessEphemeral(event, "Puntaje otorgado por el evento editado correctamente.");

                        break;
                    }
                    case "dates": {

                        SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                        ModalMapping fromMapping = event.getValue("start");
                        assert fromMapping != null;
                        Date start;
                        try {
                            start = df2.parse(fromMapping.getAsString());
                        } catch (ParseException e) {
                            DiscordUtils.respondError(event, "Error de formato.");
                            return;
                        }

                        ModalMapping toMapping = event.getValue("end");
                        assert toMapping != null;
                        Date end;
                        try {
                            end = df2.parse(toMapping.getAsString());
                        } catch (ParseException e) {
                            DiscordUtils.respondError(event, "Error de formato.");
                            return;
                        }

                        if (end.before(start) || end.equals(start) || end.getTime() - start.getTime() < 3600000) {
                            DiscordUtils.respondError(event, "La duración mínima de un evento es de una hora.");
                            return;
                        }

                        buildEvent.setStart(start);
                        buildEvent.setEnd(end);

                        DiscordUtils.respondSuccessEphemeral(event, "Fechas del evento editadas correctamente.");

                        break;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {

        String id = event.getInteraction().getSelectMenu().getId();
        assert id != null;
        if (id.startsWith("buildEventEditMinimumType")) {
            Map<String, String> query = StringUtils.getQuery(id.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar el menú.");
                return;
            }

            Country country = plugin.getCountryManager().get(query.get("country"));

            if (country.getBuildEvent().getStatus() != BuildEvent.Status.EDITED) {
                DiscordUtils.respondError(event, "El evento ya ha sido publicado.");
                return;
            }

            String typeName = (event.getValues().get(0).equals("null") ? null : event.getValues().get(0));

            try {
                plugin.getSqlManager().update(
                        "build_events",
                        new SQLValuesSet(
                                new SQLValue(
                                        "minimum_type", typeName
                                )
                        ),
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "id", "=", country.getBuildEvent().getId()
                                )
                        )
                ).execute();

                event.getMessage().delete().queue();
                DiscordUtils.respondSuccessEphemeral(event, "Mínimo tipo desbloqueado del evento editado correctamente.");
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }

    }


    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        if (event.getButton().getId() == null) {
            return;
        }

        if (event.getButton().getId().startsWith("buildEventPost")) {

            Map<String, String> query = StringUtils.getQuery(event.getButton().getId().split("\\?")[1]);
            Country country = plugin.getCountryManager().get(query.get("country"));
            if (!event.getUser().getId().equals(query.get("user"))) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            BuildEvent buildEvent = country.getBuildEvent();

            if (buildEvent.getStatus() != BuildEvent.Status.EDITED) {
                DiscordUtils.respondError(event, "El proyecto ya ha sido publicado.");
                return;
            }

            if (event.getButton().getId().startsWith("buildEventPostAccept")) {

                if (buildEvent.getStart() < System.currentTimeMillis()) {
                    DiscordUtils.respondError(event, "La fecha de inicio del evento es anterior a la fecha actual.");
                    return;
                }

                try {
                    buildEvent.post();
                    event.getMessage().delete().queue();
                    DiscordUtils.respondSuccessEphemeral(event, "Evento publicado correctamente.");
                } catch (IOException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error de I/O.");
                }

            } else {
                event.getMessage().delete().queue();
            }
        }
    }

    @Override
    public void onScheduledEventUpdateStatus(@NotNull ScheduledEventUpdateStatusEvent event) {

        Country country = plugin.getCountryManager().guilds.get(event.getGuild().getId());

        if (country != null) {
            if (event.getNewStatus() == ScheduledEvent.Status.ACTIVE) {
                country.getBuildEvent().activate();
            } else if (event.getNewStatus() == ScheduledEvent.Status.COMPLETED) {
                try {
                    country.getBuildEvent().finish();
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 1) {

            CustomSlotsPaginatedGUI gui = new CustomSlotsPaginatedGUI(
                    "Selecciona un evento",
                    3,
                    new Integer[] {
                            10, 11, 12, 13, 14, 15, 16
                    },
                    9, 17
            );
            for (Country country : plugin.getCountryManager().getAllCountries()) {

                BuildEvent event = country.getBuildEvent();

                gui.addPaginated(
                        ItemBuilder.head(
                                country.getHeadValue(),
                                "§a§l" + country.getDisplayName() + " §7- " + (event.getStatus() == BuildEvent.Status.EDITED ? "§cNo disponible" : "§a" + event.getName()),
                                null
                        ),
                        (event.getStatus() == null ? null : click -> p.teleport(CoordinatesUtils.blockVector2DtoLocation(plugin, RegionUtils.getAveragePoint((ProtectedPolygonalRegion) event.getRegion())))),
                        null, null, null
                );
            }

            gui.openTo(p, plugin);

        } else {
            switch (args[0]) {
                case "region": {

                    ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

                    if (!s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
                        p.sendMessage(getPrefix() + "Solo administradores pueden usar esto.");
                        return true;
                    }

                    Region selection;
                    try {
                        selection = plugin.getWorldEdit().getSelection(p);
                    } catch (IncompleteRegionException e) {
                        p.sendMessage(getPrefix() + "Selecciona un área poligonal completa.");
                        return true;
                    }

                    if (!(selection instanceof Polygonal2DRegion)) {
                        p.sendMessage(getPrefix() + "Selecciona un área poligonal completa.");
                        return true;
                    }

                    Polygonal2DRegion polyRegion = (Polygonal2DRegion) selection;

                    Country country = plugin.getCountryManager().getCountryAt(CoordinatesUtils.blockVector2DtoLocation(plugin, polyRegion.getPoints().get(0)));

                    if (country == null) {
                        p.sendMessage(getPrefix() + "El área seleccionada no pertenece a ningún país.");
                        return true;
                    }

                    BuildEvent event = country.getBuildEvent();

                    if (event.getStatus() != BuildEvent.Status.EDITED) {
                        p.sendMessage(getPrefix() + "El proyecto ya ha sido publicado.");
                        return true;
                    }

                    try {
                        event.setRegion(polyRegion.getPoints());
                        p.sendMessage(getPrefix() + "Región del evento modificada correctamente.");
                    } catch (SQLException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                    }
                    break;
                }
                case "spawn": {

                    ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

                    if (!s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
                        p.sendMessage(getPrefix() + "Solo administradores pueden usar esto.");
                        return true;
                    }

                    Country country = plugin.getCountryManager().getCountryAt(p.getLocation());

                    if (country ==  null) {
                        p.sendMessage(getPrefix() + "No estás dentro de ningún país.");
                        return true;
                    }

                    BuildEvent event = country.getBuildEvent();

                    if (event.getStatus() != BuildEvent.Status.EDITED) {
                        p.sendMessage(getPrefix() + "El proyecto ya ha sido publicado.");
                        return true;
                    }

                    try {
                        event.setSpawnPoint(p.getLocation());

                        p.sendMessage(getPrefix() + "Lugar de aparición del evento editada correctamente.");

                    } catch (SQLException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                    }

                    break;
                }
                case "join": {
                    boolean found = false;
                    for (ProtectedRegion region : plugin.getRegionManager().getApplicableRegions(p.getLocation())) {
                        if (region.getId().startsWith("event_")) {
                            Country country = plugin.getCountryManager().getCountryAt(p.getLocation());

                            BuildEvent event = country.getBuildEvent();

                            if (event.getStatus() == BuildEvent.Status.EDITED) {
                                break;
                            }

                            if (!event.getMembers().contains(p.getUniqueId())) {
                                try {
                                    event.addMember(p.getUniqueId());
                                    if (event.getStatus() == BuildEvent.Status.POSTED) {
                                        p.sendMessage(getPrefix() + "¡Bienvenido al evento! Te avisaremos cuando comience.");
                                    } else if (event.getStatus() == BuildEvent.Status.ACTIVE) {
                                        p.sendMessage(getPrefix() + "¡Bienvenido al evento!");
                                    }
                                } catch (SQLException e) {
                                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                }

                            } else {
                                p.sendMessage(getPrefix() + "Ya eres parte del evento.");
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        p.sendMessage(getPrefix() + "No estás dentro de la zona de ningún evento.");
                    }
                    break;
                }
                case "leave": {
                    boolean found = false;
                    for (ProtectedRegion region : plugin.getRegionManager().getApplicableRegions(p.getLocation())) {
                        if (region.getId().startsWith("event_")) {
                            Country country = plugin.getCountryManager().getCountryAt(p.getLocation());

                            BuildEvent event = country.getBuildEvent();

                            if (event.getStatus() == BuildEvent.Status.EDITED) {
                                break;
                            }

                            if (event.getMembers().contains(p.getUniqueId())) {
                                try {
                                    event.removeMember(p.getUniqueId());
                                    p.sendMessage(getPrefix() + "Has abandonado el evento.");
                                } catch (SQLException e) {
                                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                }

                            } else {
                                p.sendMessage(getPrefix() + "No eres parte del evento.");
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        p.sendMessage(getPrefix() + "No estás dentro de la zona de ningún evento.");
                    }
                    break;
                }
            }
        }


        return true;
    }

    @Override
    public String getPrefix() {
        return "§f[§1EVENTO§f] §7>> §f";
    }
}

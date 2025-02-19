package pizzaaxx.bteconosur.Projects.Commands.Listeners;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.DiscordUtils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectRedefineRequestListener extends ListenerAdapter implements Prefixable {

    private final BTEConoSur plugin;

    public ProjectRedefineRequestListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {

        if (event.getInteraction().getSelectMenu().getId().startsWith("projectRedefine")) {

            try {
                ResultSet set = plugin.getSqlManager().select(
                        "project_redefine_requests",
                        new SQLColumnSet(
                                "*"
                        ),
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "message_id", "=", event.getMessageId()
                                )
                        )
                ).retrieve();

                if (set.next()) {

                    if (!plugin.getProjectRegistry().exists(set.getString("project_id"))) {
                        plugin.getSqlManager().delete(
                                "project_redefine_requests",
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "message_id", "=", event.getMessage().getId()
                                        )
                                )
                        ).execute();

                        event.getMessage().delete().queue();

                        DiscordUtils.respondError(event, "Este proyecto ya no existe.");
                        return;
                    }

                    String moderatorID = set.getString("moderator_id");

                    if (moderatorID != null && !event.getUser().getId().equals(moderatorID)) {
                        plugin.getBot().retrieveUserById(moderatorID).queue(
                                user -> DiscordUtils.respondError(event, "Esta solicitud ya está siendo revisada por " + user.getName() + "#" + user.getDiscriminator() + ".")
                        );
                        return;
                    }

                    Country country = plugin.getCountryManager().get(set.getString("country"));

                    if (event.getInteraction().getSelectMenu().getId().equals("projectRedefineTypeSelector")) {

                        String typeString = event.getValues().get(0);
                        ProjectType type = country.getProjectType(typeString);

                        StringSelectMenu.Builder pointsMenu = StringSelectMenu.create("projectRedefinePointsSelector");
                        pointsMenu.setPlaceholder("Selecciona un puntaje");
                        for (int option : type.getPointsOptions()) {
                            pointsMenu.addOption(Integer.toString(option), Integer.toString(option));
                        }

                        Button acceptButton = Button.of(
                                ButtonStyle.SUCCESS,
                                "projectRedefineAccept",
                                "Aceptar",
                                Emoji.fromCustom(
                                        "approve",
                                        959984723868913714L,
                                        false
                                )
                        ).withDisabled(true);

                        Button rejectButton = Button.of(
                                ButtonStyle.DANGER,
                                "projectRedefineReject",
                                "Rechazar",
                                Emoji.fromCustom(
                                        "reject",
                                        959984723789250620L,
                                        false
                                )
                        );

                        plugin.getSqlManager().update(
                                "project_redefine_requests",
                                new SQLValuesSet(
                                        new SQLValue(
                                                "moderator_id", event.getUser().getId()
                                        ),
                                        new SQLValue(
                                                "target_type", typeString
                                        ),
                                        new SQLValue(
                                                "target_points", null
                                        )
                                ),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "message_id", "=", event.getMessageId()
                                        )
                                )
                        ).execute();

                        event.editComponents(
                                ActionRow.of(
                                        event.getInteraction().getSelectMenu().createCopy().setDefaultValues(typeString).build()
                                ),
                                ActionRow.of(
                                        pointsMenu.build()
                                ),
                                ActionRow.of(
                                        acceptButton,
                                        rejectButton
                                )
                        ).queue();

                    } else if (event.getInteraction().getSelectMenu().getId().equals("projectRedefinePointsSelector")) {

                        int points = Integer.parseInt(event.getValues().get(0));

                        Button acceptButton = Button.of(
                                ButtonStyle.SUCCESS,
                                "projectRedefineAccept",
                                "Aceptar",
                                Emoji.fromCustom(
                                        "approve",
                                        959984723868913714L,
                                        false
                                )
                        );

                        Button rejectButton = Button.of(
                                ButtonStyle.DANGER,
                                "projectRedefineReject",
                                "Rechazar",
                                Emoji.fromCustom(
                                        "reject",
                                        959984723789250620L,
                                        false
                                )
                        );

                        plugin.getSqlManager().update(
                                "project_redefine_requests",
                                new SQLValuesSet(
                                        new SQLValue(
                                                "moderator_id", event.getUser().getId()
                                        ),
                                        new SQLValue(
                                                "target_points", points
                                        )
                                ),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "message_id", "=", event.getMessageId()
                                        )
                                )
                        ).execute();

                        event.editComponents(
                                event.getMessage().getComponents().get(0),
                                ActionRow.of(
                                        event.getInteraction().getSelectMenu().createCopy().setDefaultValues(Integer.toString(points)).build()
                                ),
                                ActionRow.of(
                                        acceptButton,
                                        rejectButton
                                )
                        ).queue();

                    }
                } else {
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                }
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if (id != null && id.startsWith("projectRedefine")) {
            try {
                ResultSet set = plugin.getSqlManager().select(
                        "project_redefine_requests",
                        new SQLColumnSet(
                                "*"
                        ),
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "message_id", "=", event.getMessageId()
                                )
                        )
                ).retrieve();

                if (set.next()) {

                    if (!plugin.getProjectRegistry().exists(set.getString("project_id"))) {
                        plugin.getSqlManager().delete(
                                "project_redefine_requests",
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "message_id", "=", event.getMessage().getId()
                                        )
                                )
                        ).execute();

                        event.getMessage().delete().queue();

                        DiscordUtils.respondError(event, "Este proyecto ya no existe.");
                        return;
                    }

                    String moderatorID = set.getString("moderator_id");

                    if (moderatorID != null && !event.getUser().getId().equals(moderatorID)) {
                        plugin.getBot().retrieveUserById(moderatorID).queue(
                                user -> DiscordUtils.respondError(event, "Esta solicitud ya está siendo revisada por " + user.getName() + "#" + user.getDiscriminator() + ".")
                        );
                        return;
                    }

                    if (id.equals("projectRedefineAccept")) {

                        Project project = plugin.getProjectRegistry().get(set.getString("project_id"));

                        ServerPlayer s = plugin.getPlayerRegistry().get(project.getOwner());

                        List<BlockVector2D> regionPoints = new ArrayList<>();
                        JsonNode coordsNode = plugin.getJSONMapper().readTree(set.getString("region_points"));
                        for (JsonNode coord : coordsNode) {
                            regionPoints.add(new BlockVector2D(coord.path("x").asDouble(), coord.path("z").asDouble()));
                        }

                        ProtectedPolygonalRegion region = new ProtectedPolygonalRegion(
                                "project_" + set.getString("project_id"),
                                regionPoints,
                                -100, 8000
                        );
                        region.setFlag(DefaultFlag.BUILD, StateFlag.State.ALLOW);
                        region.setFlag(DefaultFlag.BUILD.getRegionGroupFlag(), RegionGroup.MEMBERS);
                        region.setPriority(1);

                        FlagRegistry registry = plugin.getWorldGuard().getFlagRegistry();

                        region.setFlag((StateFlag) registry.get("worldedit"), StateFlag.State.ALLOW);
                        region.setFlag(registry.get("worldedit").getRegionGroupFlag(), RegionGroup.MEMBERS);

                        region.setMembers(project.getRegion().getMembers());

                        plugin.getRegionManager().addRegion(region);

                        plugin.getSqlManager().update(
                                "projects",
                                new SQLValuesSet(
                                        new SQLValue(
                                                "type", set.getString("target_type")
                                        ),
                                        new SQLValue(
                                                "points", set.getString("target_points")
                                        )
                                ),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "id", "=", project.getId()
                                        )
                                )
                        ).execute();

                        project.update();

                        s.sendNotification(
                                getPrefix() + "Tu solicitud de redefinir el proyecto §a" + project.getDisplayName() + " §fha sido aceptada.",
                                "**[PROYECTOS]** » Tu solicitud de redefinir el proyecto **" + project.getDisplayName() + "** ha sido aceptada."
                        );

                        plugin.getSqlManager().delete(
                                "project_redefine_requests",
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "message_id", "=", event.getMessage().getId()
                                        )
                                )
                        ).execute();

                        event.getMessage().delete().queue();

                        DiscordUtils.respondSuccessEphemeral(event, "Solicitud aceptada correctamente.");

                    } else if (id.equals("projectRedefineReject")) {

                        Modal modal = Modal.create("projectRedefineReason", "Razón (Opcional)").addActionRows(
                                ActionRow.of(
                                        TextInput.create(
                                                "reason",
                                                "Razón de rechazo",
                                                TextInputStyle.SHORT
                                        ).setRequired(false).setPlaceholder("Introduce una razón para rechazar la solicitud").build()
                                )
                        ).build();

                        event.replyModal(modal).queue();

                    }

                } else {
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {

        if (event.getModalId().equals("projectRedefineReason")) {

            try {
                ResultSet set = plugin.getSqlManager().select(
                        "project_redefine_requests",
                        new SQLColumnSet(
                                "*"
                        ),
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "message_id", "=", event.getMessage().getId()
                                )
                        )
                ).retrieve();

                if (set.next()) {
                    ModalMapping reasonMapping = event.getValue("reason");

                    if (!plugin.getProjectRegistry().exists(set.getString("project_id"))) {
                        plugin.getSqlManager().delete(
                                "project_redefine_requests",
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "message_id", "=", event.getMessage().getId()
                                        )
                                )
                        ).execute();

                        event.getMessage().delete().queue();

                        DiscordUtils.respondError(event, "Este proyecto ya no existe.");
                        return;
                    }

                    Project project = plugin.getProjectRegistry().get(set.getString("project_id"));

                    ServerPlayer s = plugin.getPlayerRegistry().get(project.getOwner());

                    if (reasonMapping.getAsString().equals("")) {
                        s.sendNotification(
                                getPrefix() + "Tu solicitud de redefinir el proyecto §a" + project.getDisplayName() + " §fha sido rechazada.",
                                "**[PROYECTOS]** » Tu solicitud de redefinir el proyecto **" + project.getDisplayName() + "** ha sido rechazada."
                        );
                    } else {
                        s.sendNotification(
                                getPrefix() + "Tu solicitud de redefinir el proyecto §a" + project.getDisplayName() + " §fha sido rechazada. §7Razón: " + reasonMapping.getAsString(),
                                "**[PROYECTOS]** » Tu solicitud de redefinir el proyecto **" + project.getDisplayName() + "** ha sido rechazada. *Razón: " + reasonMapping.getAsString() + "*"
                        );
                    }

                    plugin.getSqlManager().delete(
                            "project_redefine_requests",
                            new SQLANDConditionSet(
                                    new SQLOperatorCondition(
                                            "message_id", "=", event.getMessage().getId()
                                    )
                            )
                    ).execute();

                    event.getMessage().delete().queue();

                    DiscordUtils.respondSuccessEphemeral(event, "Solicitud rechazada correctamente.");
                } else {
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                }

            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }

        }
    }

    @Override
    public String getPrefix() {
        return "§f[§dPROYECTO§f] §7>> §f";
    }
}

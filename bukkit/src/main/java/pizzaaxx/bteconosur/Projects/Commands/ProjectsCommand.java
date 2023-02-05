package pizzaaxx.bteconosur.Projects.Commands;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Cities.Actions.CityActionException;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Inventory.*;
import pizzaaxx.bteconosur.Player.Managers.ProjectManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.Projects.RegionSelectors.NonMemberProjectSelector;
import pizzaaxx.bteconosur.Projects.RegionSelectors.NotClaimedProjectSelector;
import pizzaaxx.bteconosur.Projects.RegionSelectors.OwnerProjectSelector;
import pizzaaxx.bteconosur.Projects.SQLSelectors.NotOwnerSQLSelector;
import pizzaaxx.bteconosur.Projects.SQLSelectors.OwnerSQLSelector;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.RegionUtils;
import pizzaaxx.bteconosur.Utils.SatMapHandler;
import xyz.upperlevel.spigot.book.BookUtil;

import java.awt.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static pizzaaxx.bteconosur.Projects.Project.MAX_PROJECTS_PER_PLAYER;

public class ProjectsCommand implements CommandExecutor, Prefixable {

    private final BTEConoSur plugin;

    public ProjectsCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());
        ProjectManager projectManager = s.getProjectManager();

        if (args.length < 1) {
            p.sendMessage(this.getPrefix() + "Introduce un subcomando. Usa §a/help project§f para obtener ayuda.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create": {

                Region region;
                try {
                    region = plugin.getWorldEdit().getSelection(p);
                } catch (IncompleteRegionException e) {
                    p.sendMessage(getPrefix() + "Selecciona un área poligonal.");
                    return true;
                }

                if (!(region instanceof Polygonal2DRegion)) {
                    p.sendMessage(getPrefix() + "Selecciona un área poligonal.");
                    return true;
                }

                Polygonal2DRegion polyRegion = (Polygonal2DRegion) region;

                Location loc = new Location(plugin.getWorld(), polyRegion.getPoints().get(0).getBlockX(), 100, polyRegion.getPoints().get(0).getBlockZ());

                if (!plugin.getCountryManager().isInsideCountry(loc)) {
                    p.sendMessage("El área seleccionada no está dentro de ningún país.");
                    return true;
                }

                Country country = plugin.getCountryManager().getCountryAt(loc);

                if (country == null) {
                    p.sendMessage(getPrefix() + "El área seleccionada no está dentro de ningún país.");
                    return true;
                }

                if (projectManager.hasAdminPermission(country)) {

                    if (args.length < 2) {
                        p.sendMessage(getPrefix() + "Introduce un tipo de proyecto.");
                        return true;
                    }

                    ProjectType type = country.getProjectType(args[1]);

                    if (type == null) {
                        p.sendMessage(getPrefix() + "Introduce un tipo de proyecto válido. §7Opciones: " + country.getProjectTypes().stream().map(ProjectType::getName).collect(Collectors.joining(", ")));
                        return true;
                    }

                    if (args.length < 3) {
                        p.sendMessage(getPrefix() + "Introduce un puntaje.");
                        return true;
                    }

                    int points;
                    try {
                        points = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        p.sendMessage(getPrefix() + "Introduce un número válido.");
                        return true;
                    }

                    if (!type.getPointsOptions().contains(points)) {
                        List<String> pointsStrings = new ArrayList<>();
                        for (int pointOption : type.getPointsOptions()) {
                            pointsStrings.add(String.valueOf(pointOption));
                        }
                        p.sendMessage(getPrefix() + "El puntaje introducido no es parte de las opciones. §7Opciones: " + String.join(", ", pointsStrings));
                        return true;
                    }

                    Project project;
                    try {
                        project = plugin.getProjectRegistry().createProject(
                                country,
                                type,
                                points,
                                polyRegion.getPoints()
                        ).exec();
                        p.sendMessage(getPrefix() + "Proyecto de tipo §a" + type.getDisplayName() + "§f y puntaje §a" + points + "§f creado con la ID §a" + project.getId() + "§f.");
                    } catch (SQLException | CityActionException | IOException e) {
                        e.printStackTrace();
                        p.sendMessage(getPrefix() + "Ha ocurrido un error.");
                        return true;
                    }
                } else {

                    try {
                        if (projectManager.getProjects(new OwnerSQLSelector(p.getUniqueId())).size() >= MAX_PROJECTS_PER_PLAYER) {
                            p.sendMessage(getPrefix() + "Solo puedes ser líder de 15 proyectos a la vez.");
                            return true;
                        }
                    } catch (SQLException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                        return true;
                    }

                    ResultSet set;
                    try {
                        set = plugin.getSqlManager().select(
                                "project_requests",
                                new SQLColumnSet(
                                        "message_id",
                                        "country"
                                ),
                                new SQLANDConditionSet(
                                        new SQLOperatorCondition(
                                                "owner", "=", p.getUniqueId()
                                        ),
                                        new SQLOperatorCondition(
                                                "country", "=", country.getName()
                                        )
                                )
                        ).retrieve();

                        BukkitRunnable sendRequest = new BukkitRunnable() {
                            @Override
                            public void run() {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.GREEN);
                                builder.setImage("attachment://map.png");
                                builder.setTitle(s.getName() + " quiere crear un proyecto.");

                                try {
                                    StringSelectMenu.Builder typeMenu = StringSelectMenu.create("projectCreationRequestTypeMenu");
                                    typeMenu.setPlaceholder("Selecciona un tipo de proyecto");

                                    for (ProjectType type : country.getProjectTypes()) {
                                        if (type.isUnlocked(projectManager)) {
                                            typeMenu.addOption(type.getDisplayName(), type.getName());
                                        }
                                    }

                                    StringSelectMenu.Builder pointsMenu = StringSelectMenu.create("projectCreationRequestPointsMenu");
                                    pointsMenu.addOption("a", "b");
                                    pointsMenu.setPlaceholder("Selecciona un puntaje");
                                    pointsMenu.setDisabled(true);

                                    Button acceptButton = Button.of(
                                            ButtonStyle.SUCCESS,
                                            "projectCreationRequestAccept",
                                            "Aceptar",
                                            Emoji.fromCustom(
                                                    "approve",
                                                    959984723868913714L,
                                                    false
                                            )
                                    ).withDisabled(true);

                                    Button rejectButton = Button.of(
                                            ButtonStyle.DANGER,
                                            "projectCreationRequestReject",
                                            "Rechazar",
                                            Emoji.fromCustom(
                                                    "reject",
                                                    959984723789250620L,
                                                    false
                                            )
                                    );

                                    country.getRequestsChannel()
                                            .sendFiles(
                                                    FileUpload.fromData(
                                                            plugin.getSatMapHandler().getMapStream(
                                                                    new SatMapHandler.SatMapPolygon(
                                                                            plugin,
                                                                            polyRegion.getPoints(),
                                                                            "3068ff"
                                                                    )
                                                            ),
                                                            "map.png"
                                                    ))
                                            .addEmbeds(builder.build())
                                            .setComponents(
                                                    ActionRow.of(
                                                            typeMenu.build()
                                                    ),
                                                    ActionRow.of(
                                                            pointsMenu.build()
                                                    ),
                                                    ActionRow.of(
                                                            acceptButton,
                                                            rejectButton
                                                    )
                                            )
                                            .queue(
                                            message -> {
                                                try {
                                                    plugin.getSqlManager().insert(
                                                            "project_requests",
                                                            new SQLValuesSet(
                                                                    new SQLValue(
                                                                            "owner", p.getUniqueId()
                                                                    ),
                                                                    new SQLValue(
                                                                            "region_points", polyRegion.getPoints()
                                                                    ),
                                                                    new SQLValue(
                                                                            "message_id", message.getId()
                                                                            ),
                                                                    new SQLValue(
                                                                            "country", country.getName()
                                                                    )
                                                            )
                                                    ).execute();
                                                } catch (SQLException e) {
                                                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                                    message.delete().queue();
                                                }
                                            }
                                    );
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    p.sendMessage(getPrefix() + "Ha ocurrido un error al enviar la solicitud.");
                                    return;
                                }
                                p.sendMessage(getPrefix() + "Solicitud enviada correctamente.");
                            }
                        };

                        if (set.next()) {
                            // OPEN GUI
                            InventoryGUI gui = new InventoryGUI(
                                    1,
                                    "¿Reemplazar la última solicitud en " + country.getDisplayName() + "?"
                            );
                            gui.setItem(
                                    ItemBuilder.head(
                                            ItemBuilder.confirmHead(),
                                            "§aConfirmar",
                                            null
                                    ),
                                    3
                            );

                            gui.setLCAction(
                                    event -> {
                                        try {
                                            String messageID = set.getString("message_id");

                                            plugin.getCountryManager().get(set.getString("country")).getRequestsChannel().deleteMessageById(messageID).queue();

                                            plugin.getSqlManager().delete(
                                                    "project_requests",
                                                    new SQLANDConditionSet(
                                                            new SQLOperatorCondition(
                                                                    "owner", "=", p.getUniqueId()
                                                            )
                                                    )
                                            ).execute();

                                            event.closeGUI();
                                        } catch (SQLException e) {
                                            event.closeGUI();
                                            p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                            return;
                                        }
                                        sendRequest.run();
                                    },
                                    3
                            );

                            gui.setItem(
                                    ItemBuilder.head(
                                            ItemBuilder.cancelHead(),
                                            "§cCancelar",
                                            null
                                    ),
                                    5
                            );
                            gui.setLCAction(
                                    InventoryGUIClickEvent::closeGUI,
                                    5
                            );
                            plugin.getInventoryHandler().open(p, gui);
                        } else {
                            sendRequest.run();
                        }
                    } catch (SQLException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                        return true;
                    }


                }

                break;
            }
            case "manage": {

                

            }
            case "borders": {
                List<String> projectIDs = plugin.getProjectRegistry().getProjectsAt(p.getLocation());
                if (projectIDs.size() == 0) {
                    p.sendMessage(getPrefix() + "No hay proyectos en este lugar.");
                } else if (projectIDs.size() == 1) {
                    Project project = plugin.getProjectRegistry().get(projectIDs.get(0));
                    Polygonal2DSelection selection = new Polygonal2DSelection(
                            plugin.getWorld(),
                            project.getRegion().getPoints(),
                            p.getLocation().getBlockY() - 10,
                            p.getLocation().getBlockY() + 10
                    );
                    plugin.getWorldEdit().setSelection(p, selection);
                } else {
                    PaginatedInventoryGUI gui = new PaginatedInventoryGUI(6, "Elige un proyecto para mostrar");
                    for (String id : projectIDs) {
                        Project project = plugin.getProjectRegistry().get(id);
                        gui.add(
                                project.getItem(),
                                event -> {
                                    event.closeGUI();
                                    Polygonal2DSelection selection = new Polygonal2DSelection(
                                            plugin.getWorld(),
                                            project.getRegion().getPoints(),
                                            p.getLocation().getBlockY() - 10,
                                            p.getLocation().getBlockY() + 10
                                    );
                                    plugin.getWorldEdit().setSelection(p, selection);
                                },
                                null,
                                null,
                                null
                        );
                    }
                    gui.openTo(p, plugin);
                }
                break;
            }
            case "name": {
                if (args.length < 2) {
                    p.sendMessage(getPrefix() + "Introduce un nombre.");
                    return true;
                }

                String name = args[1];

                if (!name.matches("[a-zA-Z1-9_]{1,32}")) {
                    p.sendMessage(getPrefix() + "Introduce un nombre válido.");
                    return true;
                }

                List<String> projectIDs = plugin.getProjectRegistry().getProjectsAt(p.getLocation(), new OwnerProjectSelector(p.getUniqueId()));

                if (projectIDs.size() == 0) {
                    p.sendMessage(getPrefix() + "No hay proyectos que lideres aquí.");
                    return true;
                } else if (projectIDs.size() == 1) {
                    Project project = plugin.getProjectRegistry().get(projectIDs.get(0));
                    try {
                        String oldName = project.getDisplayName();
                        project.setDisplayName(name).execute();
                        p.sendMessage(getPrefix() + "Nombre del proyecto §a" + oldName + "§f cambiado a §a" + name + "§f.");
                    } catch (SQLException | IOException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error.");
                    }
                } else {
                    PaginatedInventoryGUI gui = new PaginatedInventoryGUI(
                            6,
                            "Elige un proyecto"
                    );
                    for (String id : projectIDs) {
                        Project project = plugin.getProjectRegistry().get(id);
                        gui.add(
                                project.getItem(),
                                event -> {
                                    event.closeGUI();
                                    try {
                                        String oldName = project.getDisplayName();
                                        project.setDisplayName(name).execute();
                                        p.sendMessage(getPrefix() + "Nombre del proyecto §a" + oldName + "§f cambiado a §a" + name + "§f.");
                                    } catch (SQLException | IOException e) {
                                        p.sendMessage(getPrefix() + "Ha ocurrido un error.");
                                    }
                                },
                                null,
                                null,
                                null
                        );
                    }
                    gui.openTo(p, plugin);
                }
                break;
            }
            case "list": {

                try {
                    BookUtil.BookBuilder builder = BookUtil.writtenBook();
                    List<BaseComponent[]> pages = new ArrayList<>();

                    List<String> ownerIDs = new ArrayList<>(projectManager.getProjects(new OwnerSQLSelector(p.getUniqueId())));
                    Collections.sort(ownerIDs);
                    List<String> memberIDs = new ArrayList<>(projectManager.getProjects(new NotOwnerSQLSelector(p.getUniqueId())));
                    Collections.sort(memberIDs);
                    List<String> ids = new ArrayList<>(ownerIDs);
                    ids.addAll(memberIDs);

                    for (String id : ids) {
                        Project project = plugin.getProjectRegistry().get(id);
                        BlockVector2D avgPoint = RegionUtils.getAveragePoint(project.getRegion());

                        BookUtil.PageBuilder pageBuilder = BookUtil.PageBuilder.of(BookUtil.TextBuilder.of("§7\"§0" + project.getDisplayName() + "§7\"").build());
                        pageBuilder.newLine();
                        pageBuilder.newLine();
                        pageBuilder.add("• ID: §8" + project.getId());
                        pageBuilder.newLine();
                        pageBuilder.add("• País: §8" + project.getCountry().getDisplayName());
                        pageBuilder.newLine();
                        pageBuilder.add("• Tipo: §8" + project.getType().getDisplayName());
                        pageBuilder.newLine();
                        pageBuilder.add("• Ptje.: §8" + project.getPoints());
                        pageBuilder.newLine();
                        pageBuilder.add("• Coord.: §8");
                        pageBuilder.add(
                                BookUtil.TextBuilder.of(
                                        avgPoint.getBlockX() + " " + plugin.getWorld().getHighestBlockAt(avgPoint.getBlockX(), avgPoint.getBlockZ()).getLocation().getBlockY() + " " + avgPoint.getBlockZ())
                                        .onClick(BookUtil.ClickAction.runCommand("/tp " + avgPoint.getBlockX() + " " + plugin.getWorld().getHighestBlockAt(avgPoint.getBlockX(), avgPoint.getBlockZ()).getLocation().getBlockY() + " " + avgPoint.getBlockZ()))
                                        .onHover(BookUtil.HoverAction.showText("Haz click para ir"))
                                        .build());
                        pageBuilder.newLine();
                        if (project.isClaimed()) {
                            pageBuilder.add("• Líder: §8");
                            ServerPlayer owner = plugin.getPlayerRegistry().get(project.getOwner());
                            pageBuilder.add(
                                    BookUtil.TextBuilder.of(owner.getName())
                                            .onHover(BookUtil.HoverAction.showText(String.join("\n", owner.getLore(true))))
                                            .build()
                            );
                            if (project.getMembers().size() > 0) {
                                pageBuilder.add("• Mmbrs.: §8");
                                int counter = 0;
                                for (UUID memberUUID : project.getMembers()) {
                                    ServerPlayer member = plugin.getPlayerRegistry().get(memberUUID);
                                    pageBuilder.add(
                                            BookUtil.TextBuilder.of(member.getName())
                                                    .onHover(BookUtil.HoverAction.showText(String.join(", ", member.getLore(true))))
                                                    .build()
                                    );
                                    if (counter < project.getMembers().size() - 1) {
                                        pageBuilder.add(", ");
                                    }
                                    counter++;
                                }
                            }
                        }
                        pages.add(pageBuilder.build());
                    }

                    builder.pages(pages);

                    BookUtil.openPlayer(p, builder.build());
                } catch (SQLException e) {
                    e.printStackTrace();
                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                }

                break;
            }
            case "claim": {
                try {
                    if (projectManager.getProjects(new OwnerSQLSelector(p.getUniqueId())).size() >= MAX_PROJECTS_PER_PLAYER) {
                        p.sendMessage(getPrefix() + "Solo puedes ser líder de 15 proyectos a la vez.");
                        return true;
                    }
                } catch (SQLException e) {
                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                    return true;
                }

                List<String> projectIDs = plugin.getProjectRegistry().getProjectsAt(p.getLocation(), new NonMemberProjectSelector(p.getUniqueId()), new NotClaimedProjectSelector());

                if (projectIDs.size() == 0) {
                    p.sendMessage(getPrefix() + "No hay proyectos disponibles aquí.");
                } else if (projectIDs.size() == 1) {
                    Project project = plugin.getProjectRegistry().get(projectIDs.get(0));
                    if (!project.getType().isUnlocked(projectManager)) {
                        p.sendMessage(getPrefix() + "Aún no desbloqueas los proyectos de tipo §a" + project.getType().getDisplayName() + "§f. Solo puedes reclamar proyectos de tipo §a" + project.getCountry().getUnlockedProjectTypes(projectManager).stream().map(ProjectType::getDisplayName).collect(Collectors.joining("§f, §a")) + "§f.");
                        return true;
                    }

                    if (project.isClaimed()) {
                        p.sendMessage(getPrefix() + "El proyecto ya ha sido reclamado. Usa §a/p request§f para solicitar unirte.");
                        return true;
                    }

                    try {
                        project.claim(p.getUniqueId()).execute();
                        p.sendMessage(getPrefix() + "Has reclamado el proyecto §a" + project.getDisplayName() + "§f de tipo §a" + project.getType().getDisplayName() + "§f.");
                    } catch (SQLException | IOException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                    }
                } else {
                    PaginatedInventoryGUI gui = new PaginatedInventoryGUI(
                            6,
                            "Elige un proyecto para reclamar"
                    );
                    for (String id : projectIDs) {
                        Project project = plugin.getProjectRegistry().get(id);
                        gui.add(
                                project.getItem(),
                                event -> {
                                    event.closeGUI();
                                    if (!project.getType().isUnlocked(projectManager)) {
                                        p.sendMessage(getPrefix() + "Aún no desbloqueas los proyectos de tipo §a" + project.getType().getDisplayName() + "§f. Solo puedes reclamar proyectos de tipo §a" + project.getCountry().getUnlockedProjectTypes(projectManager).stream().map(ProjectType::getDisplayName).collect(Collectors.joining("§f, §a")) + "§f.");
                                        return;
                                    }

                                    if (project.isClaimed()) {
                                        p.sendMessage(getPrefix() + "El proyecto ya ha sido reclamado. Usa §a/p request§f para solicitar unirte.");
                                        return;
                                    }

                                    try {
                                        project.claim(p.getUniqueId()).execute();
                                        p.sendMessage(getPrefix() + "Has reclamado el proyecto §a" + project.getDisplayName() + "§f de tipo §a" + project.getType().getDisplayName() + "§f.");
                                    } catch (SQLException | IOException e) {
                                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                    }
                                },
                                null,
                                null,
                                null
                        );
                    }
                    gui.openTo(p, plugin);
                }
                break;
            }
            case "delete": {

                Country country = plugin.getCountryManager().getCountryAt(p.getLocation());

                if (country == null) {
                    p.sendMessage(getPrefix() + "No estás dentro de ningún país.");
                    return true;
                }

                if (!s.getProjectManager().hasAdminPermission(country)) {
                    p.sendMessage(getPrefix() + "No tienes permisos para administrar proyectos en §a" + country.getDisplayName() + "§f.");
                    return true;
                }

                List<String> projectIDs = plugin.getProjectRegistry().getProjectsAt(p.getLocation());

                if (projectIDs.size() == 0) {
                    p.sendMessage(getPrefix() + "No hay proyectos en este lugar.");
                } else if (projectIDs.size() == 1) {

                    Project project = plugin.getProjectRegistry().get(projectIDs.get(0));

                    InventoryGUI confirmGUI = new InventoryGUI(
                            1,
                            "¿Eliminar proyecto " + project.getDisplayName() + "?"
                    );
                    confirmGUI.setItem(
                            ItemBuilder.head(
                                    ItemBuilder.confirmHead(),
                                    "§aConfirmar",
                                    null
                            ),
                            3
                    );
                    confirmGUI.setLCAction(
                            confirmEvent -> {
                                confirmEvent.closeGUI();
                                try {
                                    plugin.getProjectRegistry().deleteProject(
                                            project,
                                            p.getUniqueId()
                                    ).exec();
                                    p.sendMessage(getPrefix() + "Proyecto §a" + project.getId() + "§f eliminado.");
                                } catch (SQLException e) {
                                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                }
                            },
                            3
                    );
                    confirmGUI.setItem(
                            ItemBuilder.head(
                                    ItemBuilder.cancelHead(),
                                    "§cCancelar",
                                    null
                            ),
                            5
                    );
                    confirmGUI.setLCAction(
                            InventoryGUIClickEvent::closeGUI,
                            5
                    );
                    plugin.getInventoryHandler().open(p, confirmGUI);
                } else {
                    PaginatedInventoryGUI gui = new PaginatedInventoryGUI(
                            6,
                            "Elige un proyecto para eliminar"
                    );

                    for (String id : projectIDs) {
                        Project project = plugin.getProjectRegistry().get(id);

                        gui.add(
                                project.getItem(),
                                event -> {
                                    InventoryGUI confirmGUI = new InventoryGUI(
                                            1,
                                            "¿Eliminar proyecto " + project.getDisplayName() + "?"
                                    );
                                    confirmGUI.setItem(
                                            ItemBuilder.head(
                                                    ItemBuilder.confirmHead(),
                                                    "§aConfirmar",
                                                    null
                                            ),
                                            3
                                    );
                                    confirmGUI.setLCAction(
                                            confirmEvent -> {
                                                confirmEvent.closeGUI();
                                                try {
                                                    plugin.getProjectRegistry().deleteProject(
                                                            project,
                                                            p.getUniqueId()
                                                    ).exec();
                                                    p.sendMessage(getPrefix() + "Proyecto §a" + project.getId() + "§f eliminado.");
                                                } catch (SQLException e) {
                                                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                                                }
                                            },
                                            3
                                    );
                                    confirmGUI.setItem(
                                            ItemBuilder.head(
                                                    ItemBuilder.cancelHead(),
                                                    "§cCancelar",
                                                    null
                                            ),
                                            5
                                    );
                                    confirmGUI.setLCAction(
                                            cancelEvent -> {
                                                cancelEvent.closeGUI();
                                                gui.openTo(p, plugin);
                                            },
                                            5
                                    );
                                    plugin.getInventoryHandler().open(p, confirmGUI);
                                },
                                null,
                                null,
                                null
                        );
                    }
                    gui.openTo(p, plugin);
                }

                break;
            }
        }
        return true;
    }

    private void openManageInventory(Player player, String projectID) {

    }

    @Override
    public String getPrefix() {
        return "§f[§dPROYECTO§f] §7>> §f";
    }
}

package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldedit.BlockVector2D;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Player.Managers.ProjectManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.*;
import pizzaaxx.bteconosur.SQL.Entities.SQLPolygon;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class ReviewProjectAction {

    public enum ReviewAction {
        DENY, CONTINUE, ACCEPT
    }

    private final BTEConoSur plugin;
    private final ReviewAction action;
    private final Project project;
    private final UUID moderator;

    public ReviewProjectAction(BTEConoSur plugin, ReviewAction action, Project project, UUID moderator) {
        this.plugin = plugin;
        this.action = action;
        this.project = project;
        this.moderator = moderator;
    }

    public void execute() throws SQLException, IOException {
        ServerPlayer moderator = plugin.getPlayerRegistry().get(this.moderator);
        switch (action) {
            case DENY: {

                for (UUID memberUUID : project.getAllMembers()) {
                    ServerPlayer member = plugin.getPlayerRegistry().get(memberUUID);
                    member.sendNotification(
                            "§f[§dPROYECTO§f] §7>> §fTu proyecto §a" + project.getDisplayName() + "§f ha sido rechazado.",
                            "**[PROYECTOS]** » Tu proyecto **" + project.getDisplayName() + "** ha sido rechazado."
                    );
                }
                project.getCountry().getLogsChannel().sendMessage(
                        ":mag_right: **" + moderator.getName() + "** ha rechazado el proyecto `" + project.getId() + "`."
                ).queue();

                project.emptyProject().execute();

                break;
            }
            case CONTINUE: {

                for (UUID memberUUID : project.getAllMembers()) {
                    ServerPlayer member = plugin.getPlayerRegistry().get(memberUUID);
                    member.sendNotification(
                            "§f[§dPROYECTO§f] §7>> §fTu proyecto §a" + project.getDisplayName() + "§f ha sido continuado. Puedes seguir construyendo.",
                            "**[PROYECTOS]** » Tu proyecto **" + project.getDisplayName() + "** ha sido rechazado. Puedes seguir construyendo."
                    );
                }

                project.setPending(false).execute();
                project.getCountry().getLogsChannel().sendMessage(
                        ":mag_right: **" + moderator.getName() + "** ha continuado el proyecto `" + project.getId() + "`."
                ).queue();

                break;

            }
            case ACCEPT: {

                assert project.getPending() != null;

                Date date = new Date(project.getPending());

                String id = StringUtils.generateCode(
                        8,
                        plugin.getFinishedProjectsRegistry().getIds(),
                        LOWER_CASE
                );

                plugin.getSqlManager().insert(
                        "finished_projects",
                        new SQLValuesSet(
                                new SQLValue(
                                        "finished_date", date
                                ),
                                new SQLValue(
                                        "id", id
                                ),
                                new SQLValue(
                                        "name", project.getDisplayName()
                                ),
                                new SQLValue(
                                        "country", project.getCountry().getName()
                                ),
                                new SQLValue(
                                        "cities", project.getCitiesResolved()
                                ),
                                new SQLValue(
                                        "type", project.getType().getName()
                                ),
                                new SQLValue(
                                        "points", project.getPoints()
                                ),
                                new SQLValue(
                                        "members", project.getMembers()
                                ),
                                new SQLValue(
                                        "owner", project.getOwner()
                                ),
                                new SQLValue(
                                        "tag", project.getTag()
                                ),
                                new SQLValue(
                                        "region_points", project.getRegion().getPoints()
                                ),
                                new SQLValue(
                                        "region", SQLPolygon.getFromPolygonRegion(project.getRegion())
                                )
                        )
                ).execute();

                plugin.getSqlManager().insert(
                        "tour_displays",
                        new SQLValuesSet(
                                new SQLValue(
                                        "date", date
                                ),
                                new SQLValue(
                                        "type", "finished_project"
                                ),
                                new SQLValue(
                                        "id", id
                                ),
                                new SQLValue(
                                        "cities", project.getCitiesResolved()
                                )
                        )
                ).execute();

                File source = new File(plugin.getDataFolder(), "projects/images/" + project.getId() + ".png");
                File target = new File(plugin.getDataFolder(), "projects/images/" + id + ".png");

                source.renameTo(target);

                for (City city : project.getCitiesResolved()) {
                    city.updateFinishedArea();
                }

                project.getCountry().getLogsChannel().sendMessage(
                        ":mag_right: **" + moderator.getName() + "** ha aceptado el proyecto `" + project.getId() + "`."
                ).queue();

                for (UUID uuid : project.getAllMembers()) {

                    ServerPlayer s = plugin.getPlayerRegistry().get(uuid);
                    ProjectManager manager = s.getProjectManager();

                    manager.removeProject(project);

                    double base = project.getPoints();
                    double boost = 0;
                    double futureBoosts = 0;

                    ResultSet pastProjectsSet = plugin.getSqlManager().select(
                            "finished_projects",
                            new SQLColumnSet("COUNT(id) AS total"),
                            new SQLANDConditionSet(
                                    new SQLORConditionSet(
                                            new SQLOperatorCondition(
                                                    "owner", "=", uuid
                                            ),
                                            new SQLJSONArrayCondition(
                                                    "members", uuid
                                            )
                                    ),
                                    new SQLOperatorCondition(
                                            "id", "!=", id
                                    ),
                                    new SQLBetweenCondition(
                                            "finished_date",
                                            new Date(project.getPending() - 604800000),
                                            new Date(project.getPending())
                                    )
                            )
                    ).retrieve();

                    pastProjectsSet.next();
                    double boostPercentage = 0.05 * pastProjectsSet.getInt("total");
                    boost += boostPercentage * project.getPoints();

                    ResultSet futureProjectsSet = plugin.getSqlManager().select(
                            "finished_projects",
                            new SQLColumnSet("points", "type", "country"),
                            new SQLANDConditionSet(
                                    new SQLORConditionSet(
                                            new SQLOperatorCondition(
                                                    "owner", "=", uuid
                                            ),
                                            new SQLJSONArrayCondition(
                                                    "members", uuid
                                            )
                                    ),
                                    new SQLOperatorCondition(
                                            "id", "!=", id
                                    ),
                                    new SQLBetweenCondition(
                                            "finished_date",
                                            new Date(project.getPending()),
                                            new Date(project.getPending() + 604800000)
                                    )
                            )
                    ).retrieve();

                    while (futureProjectsSet.next()) {

                        ProjectType type = plugin.getCountryManager().get(futureProjectsSet.getString("country")).getProjectType(futureProjectsSet.getString("type"));

                        manager.addPoints(type, futureProjectsSet.getInt("points") * 0.05);

                        futureBoosts += futureProjectsSet.getInt("points") * 0.05;
                    }

                    double total = base + boost + futureBoosts;

                    manager.addPoints(project.getType(), base + boost);
                    s.sendNotification(
                            project.getPrefix() + "Tu proyecto §a" + project.getDisplayName() + "§f ha sido aceptado.",
                            "**[PROYECTOS]** » Tu proyecto **" + project.getDisplayName() + "** ha sido aceptado."
                    );

                    DecimalFormat format = new DecimalFormat("#.##");

                    s.sendNotification(
                            project.getPrefix() + "Has obtenido §a" + (int) total + "§f puntos. §7(Proyecto: " + (int) base + " / Boost (x" + format.format(boostPercentage) + "): " + format.format(boost) + (futureBoosts > 0 ? " / Boost futuro: " + format.format(futureBoosts) : "") + ")",
                            "**[PROYECTOS]** » Has obtenido **" + (int) total + "** puntos. **(Proyecto: " + (int) base + " / Boost (x" + format.format(boostPercentage) + "): " + format.format(boost) + (futureBoosts > 0 ? " / Boost futuro: " + format.format(futureBoosts) : "") + ")**"
                    );

                    manager.addFinished(project);
                }

                ResultSet set = plugin.getSqlManager().select(
                        "posts",
                        new SQLColumnSet(
                                "channel_id"
                        ),
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "target_type", "=", "project"
                                ),
                                new SQLOperatorCondition(
                                        "target_id", "=", project.getId()
                                )
                        )
                ).retrieve();

                plugin.getSqlManager().delete(
                        "projects",
                        new SQLANDConditionSet(
                                new SQLOperatorCondition(
                                        "id", "=", project.getId()
                                )
                        )
                ).execute();

                plugin.getTerramapHandler().deletePolygon(project.getId());
                List<Coords2D> coords = new ArrayList<>();
                for (BlockVector2D vector2D : project.getRegion().getPoints()) {
                    coords.add(new Coords2D(plugin, vector2D));
                }
                plugin.getTerramapHandler().drawPolygon(coords, new Color(51, 60, 232), id);
                plugin.getRegionManager().removeRegion("project_" + project.getId());

                plugin.getProjectRegistry().unload(project.getId());
                plugin.getProjectRegistry().unregisterID(project.getId());
                plugin.getFinishedProjectsRegistry().registerID(id);

                break;
            }
        }

    }

}

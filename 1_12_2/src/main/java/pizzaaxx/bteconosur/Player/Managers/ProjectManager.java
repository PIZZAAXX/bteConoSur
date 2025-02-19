package pizzaaxx.bteconosur.Player.Managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.Projects.SQLSelectors.ProjectSQLSelector;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.*;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ProjectManager {

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;
    private final Set<Country> adminPermission;
    private final Set<String> ids;
    private final Map<Country, Map<ProjectType, Integer>> finished;
    private final Map<Country, Map<ProjectType, Double>> points;

    public ProjectManager(@NotNull BTEConoSur plugin, @NotNull ServerPlayer serverPlayer) throws SQLException, JsonProcessingException {
        this.plugin = plugin;
        this.serverPlayer = serverPlayer;

        ResultSet set = plugin.getSqlManager().select(
                "project_managers",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).retrieve();

        if (set.next()) {
            adminPermission = new HashSet<>();
            Set<String> adminRaw = plugin.getJSONMapper().readValue(set.getString("admin"), HashSet.class);
            for (String name : adminRaw) {
                adminPermission.add(plugin.getCountryManager().get(name));
            }

            ids = new HashSet<>();
            ResultSet projectsSet = plugin.getSqlManager().select(
                    "projects",
                    new SQLColumnSet(
                            "id"
                    ),
                    new SQLORConditionSet(
                            new SQLOperatorCondition(
                                    "owner", "=", serverPlayer.getUUID()
                            ),
                            new SQLJSONArrayCondition(
                                    "members", serverPlayer.getUUID()
                            )
                    )
            ).retrieve();

            while (projectsSet.next()) {
                ids.add(projectsSet.getString("id"));
            }

            finished = new HashMap<>();
            ResultSet finishedProjectsSet = plugin.getSqlManager().select(
                    "finished_projects",
                    new SQLColumnSet(
                            "country", "type"
                    ),
                    new SQLORConditionSet(
                            new SQLOperatorCondition(
                                    "owner", "=", serverPlayer.getUUID()
                            ),
                            new SQLJSONArrayCondition(
                                    "members", serverPlayer.getUUID()
                            )
                    )
            ).retrieve();

            while (finishedProjectsSet.next()) {
                Country country = plugin.getCountryManager().get(finishedProjectsSet.getString("country"));
                Map<ProjectType, Integer> projectsMap = finished.getOrDefault(country, new HashMap<>());
                ProjectType type = country.getProjectType(finishedProjectsSet.getString("type"));
                int amount = projectsMap.getOrDefault(type, 0);
                amount++;
                projectsMap.put(type, amount);
                finished.put(country, projectsMap);
            }

            points = new HashMap<>();
            Map<String, Object> pointsRaw = plugin.getJSONMapper().readValue(set.getString("points"), HashMap.class);
            for (Map.Entry<String, Object> entry : pointsRaw.entrySet()) {

                Country country = plugin.getCountryManager().get(entry.getKey());

                Map<String, Double> rawTypes = (Map<String, Double>) entry.getValue();
                Map<ProjectType, Double> types = new HashMap<>();
                for (String key : rawTypes.keySet()) {

                    types.put(
                          country.getProjectType(key),
                          rawTypes.get(key)
                    );

                }

                points.put(country, types);
            }
        } else {
            plugin.getSqlManager().insert(
                    "project_managers",
                    new SQLValuesSet(
                            new SQLValue(
                                    "uuid", serverPlayer.getUUID()
                            )
                    )
            ).execute();
            this.adminPermission = new HashSet<>();
            this.ids = new HashSet<>();
            this.points = new HashMap<>();
            this.finished = new HashMap<>();
        }
    }

    public Set<String> getAllProjectIDs() {
        return ids;
    }

    public Set<String> getProjects(ProjectSQLSelector... selectors) throws SQLException {
        if (ids.isEmpty()) {
            return new HashSet<>();
        }

        if (selectors.length == 0) {
            return ids;
        }

        SQLANDConditionSet conditionSet = new SQLANDConditionSet(
                new SQLContainedCondition<>(
                        "id",
                        ids,
                        true
                )
        );
        for (ProjectSQLSelector selector : selectors) {
            conditionSet.addCondition(selector.getCondition());
        }
        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "id"
                ),
                conditionSet
        ).retrieve();

        Set<String> result = new HashSet<>();
        while (set.next()) {
            result.add(set.getString("id"));
        }
        return result;
    }

    public int getFinishedProjects() {
        int counter = 0;
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            counter += this.getFinishedProjects(country);
        }
        return counter;
    }

    public int getFinishedProjects(Country country) {
        Map<ProjectType, Integer> countryMap = finished.getOrDefault(country, new HashMap<>());
        int counter = 0;
        for (int amount : countryMap.values()) {
            counter += amount;
        }
        return counter;
    }

    public int getFinishedProjects(Country country, ProjectType type) {
        Map<ProjectType, Integer> countryMap = finished.getOrDefault(country, new HashMap<>());
        return countryMap.getOrDefault(type, 0);
    }

    public double getPoints(Country country) {
        int result = 0;
        for (Double p : points.getOrDefault(country, new HashMap<>()).values()) {
            result += p;
        }
        return result;
    }

    public double getPoints(@NotNull ProjectType type) {

        if (!points.containsKey(type.getCountry())) {
            return 0;
        }

        return points.get(type.getCountry()).getOrDefault(type, 0.0);
    }

    public boolean hasAdminPermission(Country country) {
        return adminPermission.contains(country);
    }

    public void addPoints(@NotNull ProjectType type, double points) throws SQLException {
        double actual = this.getPoints(type);
        Map<ProjectType, Double> types = this.points.getOrDefault(type.getCountry(), new HashMap<>());
        types.put(type, actual + points);
        this.points.put(type.getCountry(), types);
        plugin.getSqlManager().update(
                "project_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "points", this.points
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
        plugin.getScoreboardHandler().update(serverPlayer);
        type.getCountry().getLogsChannel().sendMessage(
                ":chart_with_upwards_trend: Se han añadido **" + points + "** puntos a **" + serverPlayer.getName() + "**."
        ).queue();
    }

    public void addProject(@NotNull Project project) throws SQLException {
        this.ids.add(project.getId());
        plugin.getScoreboardHandler().update(serverPlayer);
    }

    public void removeProject(@NotNull Project project) throws SQLException {
        this.ids.remove(project.getId());
        plugin.getScoreboardHandler().update(serverPlayer);
    }

    public void addFinished(@NotNull Project project) throws SQLException {
        List<ProjectType> beforeTypes = new ArrayList<>();
        for (ProjectType type : project.getCountry().getProjectTypes()) {
            if (type.isUnlocked(this)) {
                beforeTypes.add(type);
            }
        }
        Map<ProjectType, Integer> countryMap = finished.getOrDefault(project.getCountry(), new HashMap<>());
        Integer amount = countryMap.getOrDefault(project.getType(), 0);
        amount++;
        countryMap.put(project.getType(), amount);
        finished.put(project.getCountry(), countryMap);
        for (ProjectType type : project.getCountry().getProjectTypes()) {
            if (type.isUnlocked(this) && !beforeTypes.contains(type)) {

                serverPlayer.sendNotification(
                        project.getPrefix() + "¡Has desbloqueado los proyectos de tipo §a" + type.getDisplayName() + "§f!",
                        "**[PROYECTOS]** » ¡Has desbloqueado los proyectos de tipo **" + type.getDisplayName() + "**!"
                );

            }
        }
        plugin.getScoreboardHandler().update(serverPlayer);
    }

    public void addFinished(Country country, ProjectType projectType) throws SQLException {
        Map<ProjectType, Integer> countryMap = finished.getOrDefault(country, new HashMap<>());
        Integer amount = countryMap.getOrDefault(projectType, 0);
        amount++;
        countryMap.put(projectType, amount);
        finished.put(country, countryMap);
        plugin.getScoreboardHandler().update(serverPlayer);
    }

}

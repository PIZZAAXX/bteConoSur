package pizzaaxx.bteconosur.Countries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.BuildEvents.BuildEvent;
import pizzaaxx.bteconosur.Player.Managers.ProjectManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLJSONContainsPathCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLNullCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.JSONParsable;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardDisplay;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression.Order.ASC;
import static pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression.Order.DESC;

public class Country implements JSONParsable, ScoreboardDisplay {

    private final BTEConoSur plugin;
    private final String name;
    private final String displayName;
    private final String abbreviation;
    private final String guildID;
    private final String showcaseID;
    private final String globalChatID;
    private final String countryChatID;
    private final String projectsForumChannelID;
    private final String logsID;
    private final String requestsID;
    private final String iconURL;
    private final Location spawnPoint;
    public final Set<String> cities;
    public final LinkedHashMap<String, ProjectType> projectTypes;
    public final String headValue;

    private final Emoji emoji;
    private final String chatPrefix;
    private final String tabPrefix;
    private String buildEventID;
    private final List<String> legacyDifficulties;

    public Country(@NotNull BTEConoSur plugin, @NotNull ResultSet set) throws SQLException, JsonProcessingException {
        this.plugin = plugin;
        this.name = set.getString("name");
        this.displayName = set.getString("display_name");
        this.abbreviation = set.getString("abbreviation");
        this.guildID = set.getString("guild_id");
        this.showcaseID = set.getString("showcase_id");
        this.globalChatID = set.getString("global_chat_id");
        this.countryChatID = set.getString("country_chat_id");
        this.projectsForumChannelID = set.getString("projects_forum_channel_id");
        this.logsID = set.getString("logs_id");
        this.requestsID = set.getString("requests_id");
        this.iconURL = set.getString("icon_url");
        Map<String, Integer> spawnJSON = plugin.getJSONMapper().readValue(set.getString("spawn_point"), Map.class);
        this.spawnPoint = new Location(plugin.getWorld(), spawnJSON.get("x"), spawnJSON.get("y"), spawnJSON.get("z"));

        this.cities = new HashSet<>();
        ResultSet citiesSet = plugin.getSqlManager().select(
                "cities",
                new SQLColumnSet(
                        "name"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "country", "=", this.name
                        )
                )
        ).retrieve();

        while (citiesSet.next()) {
            this.cities.add(citiesSet.getString("name"));
        }

        this.projectTypes = new LinkedHashMap<>();
        List<String> projectTypeIDs = plugin.getJSONMapper().readValue(set.getString("project_types"), ArrayList.class);
        for (String projectTypeID : projectTypeIDs) {
            this.projectTypes.put(projectTypeID, new ProjectType(plugin, this, projectTypeID));
        }
        this.headValue = set.getString("head_value");
        this.emoji = Emoji.fromFormatted(set.getString("emoji"));
        this.chatPrefix = set.getString("chat_prefix");
        this.tabPrefix = set.getString("tab_prefix");
        this.buildEventID = set.getString("current_build_event_id");

        this.legacyDifficulties = new ArrayList<>();
        JsonNode legacyNode = plugin.getJSONMapper().readTree(set.getString("legacy_difficulties"));
        for (JsonNode difficulty : legacyNode) {
            legacyDifficulties.add(difficulty.asText());
        }

    }

    public BTEConoSur getPlugin() {
        return plugin;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Guild getGuild() {
        return plugin.getBot().getGuildById(guildID);
    }

    public String getGuildID() {
        return guildID;
    }

    public TextChannel getShowcaseChannel() {
        return plugin.getBot().getTextChannelById(showcaseID);
    }

    public TextChannel getGlobalChatChannel() {
        return plugin.getBot().getTextChannelById(globalChatID);
    }

    public TextChannel getCountryChatChannel() {
        return plugin.getBot().getTextChannelById(countryChatID);
    }

    public TextChannel getLogsChannel() {
        return plugin.getBot().getTextChannelById(logsID);
    }

    public TextChannel getRequestsChannel() {
        return plugin.getBot().getTextChannelById(requestsID);
    }

    public String getIconURL() {
        return iconURL;
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public String getTabPrefix() {
        return tabPrefix;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public Set<String> getCities() {
        return cities;
    }

    public boolean isType(String type) {
        return projectTypes.containsKey(type);
    }

    public ProjectType getProjectType(String type) {
        return projectTypes.get(type);
    }

    public List<ProjectType> getProjectTypes() {
        return new ArrayList<>(projectTypes.values());
    }

    public String getHeadValue() {
        return headValue;
    }

    public Emoji getEmoji() {
        return emoji;
    }

    public void setBuildEventID(String id) throws SQLException {
        this.buildEventID = id;
        plugin.getSqlManager().update(
                "countries",
                new SQLValuesSet(
                        new SQLValue(
                                "current_build_event_id", id
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", this.name
                        )
                )
        ).execute();
    }

    public String getBuildEventID() {
        return buildEventID;
    }

    public BuildEvent getBuildEvent() {
        return plugin.getBuildEventsRegistry().get(buildEventID);
    }

    public List<String> getLegacyDifficulties() {
        return legacyDifficulties;
    }

    public List<ProjectType> getUnlockedProjectTypes(ProjectManager manager) {
        List<ProjectType> result = new ArrayList<>();
        for (ProjectType type : projectTypes.values()) {
            if (type.isUnlocked(manager)) {
                result.add(type);
            }
        }
        return result;
    }

    public String getShowcaseChannelID() {
        return showcaseID;
    }

    public Set<String> getPendingProjectsIDs() {
        Set<String> result = new HashSet<>();
        try {
            ResultSet set = plugin.getSqlManager().select(
                    "projects",
                    new SQLColumnSet(
                            "id"
                    ),
                    new SQLANDConditionSet(
                            new SQLNullCondition("pending", false),
                            new SQLOperatorCondition(
                                    "country", "=", this.name
                            )
                    ),
                    new SQLOrderSet(
                            new SQLOrderExpression(
                                    "pending", ASC
                            )
                    )
            ).retrieve();

            while (set.next()) {
                result.add(set.getString("id"));
            }
        } catch (SQLException ignored) {}
        return result;
    }

    @Override
    public String getJSON(boolean insideJSON) {
        return (insideJSON?"\"":"'") + this.name + (insideJSON?"\"":"'");
    }

    @Override
    public boolean equals(Object obj) {

        if (getClass() != obj.getClass()) {
            return false;
        }

        Country country = (Country) obj;
        return this.name.equals(country.name);
    }

    public void addCity(String name) {
        this.cities.add(name);
    }

    public void removeCity(String name) {
        this.cities.remove(name);
    }

    public String getProjectsForumChannelID() {
        return projectsForumChannelID;
    }

    public ForumChannel getProjectsForumChannel() {
        return plugin.getBot().getForumChannelById(projectsForumChannelID);
    }

    public List<UUID> getTopPlayers() throws SQLException, IOException {
        List<UUID> result = new ArrayList<>();
        ResultSet set = plugin.getSqlManager().select(
                "project_managers",
                new SQLColumnSet("uuid"),
                new SQLANDConditionSet(
                        new SQLJSONContainsPathCondition(
                                "points",
                                SQLJSONContainsPathCondition.Quantity.ONE,
                                "$." + this.name
                        )
                ),
                new SQLOrderSet(
                        new SQLOrderExpression(
                                "getcountrypoints(points, '" + this.name + "')", DESC
                        )
                )
        ).addText(" LIMIT 10").retrieve();

        while (set.next()) {
            result.add(plugin.getSqlManager().getUUID(set, "uuid"));
        }

        return result;
    }

    @Override
    public String getScoreboardTitle() {
        return "§a§lTop de " + displayName;
    }

    @Override
    public List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();

        int counter = 1;
        try {
            for (UUID uuid : this.getTopPlayers()) {

                ServerPlayer s = plugin.getPlayerRegistry().get(uuid);

                lines.add("§7#" + counter + "§a " + s.getProjectManager().getPoints(this) + "§f " + s.getName());

                counter++;
            }
        } catch (SQLException | IOException e) {
            return new ArrayList<>();
        }

        return lines;
    }

    @Override
    public String getScoreboardType() {
        return "top";
    }

    @Override
    public String getScoreboardID() {
        return "country_" + name;
    }
}

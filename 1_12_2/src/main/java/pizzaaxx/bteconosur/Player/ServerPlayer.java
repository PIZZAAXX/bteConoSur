package pizzaaxx.bteconosur.Player;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.PrefixHolder;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.Managers.*;
import pizzaaxx.bteconosur.Projects.RegionSelectors.MemberProjectSelector;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.JSONParsable;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardDisplay;
import pizzaaxx.bteconosur.Tablist.TablistPrefixHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ServerPlayer implements ScoreboardDisplay {

    private final BTEConoSur plugin;
    private final UUID uuid;
    private final String name;
    private final ChatManager chatManager;
    private final WorldEditManager worldEditManager;
    private final DiscordManager discordManager;
    private final MiscManager miscManager;
    private final ProjectManager projectManager;
    private final ScoreboardManager scoreboardManager;
    private final List<SecondaryRoles> secondaryRoles;

    @Override
    public String getScoreboardTitle() {
        return "§a§l" + name;
    }

    @Override
    public List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();

        lines.add("§aRango: §f" + StringUtils.capitalize(this.getBuilderRank().toString().toLowerCase()));

        if (!this.getSecondaryRoles().isEmpty()) {
            lines.add("§aRoles: ");
            for (SecondaryRoles role : this.getSecondaryRoles()) {
                lines.add("§7• §f" + role);
            }
        }

        if (discordManager.isLinked()) {
            lines.add("§aDiscord: §f" + discordManager.getName() + discordManager.getDiscriminator());
        }
        try {
            lines.add("§aChat: §f" + chatManager.getCurrentChat().getDisplayName());
        } catch (SQLException e) {
            return new ArrayList<>();
        }

        lines.add(" ");

        boolean wrotePoints = false;
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            double points = projectManager.getPoints(country);
            if (points > 0) {
                if (!wrotePoints) {
                    lines.add("§aPuntos: ");
                    wrotePoints = true;
                }

                lines.add("§7• §f" + country.getDisplayName() + ": §7" + points);
            }
        }

        return lines;
    }

    @Override
    public String getScoreboardType() {
        return "me";
    }

    @Override
    public String getScoreboardID() {
        return "player_" + uuid.toString();
    }

    public enum BuilderRank implements PrefixHolder, TablistPrefixHolder {
        VISITA("§f[VISITA§f] §r", "[:flag_white:] ", "§f[VIS§f]", 6),
        POSTULANTE("§f[§7POSTULANTE§f] §r", "[:books:] ", "§f[§7POS§f]", 5),
        BUILDER("§f[§9BUILDER§f] §r", "[:hammer_pick:] ", "§f[§9BUI§f]", 4);

        private final String prefix;
        private final String discordPrefix;
        private final String tablistPrefix;
        private final int priority;

        BuilderRank(String prefix, String discordPrefix, String tablistPrefix, int priority) {
            this.prefix = prefix;
            this.discordPrefix = discordPrefix;
            this.tablistPrefix = tablistPrefix;
            this.priority = priority;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getDiscordPrefix() {
            return discordPrefix;
        }

        @Override
        public String getTablistPrefix() {
            return tablistPrefix;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    public enum SecondaryRoles implements PrefixHolder, TablistPrefixHolder, JSONParsable {
        ADMIN("§f[§cADMIN§f] §r", "[:crown:] ", "§6§l", "§f[§cADM§f]", 0),
        MOD("§f[§5MOD§f] §r", "[:shield:] ", "§5§l", "§f[§5MOD§f]", 1),
        STREAMER("§f[§aSTREAMER§f] §r", "[:video_game:] ", null, "§f[§aSTR§f]", 2),
        DONADOR("§f[§dDONADOR§f] §r", "[:gem:] ", null, "§f[§dDON§f]", 3);

        private final String prefix;
        private final String discordPrefix;
        private final String chatColor;
        private final String tablistPrefix;
        private final int priority;

        SecondaryRoles(String prefix, String discordPrefix, String chatColor, String tablistPrefix, int priority) {
            this.prefix = prefix;
            this.discordPrefix = discordPrefix;
            this.chatColor = chatColor;
            this.tablistPrefix = tablistPrefix;
            this.priority = priority;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getDiscordPrefix() {
            return discordPrefix;
        }

        public String getChatColor() {
            return chatColor;
        }


        @NotNull
        @Override
        public String toString() {
            return StringUtils.capitalize(super.toString().toLowerCase());
        }

        @Override
        public String getTablistPrefix() {
            return tablistPrefix;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @NotNull
        @Override
        public String getJSON(boolean insideJSON) {
            return (insideJSON ? "\"" : "") + this.toString().toLowerCase() + (insideJSON ? "\"" : "");
        }
    }

    public ServerPlayer(@NotNull BTEConoSur plugin, UUID uuid) throws SQLException, JsonProcessingException {

        ResultSet set = plugin.getSqlManager().select(
                "players",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", uuid
                        )
                )
        ).retrieve();

        if (set.next()) {

            this.plugin = plugin;
            this.uuid = uuid;
            this.name = set.getString("name");
            this.secondaryRoles = new ArrayList<>();
            List<String> rawRoles = plugin.getJSONMapper().readValue(set.getString("roles"), ArrayList.class);
            for (String role : rawRoles) {
                secondaryRoles.add(SecondaryRoles.valueOf(role.toUpperCase()));
            }
            Collections.sort(secondaryRoles);

            this.chatManager = new ChatManager(plugin, this);
            this.worldEditManager = new WorldEditManager(plugin, this);
            this.discordManager = new DiscordManager(plugin, this);
            this.miscManager = new MiscManager(plugin, this, set);
            this.projectManager = new ProjectManager(plugin, this);
            this.scoreboardManager = new ScoreboardManager(this);

        } else {
            plugin.error("Missing player data: " + uuid);
            throw new SQLException();
        }

    }

    public TablistPrefixHolder getTablistPrefixHolder() {
        if (secondaryRoles.isEmpty()) {
            return this.getBuilderRank();
        }
        return secondaryRoles.get(0);
    }

    public int getPriority() {
        return this.getTablistPrefixHolder().getPriority();
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public WorldEditManager getWorldEditManager() {
        return worldEditManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public MiscManager getMiscManager() {
        return miscManager;
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public BTEConoSur getPlugin() {
        return plugin;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean isBuilder() {
        return true;
    }

    public boolean canBuild(Location loc) {
        return plugin.getProjectRegistry().getProjectsAt(loc, new MemberProjectSelector(uuid)).size() > 0;
    }

    public List<String> getLore(boolean name) {
        List<String> lore = new ArrayList<>();
        if (name) {
            lore.add("§a§l" + this.name);
            lore.add("§r ");
        }
        if (discordManager.isLinked()) {
            lore.add("§aDiscord: §f" + discordManager.getName() + discordManager.getDiscriminator());
        } else {
            lore.add("§aDiscord: §fN/A");
        }

        return lore;
    }

    public void sendNotification(
            String minecraftMessage,
            String discordMessage
    ) {
        plugin.getNotificationsService().sendNotification(this.uuid, minecraftMessage, discordMessage);
    }

    public BuilderRank getBuilderRank() {
        if (projectManager.getFinishedProjects() > 0) {
            return BuilderRank.BUILDER;
        } else if (projectManager.getAllProjectIDs().size() > 0) {
            return BuilderRank.POSTULANTE;
        } else {
            return BuilderRank.VISITA;
        }
    }

    public long getLastDisconnected() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "players",
                new SQLColumnSet("last_disconnected"),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", uuid
                        )
                )
        ).retrieve();

        if (set.next()) {
            return set.getLong("last_disconnected");
        } else {
            throw new SQLException();
        }
    }

    public List<SecondaryRoles> getSecondaryRoles() {
        return secondaryRoles;
    }

    public void addSecondaryRole(SecondaryRoles role) throws SQLException {
        secondaryRoles.add(role);

        plugin.getSqlManager().update(
                "players",
                new SQLValuesSet(
                        new SQLValue(
                                "roles", secondaryRoles
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", uuid
                        )
                )
        ).execute();

        plugin.getScoreboardHandler().update(this);
    }

    public void removeSecondaryRole(SecondaryRoles role) throws SQLException {
        secondaryRoles.remove(role);

        plugin.getSqlManager().update(
                "players",
                new SQLValuesSet(
                        new SQLValue(
                                "roles", secondaryRoles
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", uuid
                        )
                )
        ).execute();

        plugin.getScoreboardHandler().update(this);
    }
}

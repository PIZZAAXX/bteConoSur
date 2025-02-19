package pizzaaxx.bteconosur.Projects;

import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Point64;
import com.monst.polylabel.PolyLabel;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.PrefixHolder;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Discord.Showcase.ShowcaseContainer;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Actions.*;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.JSONParsable;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardDisplay;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class Project implements JSONParsable, Prefixable, ProjectWrapper, ScoreboardDisplay, ShowcaseContainer {

    public static int MAX_PROJECTS_PER_PLAYER = 15;

    private final BTEConoSur plugin;

    private final String id;
    private String displayName;
    private final Country country;
    private Set<String> cities;
    private Long pending;
    private ProjectType type;
    private int points;
    public Set<UUID> members;
    public UUID owner;
    private ProjectTag tag;
    private ProtectedPolygonalRegion region;
    private final Set<UUID> requests;

    public Project(@NotNull BTEConoSur plugin, String id) throws SQLException, IOException {
        this.plugin = plugin;
        this.id = id;
        this.region = (ProtectedPolygonalRegion) plugin.getRegionManager().getRegion("project_" + id);

        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", id
                        )
                )
        ).retrieve();

        if (set.next()) {

            this.displayName = set.getString("name");

            this.country = plugin.getCountryManager().get(set.getString("country"));

            this.cities = plugin.getJSONMapper().readValue(set.getString("cities"), HashSet.class);

            Timestamp timestamp = set.getTimestamp("pending");

            this.pending = (timestamp == null ? null : timestamp.getTime());

            this.type = country.getProjectType(set.getString("type"));

            this.points = set.getInt("points");

            this.members = new HashSet<>();
            Set<String> uuids = plugin.getJSONMapper().readValue(set.getString("members"), HashSet.class);
            for (String uuid : uuids) {
                members.add(UUID.fromString(uuid));
            }

            this.owner = plugin.getSqlManager().getUUID(set, "owner");

            String tag = set.getString("tag");

            this.tag = tag == null ? null : ProjectTag.valueOf(tag);

            ResultSet requestsSet = plugin.getSqlManager().select(
                    "project_join_requests",
                    new SQLColumnSet(
                            "target"
                    ),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "project_id", "=", this.id
                            )
                    )
            ).retrieve();

            requests = new HashSet<>();

            while (set.next()) {
                requests.add(plugin.getSqlManager().getUUID(requestsSet, "target"));
            }

        } else {
            throw new IllegalArgumentException();
        }
    }

    public BTEConoSur getPlugin() {
        return plugin;
    }

    public boolean hasCustomName() {
        return displayName != null;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        if (displayName ==  null) {
            return id.toUpperCase();
        }
        return displayName;
    }

    public String getName() {
        return displayName;
    }

    @Override
    public String getOptionName() {
        return "Proyecto " + this.getDisplayName();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public String getOptionDescription() {
        String citiesString = cities.stream().map(name -> plugin.getCityManager().get(name)).map(City::getDisplayName).collect(Collectors.joining(", "));
        return id + (cities.isEmpty() ? "" : " • " + citiesString.substring(0, Math.min(citiesString.length(), 91)));
    }



    public Country getCountry() {
        return country;
    }

    public Set<String> getCities() {
        return cities;
    }

    public Set<City> getCitiesResolved() {
        Set<City> cities = new HashSet<>();
        for (String cityName : this.cities) {
            cities.add(plugin.getCityManager().get(cityName));
        }
        return cities;
    }

    public boolean isPending() {
        return pending != null;
    }

    public Long getPending() {
        return pending;
    }

    public ProjectType getType() {
        return type;
    }

    public int getPoints() {
        return points;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getAllMembers() {
        Set<UUID> result = new HashSet<>(members);
        result.add(owner);
        return result;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isClaimed() {
        return owner != null;
    }

    public boolean isMember(UUID uuid) {
        return (owner.equals(uuid) || members.contains(uuid));
    }

    public boolean isFull() {
        return members.size() >= type.getMaxMembers();
    }

    public ProjectTag getTag() {
        return tag;
    }

    public ProtectedPolygonalRegion getRegion() {
        return region;
    }

    public double getTotalArea() {
        Path64 polygon = new Path64();

        List<BlockVector2D> vectors = region.getPoints();

        vectors.forEach(vector -> {
            Point64 point = new Point64();
            point.x = (long) vector.getX();
            point.y = (long) vector.getZ();
            polygon.add(point);
        });

        BlockVector2D vector = vectors.get(0);
        Point64 point = new Point64();
        point.x = (long) vector.getX();
        point.y = (long) vector.getZ();
        polygon.add(point);

        return Clipper.Area(polygon);
    }

    public Location getTeleportLocation() {

        Double[][][] points = new Double[1][region.getPoints().size()][2];

        int counter = 0;
        for (BlockVector2D vector : region.getPoints()) {
            Double[] coords = new Double[] {vector.getX(), vector.getZ()};
            points[0][counter] = coords;
            counter++;
        }

        PolyLabel.Result result = PolyLabel.polyLabel(points, 1);

        return plugin.getWorld().getHighestBlockAt((int) Math.floor(result.getX()), (int) Math.floor(result.getY())).getLocation();
    }

    public enum ProjectLoreField {

        ID, ID_UPPERCASE, DISPLAY_NAME, OWNER, MEMBER_IGNORE, MEMBER, MEMBER_COUNT, CITIES, COUNTRY, PENDING, TYPE, POINTS, TELEPORT, TAG, TAG_IGNORE

    }

    public List<String> getLore(boolean includeName, ProjectLoreField... fields) {
        return this.getLore(includeName, "§f• %f: §7%v", fields);
    }

    public List<String> getLore(boolean includeName, String format, ProjectLoreField... fields) {

        List<String> lore = new ArrayList<>();

        if (includeName) {
            lore.add("§a§l" + this.getDisplayName());
        }

        for (ProjectLoreField field : fields) {

            switch (field) {
                case ID: {
                    lore.add(
                            format.replace("%f", "ID").replace("%v", this.id)
                    );
                    break;
                }
                case ID_UPPERCASE: {
                    lore.add(
                            format.replace("%f", "ID").replace("%v", this.id.toUpperCase())
                    );
                    break;
                }
                case DISPLAY_NAME: {
                    lore.add(
                            format.replace("%f", "Nombre").replace("%v", this.getDisplayName())
                    );
                    break;
                }
                case OWNER: {
                    if (this.owner != null) {
                        lore.add(
                                format.replace("%f", "Líder").replace("%v", plugin.getPlayerRegistry().get(this.owner).getName())
                        );
                    }
                    break;
                }
                case MEMBER: {
                    if (members.isEmpty()) {
                        lore.add(
                                format.replace("%f", "Miembros").replace("%v", "Sin miembros")
                        );
                    } else {
                        lore.add(
                                format.replace("%f", "Miembros").replace("%v", String.join(", ", plugin.getPlayerRegistry().getNames(this.members)))
                        );
                    }
                    break;
                }
                case MEMBER_IGNORE: {
                    if (!members.isEmpty()) {
                        lore.add(
                                format.replace("%f", "Miembros").replace("%v", String.join(", ", plugin.getPlayerRegistry().getNames(this.members)))
                        );
                    }
                    break;
                }
                case MEMBER_COUNT: {
                    lore.add(
                            format.replace("%f", "Miembros").replace("%v", Integer.toString(members.size()))
                    );
                    break;
                }
                case CITIES: {
                    lore.add(
                            format.replace("%f", "Ciudad(es)").replace("%v", (this.cities.isEmpty() ? "Ninguna" : this.getCitiesResolved().stream().map(City::getDisplayName).collect(Collectors.joining(", "))))
                    );
                    break;
                }
                case COUNTRY: {
                    lore.add(
                            format.replace("%f", "País").replace("%v", this.country.getDisplayName())
                    );
                    break;
                }
                case PENDING: {
                    lore.add(
                            format.replace("%f", "Pendiente").replace("%v", (pending != null ? "Si" : "No"))
                    );
                    break;
                }
                case TYPE: {
                    lore.add(
                            format.replace("%f", "Tipo").replace("%v", this.type.getDisplayName())
                    );
                    break;
                }
                case POINTS: {
                    lore.add(
                            format.replace("%f", "Puntos").replace("%v", Integer.toString(this.points))
                    );
                    break;
                }
                case TELEPORT: {
                    Location loc = this.getTeleportLocation();
                    lore.add(
                            format.replace("%f", "Coordenadas").replace("%v", loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ())
                    );
                    break;
                }
                case TAG: {
                    lore.add(
                            format.replace("%f", "Etiqueta").replace("%v", (tag != null ? StringUtils.capitalize(this.tag.toString()) : "Ninguna"))
                    );
                    break;
                }
                case TAG_IGNORE: {
                    if (tag != null) {
                        lore.add(
                                format.replace("%f", "Etiqueta").replace("%v", StringUtils.capitalize(this.tag.toString()))
                        );
                    }
                    break;
                }
            }

        }

        return lore;

    }

    public ItemStack getItem() {
        List<String> lore = this.getLore(
                false,
                ProjectLoreField.DISPLAY_NAME,
                ProjectLoreField.OWNER,
                ProjectLoreField.MEMBER_IGNORE,
                ProjectLoreField.COUNTRY,
                ProjectLoreField.CITIES,
                ProjectLoreField.TYPE,
                ProjectLoreField.POINTS,
                ProjectLoreField.TAG_IGNORE
        );
        if (tag != null) {
            return ItemBuilder.head(
                    tag.getHeadValue(),
                    "§aProyecto " + this.getDisplayName(),
                    lore
            );
        } else {
            return ItemBuilder.of(Material.MAP)
                    .name("§aProyecto " + this.getDisplayName())
                    .lore(lore)
                    .build();
        }
    }

    @Override
    public String getJSON(boolean insideJSON) {
        return (insideJSON?"\"":"'") + this.id + (insideJSON?"\"":"'");
    }

    @Override
    public String getPrefix() {
        return "§f[§dPROYECTO§f] §7>> §f";
    }

    @Override
    public List<BlockVector2D> getRegionPoints() {
        return region.getPoints();
    }

    public enum ProjectRole implements PrefixHolder {
        LEADER("§f[§6LÍDER§f] §r", ""),
        MEMBER("§f[§9MIEMBRO§f] §r", ""),
        GUEST("§f[§7INVITADO§f] §r", "");

        private final String prefix;
        private final String discordPrefix;

        ProjectRole(String prefix, String discordPrefix) {
            this.prefix = prefix;
            this.discordPrefix = discordPrefix;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getDiscordPrefix() {
            return discordPrefix;
        }
    }

    public ProjectRole getProjectRole(UUID uuid) {
        if (owner.equals(uuid)) {
            return ProjectRole.LEADER;
        } else if (members.contains(uuid)) {
            return ProjectRole.MEMBER;
        } else {
            return ProjectRole.GUEST;
        }
    }

    public Set<UUID> getRequests() {
        return requests;
    }

    public boolean request(UUID target) throws SQLException {
        if (!requests.contains(target)) {
            requests.add(target);
            plugin.getSqlManager().insert(
                    "project_join_requests",
                    new SQLValuesSet(
                            new SQLValue(
                                    "project_id", this.id
                            ),
                            new SQLValue(
                                    "target", target
                            )
                    )
            ).execute();
            ServerPlayer targetPlayer = plugin.getPlayerRegistry().get(target);
            plugin.getPlayerRegistry().get(owner).sendNotification(
                    getPrefix() + "§a" + targetPlayer.getName() + "§f ha solicitado unirse a tu proyecto §a" + this.getDisplayName() + "§f.",
                    "**[PROYECTOS]** » **" + targetPlayer.getName() + "** ha solicitado unirse a tu proyecto **" + this.getDisplayName() + "**."
            );
            return true;
        }
        return false;
    }

    public void deleteRequest(UUID target) throws SQLException {
        if (requests.remove(target)) {
            plugin.getSqlManager().delete(
                    "project_join_requests",
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "target", "=", target
                            ),
                            new SQLOperatorCondition(
                                    "project_id", "=", id
                            )
                    )
            ).execute();
        }
    }

    public boolean setTag(@Nullable ProjectTag tag) throws SQLException {
        if (tag != this.tag) {
            this.tag = tag;
            plugin.getSqlManager().update(
                    "projects",
                    new SQLValuesSet(
                            new SQLValue(
                                    "tag", tag
                            )
                    ),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "id", "=", this.id
                            )
                    )
            ).execute();
            return true;
        }
        return false;
    }

    public void update() throws SQLException, IOException {
        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "name",
                        "pending",
                        "type",
                        "points",
                        "owner",
                        "tag",
                        "members",
                        "cities"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", id
                        )
                )
        ).retrieve();

        if (set.next()) {

            this.displayName = set.getString("name");

            Timestamp timestamp = set.getTimestamp("pending");

            this.pending = (timestamp == null ? null : timestamp.getTime());

            this.type = country.getProjectType(set.getString("type"));
            this.points = set.getInt("points");
            this.owner = plugin.getSqlManager().getUUID(set, "owner");
            String tag = set.getString("tag");
            this.tag = tag == null ? null : ProjectTag.valueOf(tag);
            this.members = new HashSet<>();
            Set<String> uuids = plugin.getJSONMapper().readValue(set.getString("members"), HashSet.class);
            for (String uuid : uuids) {
                members.add(UUID.fromString(uuid));
            }
            this.cities = plugin.getJSONMapper().readValue(set.getString("cities"), HashSet.class);

            this.region = (ProtectedPolygonalRegion) plugin.getRegionManager().getRegion("project_" + id);

            plugin.getScoreboardHandler().update(this);

        } else {
            throw new IllegalArgumentException();
        }
    }

    public AddMemberProjectAction addMember(UUID uuid) {
        return new AddMemberProjectAction(plugin, this, uuid);
    }

    @CheckReturnValue
    public ClaimProjectAction claim(UUID owner) {
        return new ClaimProjectAction(plugin, this, owner);
    }

    public RemoveMemberProjectAction removeMember(UUID uuid) {
        return new RemoveMemberProjectAction(plugin, this, uuid);
    }

    public SetDisplayNameProjectAction setDisplayName(String name) {
        return new SetDisplayNameProjectAction(plugin, this, name);
    }

    public SetPendingProjectAction setPending(boolean pending) {
        return new SetPendingProjectAction(plugin, this, pending);
    }

    public TransferProjectAction transfer(UUID uuid) {
        return new TransferProjectAction(plugin, this, uuid);
    }

    public MemberLeaveProjectAction memberLeave(UUID uuid) {
        return new MemberLeaveProjectAction(plugin, this, uuid);
    }

    public EmptyProjectAction emptyProject() {
        return new EmptyProjectAction(plugin, this);
    }

    public ReviewProjectAction review(ReviewProjectAction.ReviewAction reviewAction, UUID moderator) {
        return new ReviewProjectAction(
                plugin,
                reviewAction,
                this,
                moderator
        );
    }

    @Override
    public String getScoreboardTitle() {
        return "§a§l" + this.getDisplayName();
    }

    @Override
    public List<String> getScoreboardLines() {
        return this.getLore(
                false,
                "§f%f: §7%v",
                ProjectLoreField.COUNTRY, ProjectLoreField.TYPE, ProjectLoreField.POINTS, ProjectLoreField.OWNER, ProjectLoreField.MEMBER_COUNT, ProjectLoreField.TAG_IGNORE
        );
    }

    @Override
    public String getScoreboardType() {
        return "project";
    }

    @Override
    public String getScoreboardID() {
        return "project_" + id;
    }

    @Override
    public boolean equals(Object obj) {

        if (getClass() != obj.getClass()) {
            return false;
        }

        Project project = (Project) obj;
        return this.id.equals(project.id);
    }
}

package pizzaaxx.bteconosur.BuildEvents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Commands.TourCommand;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Discord.Showcase.ShowcaseContainer;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Entities.SQLPolygon;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.CoordinatesUtils;
import pizzaaxx.bteconosur.Utils.RegionUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;

import static pizzaaxx.bteconosur.BuildEvents.BuildEvent.Status.*;
import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class BuildEvent implements TourCommand.TourDisplay, ShowcaseContainer {

    @Override
    public String getOptionName() {
        return "Evento " + name;
    }

    @Nullable
    @Override
    public String getOptionDescription() {
        return null;
    }

    @Override
    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    @Override
    public Set<String> getCities() {
        return new HashSet<>();
    }

    public enum Status {
        EDITED, POSTED, ACTIVE, FINISHED
    }

    private final BTEConoSur plugin;
    private final String id;
    private final Country country;
    private Status status;
    private String name;
    private String description;
    private final Set<UUID> members;
    private ProjectType minimumType;
    private int pointsGiven;
    private final File image;
    private ProtectedRegion region;
    private Location spawnPoint;
    private Long start;
    private Long end;
    private String postChannelID;
    private String postMessageID;
    private String scheduledEventID;

    public BuildEvent(@NotNull BTEConoSur plugin, String id) throws SQLException, JsonProcessingException {
        ResultSet set = plugin.getSqlManager().select(
                "build_events",
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

            this.plugin = plugin;
            this.id = id;
            this.country = plugin.getCountryManager().get(set.getString("country"));
            status = Status.valueOf(set.getString("status"));
            this.name = set.getString("name");
            this.description = set.getString("description");
            this.members = new HashSet<>();
            Set<String> rawUUIDs = plugin.getJSONMapper().readValue(set.getString("members"), HashSet.class);
            for (String rawUUID : rawUUIDs) {
                members.add(UUID.fromString(rawUUID));
            }
            String typeString = set.getString("minimum_type");
            this.minimumType = (typeString != null ? country.getProjectType(typeString) : null);
            this.pointsGiven = set.getInt("points_given");
            this.image = new File(plugin.getDataFolder(), "buildEvents/" + id + ".png");
            if (status == FINISHED) {
                List<BlockVector2D> regionPoints = new ArrayList<>();
                List<Object> rawCoords = plugin.getJSONMapper().readValue(set.getString("region_points"), ArrayList.class);
                for (Object obj : rawCoords) {
                    Map<String, Double> coords = (Map<String, Double>) obj;
                    regionPoints.add(new BlockVector2D(coords.get("x"), coords.get("z")));
                }
                this.region = new ProtectedPolygonalRegion(
                        "event_" + id,
                        regionPoints,
                        -100, 8000
                );
            } else {
                this.region = plugin.getRegionManager().getRegion("event_" + id);
            }
            String spawnString = set.getString("spawn_point");
            if (spawnString != null) {
                Map<String, Double> spawnPointCoordinates = plugin.getJSONMapper().readValue(spawnString, HashMap.class);
                this.spawnPoint = new Location(plugin.getWorld(), spawnPointCoordinates.get("x"), spawnPointCoordinates.get("y"), spawnPointCoordinates.get("z"));
            } else {
                this.spawnPoint = null;
            }
            this.start = (set.getTimestamp("start") == null ? null : set.getTimestamp("start").getTime());
            this.end = (set.getTimestamp("end") == null ? null : set.getTimestamp("end").getTime());
            this.postChannelID = set.getString("post_channel_id");
            this.postMessageID = set.getString("post_message_id");
            this.scheduledEventID = set.getString("scheduled_event_id");

        } else {
            throw new IllegalArgumentException();
        }
    }

    public String getId() {
        return id;
    }

    public Country getCountry() {
        return country;
    }

    public Status getStatus() {
        return status;
    }

    private void setStatus(Status status) throws SQLException {
        this.update("status", status);
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws SQLException {
        this.update("name", name);
        this.name = name;
        if (status == POSTED) {
            this.getScheduledEvent().queue(
                    scheduledEvent -> scheduledEvent.getManager().setName(name).queue()
            );
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) throws SQLException {
        this.update("description", description);
        this.description = description;
        if (status == POSTED) {
            this.getScheduledEvent().queue(
                    scheduledEvent -> scheduledEvent.getManager().setDescription(description).queue()
            );
        }
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID member) throws SQLException {
        this.members.add(member);
        this.update("members", this.members);
        plugin.getSqlManager().update(
                "posts",
                new SQLValuesSet(
                        new SQLValue("members", members)
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "target_type", "=", "event"
                        ),
                        new SQLOperatorCondition(
                                "target_id", "=", id
                        )
                )
        ).execute();
        if (status == Status.ACTIVE) {
            DefaultDomain domain = region.getMembers();
            domain.addPlayer(member);
            plugin.getRegionManager().addRegion(region);
            this.getPostMessage().queue(
                    message -> {
                        MessageEmbed embed = message.getEmbeds().get(0);
                        EmbedBuilder builder = new EmbedBuilder(embed);
                        List<MessageEmbed.Field> fields = builder.getFields();
                        fields.remove(fields.size() - 1);
                        fields.add(
                                new MessageEmbed.Field(
                                        ":busts_in_silhouette: Miembros:",
                                        (members.isEmpty() ? "Ninguno." : String.join(", ", plugin.getPlayerRegistry().getNames(members))),
                                        false
                                )
                        );
                        message.editMessageEmbeds(builder.build()).queue();
                    }
            );
        }
    }

    public void removeMember(UUID member) throws SQLException {
        this.members.remove(member);
        this.update("members", this.members);
        plugin.getSqlManager().update(
                "posts",
                new SQLValuesSet(
                        new SQLValue("members", members)
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "target_type", "=", "event"
                        ),
                        new SQLOperatorCondition(
                                "target_id", "=", id
                        )
                )
        ).execute();
        if (status == Status.ACTIVE) {
            DefaultDomain domain = region.getMembers();
            domain.removePlayer(member);
            plugin.getRegionManager().addRegion(region);
            this.getPostMessage().queue(
                    message -> {
                        MessageEmbed embed = message.getEmbeds().get(0);
                        EmbedBuilder builder = new EmbedBuilder(embed);
                        List<MessageEmbed.Field> fields = builder.getFields();
                        fields.remove(fields.size() - 1);
                        fields.add(
                                new MessageEmbed.Field(
                                        ":busts_in_silhouette: Miembros:",
                                        (members.isEmpty() ? "Ninguno." : String.join(", ", plugin.getPlayerRegistry().getNames(members))),
                                        false
                                )
                        );
                        message.editMessageEmbeds(builder.build()).queue();
                    }
            );
        }
    }

    public ProjectType getMinimumType() {
        return minimumType;
    }

    public void setMinimumType(ProjectType type) throws SQLException {
        this.update("minimum_type", type);
        this.minimumType = type;
    }

    public int getPointsGiven() {
        return pointsGiven;
    }

    public void setPointsGiven(int pointsGiven) throws SQLException {
        this.update("points_given", pointsGiven);
        this.pointsGiven = pointsGiven;
    }

    public File getImage() {
        return image;
    }

    public void setImage(@Nullable InputStream is) throws IOException {
        if (is == null) {
            image.delete();
            if (status == POSTED) {
                this.getScheduledEvent().queue(
                        scheduledEvent -> scheduledEvent.getManager().setImage(null).queue()
                );
            }
            return;
        }
        FileUtils.copyInputStreamToFile(is, image);
        if (status == POSTED) {
            this.getScheduledEvent().queue(
                    scheduledEvent -> {
                        try {
                            scheduledEvent.getManager().setImage(Icon.from(is)).queue();
                        } catch (IOException e) {e.printStackTrace();}
                    }
            );
        }
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public void setRegion(List<BlockVector2D> points) throws SQLException {
        this.update("region_points", points);
        this.update("region", SQLPolygon.getFromVectors(points));
        ProtectedPolygonalRegion r = new ProtectedPolygonalRegion(
                "event_" + id,
                points,
                -100, 8000
        );
        plugin.getRegionManager().addRegion(r);
        this.region = r;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(Location loc) throws SQLException {
        this.update("spawn_point", loc);
        this.spawnPoint = loc;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Date start) throws SQLException {
        this.update("start", start);
        this.start = start.getTime();
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Date end) throws SQLException {
        this.update("end", end);
        this.end = end.getTime();
    }

    public void setPostChannelID(String postChannelID) {
        this.postChannelID = postChannelID;
    }

    public ThreadChannel getPostChannel() {
        return country.getGuild().getThreadChannelById(postChannelID);
    }

    public String getPostMessageID() {
        return postMessageID;
    }

    public RestAction<Message> getPostMessage() {
        return this.getPostChannel().retrieveMessageById(postMessageID);
    }

    public String getScheduledEventID() {
        return scheduledEventID;
    }

    public CacheRestAction<ScheduledEvent> getScheduledEvent() {
        return country.getGuild().retrieveScheduledEventById(scheduledEventID);
    }

    private void update(String key, Object value) throws SQLException {
        plugin.getSqlManager().update(
                "build_events",
                new SQLValuesSet(
                        new SQLValue(
                                key, value
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", this.id
                        )
                )
        ).execute();
    }

    public EmbedBuilder getEmbed() {
        EmbedBuilder builder = new EmbedBuilder();
        if (status == POSTED || status == EDITED) {
            builder.setColor(Color.ORANGE);
        } else if (status == Status.ACTIVE) {
            builder.setColor(Color.GREEN);
        }
        builder.setTitle("Evento " + name);
        builder.setThumbnail(country.getIconURL());
        builder.setDescription(description);

        builder.addField(
                ":game_die: Mínimo tipo desbloqueado:",
                (minimumType == null ? "Ninguno." : minimumType.getDisplayName()),
                true
        );
        builder.addField(
                ":chart_with_upwards_trend: Puntos otorgados:",
                Integer.toString(pointsGiven),
                true
        );
        builder.addBlankField(false);
        builder.addField(
                ":inbox_tray: Inicio:",
                "<t:" + (start / 1000) + ":F>",
                true
        );
        builder.addField(
                ":outbox_tray: Término:",
                "<t:" + (end / 1000) + ":F>",
                true
        );
        if (status == Status.ACTIVE) {
            builder.addBlankField(false);
            builder.addField(
                    ":busts_in_silhouette: Miembros:",
                    String.join(", ", plugin.getPlayerRegistry().getNames(members)),
                    false
            );
        }
        return builder;
    }

    public boolean canBePosted() {
        return name != null && description != null && image.exists() && region != null && spawnPoint != null && start != null && end != null && end > start;
    }

    public void post() throws IOException {

        if (status == EDITED) {

            try {
                country.getGuild().createScheduledEvent(
                                "Evento " + name,
                                "bteconosur.com",
                                OffsetDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.of("Z")),
                                OffsetDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.of("Z"))
                        )
                        .setDescription(description)
                        .setImage((image.exists() ? Icon.from(image) : null))
                        .queue(
                                scheduledEvent -> {
                                    try {
                                        this.scheduledEventID = scheduledEvent.getId();
                                        this.update("scheduled_event_id", scheduledEvent.getId());

                                        EmbedBuilder builder = this.getEmbed();
                                        Button link = Button.of(
                                                ButtonStyle.LINK,
                                                "https://discord.com/events/" + country.getGuildID() + "/" + scheduledEvent.getId(),
                                                "Ver evento de Discord",
                                                Emoji.fromUnicode("U+1F5D3")
                                        );

                                        if (image.exists()) {
                                            builder.setImage("attachment://image.png");
                                            for (Country c : plugin.getCountryManager().getAllCountries()) {
                                                c.getGlobalChatChannel().sendMessageEmbeds(builder.build())
                                                        .setFiles(FileUpload.fromData(image, "image.png"))
                                                        .setComponents(
                                                                ActionRow.of(
                                                                        link
                                                                )
                                                        ).queue();
                                                c.getCountryChatChannel().sendMessageEmbeds(builder.build())
                                                        .setFiles(FileUpload.fromData(image, "image.png"))
                                                        .setComponents(
                                                                ActionRow.of(
                                                                        link
                                                                )
                                                        ).queue();
                                            }
                                        } else {
                                            for (Country c : plugin.getCountryManager().getAllCountries()) {
                                                c.getGlobalChatChannel().sendMessageEmbeds(builder.build())
                                                        .setComponents(
                                                                ActionRow.of(
                                                                        link
                                                                )
                                                        ).queue();
                                                c.getCountryChatChannel().sendMessageEmbeds(builder.build())
                                                        .setComponents(
                                                                ActionRow.of(
                                                                        link
                                                                )
                                                        ).queue();
                                            }
                                        }

                                        this.update("status", "POSTED");
                                        this.status = POSTED;

                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void activate() {

        if (status == POSTED) {

            EmbedBuilder postBuilder = new EmbedBuilder();
            postBuilder.setTitle("Evento " + name);
            postBuilder.setColor(Color.GREEN);
            postBuilder.setDescription(description);
            postBuilder.setThumbnail(country.getIconURL());
            postBuilder.addField(
                    ":inbox_tray: Inicio:",
                    "<t:" + (start / 1000) + ":F>",
                    true
            );
            postBuilder.addField(
                    ":outbox_tray: Término:",
                    "<t:" + (end / 1000) + ":F>",
                    true
            );
            postBuilder.addBlankField(false);
            postBuilder.addField(
                    ":busts_in_silhouette: Miembros:",
                    (members.isEmpty() ? "Ninguno." : String.join(", ", plugin.getPlayerRegistry().getNames(members))),
                    false
            );
            List<FileUpload> uploads = new ArrayList<>();
            if (image.exists()) {
                postBuilder.setImage("attachment://image.png");
                uploads.add(FileUpload.fromData(image, "image.png"));
            }

            country.getProjectsForumChannel().createForumPost(
                    "Evento " + name,
                    MessageCreateData.fromEmbeds(postBuilder.build())
            )
                    .setFiles(uploads)
                    .setTags(country.getProjectsForumChannel().getAvailableTagsByName("Evento", true))
                    .queue(
                            forumPost -> {
                                try {

                                    plugin.getSqlManager().insert(
                                            "posts",
                                            new SQLValuesSet(
                                                    new SQLValue(
                                                            "target_type", "event"
                                                    ),
                                                    new SQLValue("target_id", id),
                                                    new SQLValue("channel_id", forumPost.getThreadChannel().getId()),
                                                    new SQLValue("message_id", forumPost.getMessage().getId()),
                                                    new SQLValue("members", members),
                                                    new SQLValue("country", country),
                                                    new SQLValue("cities", plugin.getCityManager().getCitiesAt(region, country)),
                                                    new SQLValue("name", "Evento " + name),
                                                    new SQLValue("description", description)
                                            )
                                    ).execute();

                                    this.update("post_channel_id", forumPost.getThreadChannel().getId());
                                    this.update("post_message_id", forumPost.getMessage().getId());
                                    this.postChannelID = forumPost.getThreadChannel().getId();
                                    this.postMessageID = forumPost.getMessage().getId();
                                    plugin.getBuildEventsRegistry().channelIDToEventID.put(forumPost.getThreadChannel().getId(), this.id);

                                    EmbedBuilder builder = this.getEmbed();
                                    builder.setColor(Color.GREEN);
                                    builder.setTitle("¡Evento " + name + " iniciado!");
                                    Button link = Button.of(
                                            ButtonStyle.LINK,
                                            "https://discord.com/events/" + country.getGuildID() + "/" + scheduledEventID,
                                            "Ver evento de Discord",
                                            Emoji.fromUnicode("U+1F5D3")
                                    );

                                    if (image.exists()) {
                                        builder.setImage("attachment://image.png");
                                        for (Country c : plugin.getCountryManager().getAllCountries()) {
                                            c.getGlobalChatChannel().sendMessageEmbeds(builder.build())
                                                    .setFiles(FileUpload.fromData(image, "image.png"))
                                                    .setComponents(
                                                            ActionRow.of(
                                                                    link
                                                            )
                                                    ).queue();
                                            c.getCountryChatChannel().sendMessageEmbeds(builder.build())
                                                    .setFiles(FileUpload.fromData(image, "image.png"))
                                                    .setComponents(
                                                            ActionRow.of(
                                                                    link
                                                            )
                                                    ).queue();
                                        }
                                    } else {
                                        for (Country c : plugin.getCountryManager().getAllCountries()) {
                                            c.getGlobalChatChannel().sendMessageEmbeds(builder.build())
                                                    .setComponents(
                                                            ActionRow.of(
                                                                    link
                                                            )
                                                    ).queue();
                                            c.getCountryChatChannel().sendMessageEmbeds(builder.build())
                                                    .setComponents(
                                                            ActionRow.of(
                                                                    link
                                                            )
                                                    ).queue();
                                        }
                                    }

                                    DefaultDomain domain = new DefaultDomain();
                                    for (UUID member : members) {
                                        domain.addPlayer(member);
                                        ServerPlayer s = plugin.getPlayerRegistry().get(member);
                                        s.sendNotification(
                                                getPrefix() + "¡El evento " + name + " ha comenzado!",
                                                "**[EVENTO]** » ¡El evento " + name + " ha comenzado!"
                                        );
                                    }
                                    region.setMembers(domain);
                                    plugin.getRegionManager().addRegion(region);

                                    this.update("status", "ACTIVE");
                                    this.status = ACTIVE;
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                    );
        }
    }

    public void finish() throws SQLException, IOException {

        if (status == Status.ACTIVE) {

            this.update("status", "FINISHED");
            this.status = FINISHED;

            plugin.getTerramapHandler().drawPolygon(
                    CoordinatesUtils.getCoords2D(plugin, ((ProtectedPolygonalRegion) region).getPoints()),
                    new Color(51, 60, 232),
                    "event" + id.toUpperCase()
            );

            plugin.getRegionManager().removeRegion("event_" + id);

            for (UUID member : members) {
                ServerPlayer s = plugin.getPlayerRegistry().get(member);
                s.sendNotification(
                        getPrefix() + "¡El evento " + name + " ha terminado!",
                        "**[EVENTO]** » ¡El evento " + name + " ha terminado!"
                );
                if (minimumType != null) {
                    s.getProjectManager().addPoints(minimumType, pointsGiven);
                } else {
                    s.getProjectManager().addPoints(country.getProjectTypes().get(0), pointsGiven);
                }
            }

            country.getLogsChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(Color.GREEN)
                            .setTitle("Evento " + name)
                            .addField(
                                    ":busts_in_silhouette: Miembros:",
                                    (members.isEmpty() ? "Ninguno." : String.join(", ", plugin.getPlayerRegistry().getNames(members))),
                                    false
                            )
                            .build()
            ).queue();

            String newID = StringUtils.generateCode(8, plugin.getBuildEventsRegistry().getIds(), LOWER_CASE);

            plugin.getSqlManager().insert(
                    "build_events",
                    new SQLValuesSet(
                            new SQLValue(
                                    "id", newID
                            ),
                            new SQLValue(
                                    "country", country
                            ),
                            new SQLValue(
                                    "status", "EDITED"
                            )
                    )
            ).execute();

            Set<City> cities = plugin.getCityManager().getCitiesAt(region, country);

            plugin.getSqlManager().insert(
                    "tour_displays",
                    new SQLValuesSet(
                            new SQLValue(
                                    "date", new Date(end)
                            ),
                            new SQLValue(
                                    "type", "build_event"
                            ),
                            new SQLValue(
                                    "id", this.id
                            ),
                            new SQLValue(
                                    "cities", cities
                            )
                    )
            ).execute();

            country.setBuildEventID(newID);
            plugin.getBuildEventsRegistry().addId(newID);

            image.delete();

        }

    }

    public String getPrefix() {
        return "§f[§1EVENTO§f] §7>> §f";
    }

    @Override
    public String getScoreboardTitle() {
        return "§a§lEvento " + name;
    }

    @Override
    public List<String> getScoreboardLines() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        List<String> lines = new ArrayList<>();

        lines.add("§fMiembros: §7" + members.size());

        lines.add(" ");
        lines.add("§fInicio: §7" + format.format(start));
        lines.add("§fTérmino: §7" + format.format(end));
        return lines;
    }

    @Override
    public String getScoreboardType() {
        return "event";
    }

    @Override
    public String getScoreboardID() {
        return "event_" + id;
    }

    @Override
    public Location getTeleportLocation() {
        return CoordinatesUtils.blockVector2DtoLocation(plugin, RegionUtils.getAveragePoint((ProtectedPolygonalRegion) region));
    }

}

package pizzaaxx.bteconosur.LegacyConversion;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Configuration.Configuration;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectTag;
import pizzaaxx.bteconosur.Projects.ProjectType;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.CoordinatesUtils;
import pizzaaxx.bteconosur.Utils.SatMapHandler;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

public class LegacyConverterCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public LegacyConverterCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (args.length < 1) {
            return true;
        }

        String countryName = args[0];

        Country c = plugin.getCountryManager().get(countryName);
        if (c == null) {
            return true;
        }


        int counter = 0;

        int sum = 0;
        for (Map.Entry<String, ProtectedRegion> entry : plugin.getRegionManager().getRegions().entrySet()) {
            Location loc = new Location(plugin.getWorld(), entry.getValue().getMaximumPoint().getX(), 100, entry.getValue().getMaximumPoint().getZ());
            Country country = plugin.getCountryManager().getCountryAt(loc);
            if (country == null) {
                continue;
            }
            if (entry.getKey().startsWith("project_") && country.equals(c)) {
                sum++;
            }
        }

        plugin.log(String.valueOf(sum));

        for (Map.Entry<String, ProtectedRegion> entry : plugin.getRegionManager().getRegions().entrySet()) {

            if (counter >= 3) {
                break;
            }

            String regionName = entry.getKey();
            ProtectedRegion region = entry.getValue();

            Random random = new Random();

            if (regionName.matches("project_[a-z]{6}")) {

                String id = regionName.replace("project_", "");

                if (plugin.getProjectRegistry().exists(id)) {
                    continue;
                }

                File file = new File(plugin.getDataFolder(), "projects/" + id + ".yml");

                if (!file.exists()) {
                    plugin.getRegionManager().removeRegion(regionName);
                    continue;
                }

                Configuration config = new Configuration(plugin, "projects/" + id);

                if (!config.contains("country") || !config.contains("difficulty")) {
                    plugin.getRegionManager().removeRegion(regionName);
                    continue;
                }

                UUID ownerUUID = null;
                if (config.contains("owner")) {
                    ownerUUID = UUID.fromString(config.getString("owner"));
                }

                Country country = plugin.getCountryManager().get(config.getString("country"));

                if (country == null || !country.equals(c)) {
                    continue;
                }

                String legacyDifficulty = config.getString("difficulty");

                int typeIndex = (legacyDifficulty.equals("facil") ? 0 : (legacyDifficulty.equals("intermedio") ? 1 : 2));

                ProjectType type = country.getProjectType(country.getLegacyDifficulties().get(typeIndex));

                int projectPoints = type.getPointsOptions().get(random.nextInt(type.getPointsOptions().size()));

                boolean pending = config.getBoolean("pending", false);

                String name = config.getString("name", null);

                Set<UUID> members = new HashSet<>();
                Set<UUID> allMembers = new HashSet<>();
                if (pending) {
                    if (config.contains("members")) {
                        for (String uuidString : config.getStringList("members")) {
                            members.add(UUID.fromString(uuidString));
                        }
                        allMembers.addAll(members);
                    }
                    allMembers.add(ownerUUID);
                } else {
                    allMembers.addAll(region.getMembers().getUniqueIds());
                    members.addAll(region.getMembers().getUniqueIds());
                    members.remove(ownerUUID);
                }

                String legacyTag = config.getString("tag", null);

                ProjectTag tag = null;
                if (legacyTag != null) {
                    tag = ProjectTag.valueOf(legacyTag.replace("centros_comerciales", "shopping").toUpperCase());
                }

                Set<City> cities = plugin.getCityManager().getCitiesAt(region, country);
                Set<String> cityNames = new HashSet<>();
                for (City city : cities) {
                    cityNames.add(city.getDisplayName());
                }

                for (UUID uuid : allMembers) {

                    if (!plugin.getPlayerRegistry().hasPlayedBefore(uuid)) {

                        Configuration playerConfig = new Configuration(plugin, "playerData/" + uuid.toString());

                        String playerName = playerConfig.getString("name", "~");

                        Set<String> roles = new HashSet<>();
                        if (playerConfig.contains("primaryGroup")) {
                            String role = playerConfig.getString("primaryGroup");

                            if (role.equals("mod") || role.equals("admin")) {
                                roles.add(role);
                            }
                        }

                        Map<String, String> presets = new HashMap<>();
                        if (playerConfig.contains("presets")) {
                            ConfigurationSection presetsSection = playerConfig.getConfigurationSection("presets");
                            for (String preset : presetsSection.getKeys(false)) {
                                presets.put(preset, presetsSection.getString(preset));
                            }
                        }

                        try {
                            plugin.getSqlManager().insert(
                                    "players",
                                    new SQLValuesSet(
                                            new SQLValue("uuid", uuid),
                                            new SQLValue("name", playerName),
                                            new SQLValue("roles", roles),
                                            new SQLValue("last_disconnected", 1683852998771L)
                                    )
                            ).execute();
                            plugin.getSqlManager().insert(
                                    "world_edit_managers",
                                    new SQLValuesSet(
                                            new SQLValue(
                                                    "uuid", uuid
                                            ),
                                            new SQLValue(
                                                    "presets", presets
                                            )
                                    )
                            ).execute();
                            plugin.getSqlManager().insert(
                                    "project_managers",
                                    new SQLValuesSet(
                                            new SQLValue("uuid", uuid),
                                            new SQLValue("points", "{}")
                                    )
                            ).execute();
                            if (playerConfig.contains("discord.name")) {
                                String dscName = playerConfig.getString("discord.name");
                                String dscDiscriminator = playerConfig.getString("discord.discriminator");
                                String dscID = playerConfig.getString("discord.id");
                                plugin.getSqlManager().insert(
                                        "discord_managers",
                                        new SQLValuesSet(
                                                new SQLValue("uuid", uuid),
                                                new SQLValue("id", dscID),
                                                new SQLValue("name", dscName),
                                                new SQLValue("discriminator", dscDiscriminator)
                                        )
                                ).execute();
                            }
                            plugin.getPlayerRegistry().registerUUID(uuid);
                            plugin.log("Created database registry for player with UUID §f" + uuid);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                    }

                }

                try {
                    plugin.getSqlManager().insert(
                            "projects",
                            new SQLValuesSet(
                                    new SQLValue("id", id),
                                    new SQLValue("name", name),
                                    new SQLValue("country", country),
                                    new SQLValue("cities", cities),
                                    new SQLValue("pending", (pending ? new Date(System.currentTimeMillis()) : null)),
                                    new SQLValue("type", type),
                                    new SQLValue("points", projectPoints),
                                    new SQLValue("members", members),
                                    new SQLValue("owner", ownerUUID),
                                    new SQLValue("tag", tag)
                            )
                    ).execute();
                    plugin.getProjectRegistry().registerID(id);
                    Project project = plugin.getProjectRegistry().get(id);
                    if (ownerUUID != null) {
                        ServerPlayer s = plugin.getPlayerRegistry().get(ownerUUID);
                        s.getProjectManager().addProject(project);
                    }
                    for (UUID member : members) {
                        plugin.getPlayerRegistry().get(member).getProjectManager().addProject(project);
                    }

                    if (project.isClaimed()) {
                        List<String> memberNames = new ArrayList<>();
                        for (UUID memberUUID : project.getMembers()) {
                            memberNames.add(plugin.getPlayerRegistry().get(memberUUID).getName().replace("_", "\\_"));
                        }

                        ForumChannel channel = project.getCountry().getProjectsForumChannel();
                        Set<ForumTag> tags = new HashSet<>();
                        tags.add(channel.getAvailableTagsByName("En construcción", true).get(0));
                        if (project.getTag() != null) {
                            tags.add(channel.getAvailableTagsByName(project.getTag().toString(), true).get(0));
                        }
                        tags.add(channel.getAvailableTagsByName(project.getType().getDisplayName(), true).get(0));

                        project.getCountry().getProjectsForumChannel().createForumPost(
                                        "Proyecto " + project.getId().toUpperCase() + (cities.isEmpty() ? "" : " - " + String.join(", ", cityNames)),
                                        MessageCreateData.fromContent(":speech_balloon: **Descripción:** N/A")
                                )
                                .setEmbeds(
                                        new EmbedBuilder()
                                                .setColor(project.getType().getColor())
                                                .addField(
                                                        ":crown: Líder:",
                                                        plugin.getPlayerRegistry().get(project.getOwner()).getName(),
                                                        true
                                                )
                                                .addField(
                                                        ":busts_in_silhouette: Miembros:",
                                                        (memberNames.isEmpty() ? "Sin miembros." : String.join(", ", memberNames)),
                                                        true
                                                )
                                                .addField(
                                                        ":game_die: Tipo:",
                                                        project.getType().getDisplayName() + " (" + project.getPoints() + " puntos)",
                                                        true
                                                )
                                                .build()
                                )
                                .setFiles(
                                        FileUpload.fromData(
                                                plugin.getSatMapHandler().getMapStream(
                                                        new SatMapHandler.SatMapPolygon(
                                                                plugin,
                                                                project.getRegionPoints()
                                                        )
                                                ),
                                                "projectMap.png"
                                        )
                                ).setTags(
                                        tags
                                ).queue(
                                        forumPost -> {
                                            try {
                                                plugin.getSqlManager().insert(
                                                        "posts",
                                                        new SQLValuesSet(
                                                                new SQLValue("target_type", "project"),
                                                                new SQLValue("target_id", project.getId()),
                                                                new SQLValue("channel_id", forumPost.getThreadChannel().getId()),
                                                                new SQLValue("members", project.getAllMembers()),
                                                                new SQLValue("country", project.getCountry()),
                                                                new SQLValue("cities", project.getCities()),
                                                                new SQLValue("name", "Proyecto " + project.getId().toUpperCase()),
                                                                new SQLValue("description", "N/A"),
                                                                new SQLValue("message_id", forumPost.getMessage().getId())
                                                        )
                                                ).execute();
                                            } catch (SQLException e) {
                                                forumPost.getThreadChannel().delete().queue();
                                            }
                                        }
                                );
                    }

                    plugin.getTerramapHandler().drawPolygon(
                            CoordinatesUtils.getCoords2D(plugin, project.getRegionPoints()),
                            (ownerUUID == null ? new Color(78, 255, 71) : new Color(255, 200, 0)),
                            id
                    );
                    sender.sendMessage("Created project " + id.toUpperCase() + ".");
                    counter++;
                } catch (SQLException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }
}

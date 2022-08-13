package pizzaaxx.bteconosur.country.cities.projects;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BteConoSur;
import pizzaaxx.bteconosur.Chat.ProjectChat;
import pizzaaxx.bteconosur.configuration.Configuration;
import pizzaaxx.bteconosur.country.Country;
import pizzaaxx.bteconosur.country.cities.City;
import pizzaaxx.bteconosur.country.cities.projects.ChangeAction.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Project {

    /**
     * The difficulty of a project. A higher difficulty gives a higher amount of points.
     */
    public enum Difficulty {
        FACIL(15), INTERMEDIO(50), DIFICIL(100);

        private final int points;

        Difficulty(int points) {
            this.points = points;
        }

        public int getPoints() {
            return points;
        }
    }

    /**
     * Represents what kind of buildings this project contains.
     */
    public enum Tag {

        EDIFICIOS(":department_store:"),
        DEPARTAMENTOS(":hotel:"),
        CASAS(":homes:"),
        PARQUES(":deciduous_tree:"),
        ESTABLECIMIENTOS(":school:"),
        CARRETERAS(":motorway:"),
        CENTROS_COMERCIALES(":shopping_bags:");

        private final String emoji;

        Tag(String emoji) {
            this.emoji = emoji;
        }

        public String getEmoji() {
            return emoji;
        }

        @Override
        public @NotNull String toString() {
            return super.toString().toLowerCase();
        }

        public @NotNull String toFormattedString() {
            return super.toString().replace("_", "");
        }
    }

    private final BteConoSur plugin;
    private final Configuration config;
    private final String id;
    private final Country country;
    private final City city;
    public Difficulty difficulty;

    public final Set<UUID> members = new HashSet<>();
    public UUID owner;

    public String name;
    public Tag tag;
    public boolean pending;

    public ProtectedRegion region;
    private final ProjectChat chat;
    private final ProjectsRegistry registry;

    /**
     * Loads a project from the server's storage. You should check first if the project exists.
     * @param registry The {@link pizzaaxx.bteconosur.projects.ProjectRegistry} this project belongs to.
     * @param id The id of this project.
     */
    public Project(@NotNull ProjectsRegistry registry, @NotNull String id) {

        this.registry = registry;

        this.id = id;

        this.plugin = registry.getPlugin();

        this.region = plugin.getRegionsManager().getRegion("project_" + id);

        this.city = registry.getCity();

        this.country = city.getCountry();

        config = new Configuration(plugin, "countries/" + country.getName() + "/cities/" + city.getName() + "/projects/" + id);

        difficulty = Difficulty.valueOf(config.getString("difficulty").toUpperCase());

        if (config.contains("tag")) {
            tag = Tag.valueOf(config.getString("tag").toUpperCase());
        }

        name = config.getString("name");

        if (config.contains("members")) {
            for (String uuid : config.getStringList("members")) {
                members.add(UUID.fromString(uuid));
            }
        }

        if (config.contains("owner")) {
            owner = UUID.fromString(config.getString("owner"));
        }

        pending = config.getBoolean("pending");

        this.chat = plugin.getChatManager().getChat(this);

    }

    public Country getCountry() {
        return country;
    }

    public City getCity() {
        return city;
    }

    public String getId() {
        return id;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    /**
     *
     * @return All members, included the owner.
     */
    public Set<UUID> getAllMembers() {
        Set<UUID> members = new HashSet<>(this.members);
        members.add(owner);
        return members;
    }

    public UUID getOwner() {
        return owner;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        return id.toUpperCase();
    }

    public BteConoSur getPlugin() {
        return plugin;
    }

    public Set<Player> getPlayersInsideProject() {

        RegionManager manager = plugin.getRegionsManager();
        Set<Player> players = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (manager.getApplicableRegions(player.getLocation()).getRegions().contains(region)) {
                players.add(player);
            }
        }

        return players;
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isPending() {
        return pending;
    }

    public Configuration getConfig() {
        return config;
    }

    public void updatePlayersScoreboard() {
        new UpdateScoreboardProjectAction(this).exec();
    }

    public SetNameProjectAction setName(String name) {
        return new SetNameProjectAction(this, name);
    }

    public SetTagProjectAction setTag(Tag tag) {
        return new SetTagProjectAction(this, this.tag, tag);
    }

    public TransferProjectAction transferTo(UUID target) {
        return new TransferProjectAction(this, this.owner, target);
    }

    public SetPendingProjectAction setPending(boolean pending) {
        return new SetPendingProjectAction(this, pending);
    }

    public AddMembersProjectAction addMember(UUID uuid) {
        return new AddMembersProjectAction(this, uuid);
    }

    public RemoveMembersProjectAction removeMember(UUID uuid) {
        return new RemoveMembersProjectAction(this, uuid);
    }

    public ProjectChat getChat() {
        return chat;
    }

    public ProjectsRegistry getRegistry() {
        return registry;
    }

    public void saveToDisk() {

    }
}
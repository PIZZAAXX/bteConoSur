package pizzaaxx.bteconosur;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import fr.minuskube.netherboard.Netherboard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BuildEvents.BuildEventsRegistry;
import pizzaaxx.bteconosur.BuildEvents.Commands.BuildEventCommand;
import pizzaaxx.bteconosur.Chat.*;
import pizzaaxx.bteconosur.Chat.Commands.ChatCommand;
import pizzaaxx.bteconosur.Chat.Commands.NicknameCommand;
import pizzaaxx.bteconosur.Cities.CityManager;
import pizzaaxx.bteconosur.Cities.CityScoreboardRegionListener;
import pizzaaxx.bteconosur.Cities.Commands.CitiesCommand;
import pizzaaxx.bteconosur.Cities.Events.CityEnterEvent;
import pizzaaxx.bteconosur.Commands.*;
import pizzaaxx.bteconosur.Commands.Custom.CustomCommandsManager;
import pizzaaxx.bteconosur.Commands.Managing.DeletePlayerDataCommand;
import pizzaaxx.bteconosur.Commands.Managing.DonatorCommand;
import pizzaaxx.bteconosur.Commands.Managing.ReloadPlayerCommand;
import pizzaaxx.bteconosur.Commands.Managing.StreamerCommand;
import pizzaaxx.bteconosur.Configuration.Configuration;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Countries.CountryManager;
import pizzaaxx.bteconosur.Countries.CountryScoreboardRegionListener;
import pizzaaxx.bteconosur.Discord.DiscordHandler;
import pizzaaxx.bteconosur.Discord.FuzzyMatching.FuzzyMatchCondition;
import pizzaaxx.bteconosur.Discord.FuzzyMatching.FuzzyMatcherListener;
import pizzaaxx.bteconosur.Discord.FuzzyMatching.MustContainFuzzyMatcherCondition;
import pizzaaxx.bteconosur.Discord.Link.LinkCommand;
import pizzaaxx.bteconosur.Discord.Link.LinksRegistry;
import pizzaaxx.bteconosur.Discord.SlashCommands.*;
import pizzaaxx.bteconosur.Events.*;
import pizzaaxx.bteconosur.Help.HelpCommand;
import pizzaaxx.bteconosur.Inventory.InventoryHandler;
import pizzaaxx.bteconosur.LegacyConversion.LegacyConverterCommand;
import pizzaaxx.bteconosur.LegacyConversion.RegisterFinishedCommand;
import pizzaaxx.bteconosur.Player.Managers.ChatManager;
import pizzaaxx.bteconosur.Player.Notifications.NotificationsService;
import pizzaaxx.bteconosur.Player.PlayerRegistry;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Commands.Listeners.ProjectCreationRequestListener;
import pizzaaxx.bteconosur.Projects.Commands.Listeners.ProjectRedefineRequestListener;
import pizzaaxx.bteconosur.Projects.Commands.ProjectsCommand;
import pizzaaxx.bteconosur.Projects.Finished.FinishedProjectsRegistry;
import pizzaaxx.bteconosur.Projects.Listeners.ProjectClickListener;
import pizzaaxx.bteconosur.Projects.Listeners.ProjectRegionListener;
import pizzaaxx.bteconosur.Projects.ProjectRegistry;
import pizzaaxx.bteconosur.Regions.RegionListenersHandler;
import pizzaaxx.bteconosur.SQL.SQLManager;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardDisplay;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardHandler;
import pizzaaxx.bteconosur.Tablist.TablistExpansion;
import pizzaaxx.bteconosur.Terramap.TerramapHandler;
import pizzaaxx.bteconosur.Terramap.TerramapServer;
import pizzaaxx.bteconosur.Utils.FuzzyMatching.FuzzyMatcher;
import pizzaaxx.bteconosur.Utils.SatMapHandler;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetFillCommand;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetsRegistry;
import pizzaaxx.bteconosur.WorldEdit.Assets.Commands.AssetGroupCommand;
import pizzaaxx.bteconosur.WorldEdit.Assets.Commands.AssetsCommand;
import pizzaaxx.bteconosur.WorldEdit.Assets.Listener.AssetInventoryListener;
import pizzaaxx.bteconosur.WorldEdit.Assets.Listener.AssetListener;
import pizzaaxx.bteconosur.WorldEdit.Assets.Rendering.ModelsManager;
import pizzaaxx.bteconosur.WorldEdit.Commands.DivideCommand;
import pizzaaxx.bteconosur.WorldEdit.Commands.IncrementCommand;
import pizzaaxx.bteconosur.WorldEdit.Commands.PolywallsCommand;
import pizzaaxx.bteconosur.WorldEdit.Commands.TerraformCommand;
import pizzaaxx.bteconosur.WorldEdit.Presets.PresetsCommand;
import pizzaaxx.bteconosur.WorldEdit.Presets.PresetsListener;
import pizzaaxx.bteconosur.WorldEdit.Selection.SelUndoRedoCommand;
import pizzaaxx.bteconosur.WorldEdit.Shortcuts;
import pizzaaxx.bteconosur.WorldEdit.WorldEditHandler;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

public class BTEConoSur extends JavaPlugin implements Prefixable, ScoreboardDisplay {

    private World mainWorld;

    public World getWorld() {
        return mainWorld;
    }

    private com.sk89q.worldedit.world.World worldEditWorld;

    public com.sk89q.worldedit.world.World getWorldEditWorld() {
        return this.worldEditWorld;
    }

    private final FuzzyMatcher fuzzyMatcher = new FuzzyMatcher(this);

    public FuzzyMatcher getFuzzyMatcher() {
        return fuzzyMatcher;
    }

    private final Netherboard netherboard = Netherboard.instance();

    public Netherboard getNetherboard() {
        return netherboard;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectMapper getJSONMapper() {
        return mapper;
    }

    private SQLManager sqlManager;

    public SQLManager getSqlManager() {
        return sqlManager;
    }

    private PlayerRegistry playerRegistry;

    public PlayerRegistry getPlayerRegistry() {
        return playerRegistry;
    }

    private final AssetsRegistry assetsRegistry = new AssetsRegistry(this);

    public AssetsRegistry getAssetsRegistry() {
        return assetsRegistry;
    }

    private JDA bot;

    public JDA getBot() {
        return bot;
    }

    private final CountryManager countryManager = new CountryManager(this);

    public CountryManager getCountryManager() {
        return countryManager;
    }

    private WorldGuardPlugin worldGuard;

    public WorldGuardPlugin getWorldGuard() {
        return worldGuard;
    }

    private RegionManager regionManager;

    public RegionManager getRegionManager() {
        return regionManager;
    }

    private final CityManager cityManager = new CityManager(this);

    public CityManager getCityManager() {
        return cityManager;
    }

    private final ProjectRegistry projectRegistry = new ProjectRegistry(this);

    public ProjectRegistry getProjectRegistry() {
        return projectRegistry;
    }

    private final FinishedProjectsRegistry finishedProjectsRegistry = new FinishedProjectsRegistry(this);

    public FinishedProjectsRegistry getFinishedProjectsRegistry() {
        return finishedProjectsRegistry;
    }

    private final WorldEditHandler worldEditHandler = new WorldEditHandler(this);

    public WorldEditHandler getWorldEdit() {
        return worldEditHandler;
    }

    private final InventoryHandler inventoryHandler = new InventoryHandler(this);

    public InventoryHandler getInventoryHandler() {
        return inventoryHandler;
    }

    private final NotificationsService notificationsService = new NotificationsService(this);

    public NotificationsService getNotificationsService() {
        return notificationsService;
    }

    private final SelUndoRedoCommand selUndoRedoCommand = new SelUndoRedoCommand(this);

    public SelUndoRedoCommand getSelUndoRedoCommand() {
        return selUndoRedoCommand;
    }

    private final SatMapHandler satMapHandler = new SatMapHandler(this);

    public SatMapHandler getSatMapHandler() {
        return satMapHandler;
    }

    private final TerramapHandler terramapHandler = new TerramapHandler(this);

    public TerramapHandler getTerramapHandler() {
        return terramapHandler;
    }

    TerramapServer terramapServer = new TerramapServer(this);

    private final DiscordHandler discordHandler = new DiscordHandler();

    public DiscordHandler getDiscordHandler() {
        return discordHandler;
    }

    private final ChatHandler chatHandler = new ChatHandler(this);

    public ChatHandler getChatHandler() {
        return chatHandler;
    }

    private final LinksRegistry linksRegistry = new LinksRegistry(this);

    public LinksRegistry getLinksRegistry() {
        return linksRegistry;
    }

    private final CustomCommandsManager customCommandsManager = new CustomCommandsManager(this);

    public CustomCommandsManager getCustomCommandsManager() {
        return customCommandsManager;
    }

    private final ScoreboardHandler scoreboardHandler = new ScoreboardHandler(this);

    public ScoreboardHandler getScoreboardHandler() {
        return scoreboardHandler;
    }

    private final PrefixCommand prefixCommand = new PrefixCommand(this);

    public PrefixCommand getPrefixCommand() {
        return prefixCommand;
    }

    private final BuildEventsRegistry buildEventsRegistry = new BuildEventsRegistry(this);

    public BuildEventsRegistry getBuildEventsRegistry() {
        return buildEventsRegistry;
    }

    private final ModelsManager modelsManager = new ModelsManager(this);

    public ModelsManager getModelsManager() {
        return modelsManager;
    }

    @Override
    public void onEnable() {
        this.log("BUILD THE EARTH: CONO SUR");
        this.log("Developed by PIZZAAXX");
        this.log(" ");
        this.log("Starting plugin...");

        this.log("Starting database connection...");
        try {
            sqlManager = new SQLManager(this);
        } catch (SQLException e) {
            this.error("Plugin starting stopped. Database connection failed.");
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.log("Database connection established.");

        this.log("Starting player registry...");
        try {
            this.playerRegistry = new PlayerRegistry(this);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        mainWorld = Bukkit.getWorld("BTECS");
        worldEditWorld = new BukkitWorld(mainWorld);
        worldGuard = WorldGuardPlugin.inst();
        regionManager = WorldGuardPlugin.inst().getRegionManager(mainWorld);

        this.log("Registering events...");

        RegionListenersHandler regionListenersHandler = new RegionListenersHandler(this);
        regionListenersHandler.registerEnter(
                input -> input.startsWith("city_") && !input.endsWith("_urban"),
                new CityEnterEvent(this)
        );
        regionListenersHandler.registerBoth(
                input -> input.matches("project_[a-z]{6}"),
                new ProjectRegionListener(this)
        );
        regionListenersHandler.registerBoth(
                input -> input.matches("city_([a-z]{1,32})(?!_urban)"),
                new CityScoreboardRegionListener(this)
        );
        regionListenersHandler.registerBoth(
                input -> input.startsWith("country_"),
                new CountryScoreboardRegionListener(this)
        );

        GetCommand getCommand = new GetCommand(this);

        ProjectsCommand projectsCommand = new ProjectsCommand(this);

        TourCommand tourCommand = new TourCommand(this);

        this.registerListeners(
                this,
                regionListenersHandler,
                new PreLoginEvent(this),
                new JoinEvent(this),
                new QuitEvent(this),
                new Shortcuts(this),
                this.inventoryHandler,
                new TeleportEvent(),
                getCommand,
                new AssetListener(this),
                new AssetInventoryListener(this),
                new PresetsListener(this),
                this.selUndoRedoCommand,
                chatHandler,
                projectsCommand,
                tourCommand,
                new SecurityEvents(this),
                new ProjectClickListener(this)
        );

        this.log("Starting chats...");

        // --- COUNTRIES ---
        this.log("Starting country manager...");
        try {
            countryManager.init();
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            this.error("Plugin starting stopped. Country manager startup failed.");
            return;
        }

        // --- CITIES ---
        this.log("Starting city manager...");
        try {
            cityManager.init();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            this.error("Plugin starting stopped. City manager startup failed.");
            return;
        }

        // --- ASSETS ---
        this.log("Starting assets registry...");
        try {
            assetsRegistry.init();
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            this.error("Plugin starting stopped. Assets registry startup failed.");
            return;
        }

        // --- PROJECTS ---
        this.log("Starting project registry...");
        try {
            projectRegistry.init();
        } catch (SQLException e) {
            this.error("Plugin starting stopped. Project registry startup failed.");
            return;
        }

        // --- FINISHED PROJECTS ---
        this.log("Starting finished projects registry...");
        try {
            finishedProjectsRegistry.init();
        } catch (SQLException e) {
            this.error("Plugin starting stopped. Finished projects registry startup failed.");
            return;
        }

        // --- LINKS ---
        this.log("Starting links registry...");
        try {
            linksRegistry.init();
        } catch (SQLException | IOException e) {
            this.error("Plugin starting stopped. Links registry startup failed.");
            return;
        }

        // --- LINKS ---
        this.log("Starting build events registry...");
        try {
            buildEventsRegistry.init();
        } catch (SQLException e) {
            this.error("Plugin starting stopped. Build events registry startup failed.");
            return;
        }

        /*
        // --- MODELS ---
        this.log("Starting models manager...");
        try {
            modelsManager.init();
        } catch (IOException e) {
            e.printStackTrace();
            this.error("Plugin starting stopped. Models manager startup failed.");
            return;
        }


        try {
            RenderableAsset renderableAsset = new RenderableAsset(this, assetsRegistry.get("emtfov"));
            renderableAsset.generateGIF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
         */


        LinkCommand linkCommand = new LinkCommand(this);
        BuildEventCommand buildEventCommand = new BuildEventCommand(this);
        FindColorCommand findColorCommand;
        try {
            findColorCommand = new FindColorCommand(this);
        } catch (URISyntaxException | IOException e) {
            this.error("Plugin starting stopped. FindColor command startup failed.");
            return;
        }

        // --- DISCORD ---
        Configuration config = new Configuration(this, "config");
        String token = config.getString("token");

        JDABuilder jdaBuilder = JDABuilder.createDefault(token);
        HelpCommand helpCommand = new HelpCommand(this);
        IPCommand ipCommand = new IPCommand(this);

        FuzzyMatchCondition notInPostCondition = (event, message) -> {
            Channel channel = event.getChannel();
            if (channel instanceof ThreadChannel) {
                ThreadChannel threadChannel = (ThreadChannel) channel;
                if (threadChannel.getParentChannel() instanceof ForumChannel) {
                    return !this.getCountryManager().projectForumChannels.contains(threadChannel.getParentChannel().getId());
                }
            }
            return true;
        };

        FuzzyMatcherListener fuzzyMatcherListener = new FuzzyMatcherListener(this);
        fuzzyMatcherListener.register(
                new String[]{
                        "cual es la ip?",
                        "cual es la ip del servidor?",
                        "cual es la ip del server?",
                        "ip del servidor?",
                        "ip del server?"
                },
                FuzzyMatcherListener.MatchingMethod.COMPLETE,
                3,
                ipCommand,
                new MustContainFuzzyMatcherCondition("ip"),
                notInPostCondition
        );
        fuzzyMatcherListener.register(
                new String[]{
                        "que hay construido en",
                        "que hay hecho en",
                        "hay algo construido en",
                        "hay algo hecho en",
                        "que hay en"
                },
                FuzzyMatcherListener.MatchingMethod.PARTIAL,
                3,
                new CityFuzzyListener(this),
                notInPostCondition
        );

        jdaBuilder.enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.SCHEDULED_EVENTS
        );
        jdaBuilder.enableCache(
                CacheFlag.SCHEDULED_EVENTS
        );
        jdaBuilder.addEventListeners(
                linkCommand,
                new ProjectCreationRequestListener(this),
                new ProjectRedefineRequestListener(this),
                chatHandler,
                helpCommand,
                discordHandler,
                new CityCommand(this),
                new ScoreboardCommand(this),
                new PlayerCommand(this),
                buildEventCommand,
                new ProjectCommand(this),
                new TutorialsCommand(this),
                new IPCommand(this),
                new OnlineCommand(this),
                new ModsCommand(this),
                new RegisterFinishedCommand(this),
                fuzzyMatcherListener,
                new PatternCommand(this),
                findColorCommand,
                new SchematicCommand(this),
                new ImagesCommand(this)
        );

        try {
            jdaBuilder.addEventListeners(
                    new ManageCityCommand(this)
            );
        } catch (IOException e) {
            this.error("Error loading cities schema.");
            return;
        }

        jdaBuilder.setStatus(OnlineStatus.ONLINE);
        jdaBuilder.setActivity(Activity.playing("bteconosur.com"));

        try  {
            bot = jdaBuilder.build().awaitReady();

            for (Object obj : bot.getRegisteredListeners()) {
                if (obj instanceof SlashCommandContainer) {
                    SlashCommandContainer container = (SlashCommandContainer) obj;
                    this.getDiscordHandler().checkCommand(container);
                }
            }
        } catch (InterruptedException e) {
            this.error("Plugin starting stopped. Bot startup failed.");
            return;
        }

        // --- COMMANDS ---
        this.log("Registering commands...");
        getCommand("city").setExecutor(new CitiesCommand(this));
        getCommand("increment").setExecutor(new IncrementCommand(this));
        getCommand("link").setExecutor(linkCommand);
        getCommand("unlink").setExecutor(linkCommand);
        getCommand("tpdir").setExecutor(new TpdirCommand(this));
        getCommand("deleteplayerdata").setExecutor(new DeletePlayerDataCommand(this));
        getCommand("height").setExecutor(new HeightCommand(this));
        getCommand("googleMaps").setExecutor(new GoogleMapsCommand(this));
        getCommand("banners").setExecutor(new BannersCommand());
        getCommand("get").setExecutor(getCommand);
        getCommand("/polywalls").setExecutor(new PolywallsCommand(this));
        getCommand("pwarp").setExecutor(new PWarpsCommand(this));
        getCommand("asset").setExecutor(new AssetsCommand(this));
        getCommand("assetgroup").setExecutor(new AssetGroupCommand(this));
        getCommand("/divide").setExecutor(new DivideCommand(this));
        getCommand("/terraform").setExecutor(new TerraformCommand(this));
        getCommand("/assetfill").setExecutor(new AssetFillCommand(this));
        getCommand("preset").setExecutor(new PresetsCommand(this));
        getCommand("/selredo").setExecutor(this.selUndoRedoCommand);
        getCommand("/selundo").setExecutor(this.selUndoRedoCommand);
        getCommand("nightvision").setExecutor(new NightVisionCommand());
        getCommand("project").setExecutor(projectsCommand);
        getCommand("chat").setExecutor(new ChatCommand(this));
        getCommand("nickname").setExecutor(new NicknameCommand(this));
        getCommand("runnableCommand").setExecutor(customCommandsManager);
        getCommand("tour").setExecutor(tourCommand);
        getCommand("scoreboard").setExecutor(new pizzaaxx.bteconosur.Scoreboard.ScoreboardCommand(this));
        getCommand("prefix").setExecutor(prefixCommand);
        getCommand("buildevent").setExecutor(buildEventCommand);
        getCommand("streamer").setExecutor(new StreamerCommand(this));
        getCommand("donator").setExecutor(new DonatorCommand(this));
        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("convertlegacy").setExecutor(new LegacyConverterCommand(this));
        getCommand("registerfinished").setExecutor(new RegisterFinishedCommand(this));
        getCommand("reloadplayer").setExecutor(new ReloadPlayerCommand(this));

        EmbedBuilder startEmbed = new EmbedBuilder();
        startEmbed.setColor(Color.GREEN);
        startEmbed.setTitle("¡El servidor está online!");
        startEmbed.setDescription(":link: **IP:** bteconosur.com");
        MessageEmbed embed = startEmbed.build();
        for (Country country : countryManager.getAllCountries()) {
            country.getGlobalChatChannel().sendMessageEmbeds(embed).queue();
            country.getCountryChatChannel().sendMessageEmbeds(embed).queue();

        }

        try {
            terramapServer.init();
            this.log("Terramap server started on " + terramapServer.getServer().getAddress().getHostName() + ":" + terramapServer.getServer().getAddress().getPort());
        } catch (Exception e) {
            e.printStackTrace();
            this.error("The Terramap tile server couldn't be started.");
        }

        chatHandler.registerChat(new GlobalChat(this, chatHandler));
        for (Country country : countryManager.getAllCountries()) {
            chatHandler.registerChat(new CountryChat(this, chatHandler, country));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ServerPlayer s = this.getPlayerRegistry().get(player.getUniqueId());
            ChatManager chatManager = s.getChatManager();
            try {
                Chat chat = chatManager.getCurrentChat();
                chat.addPlayer(player.getUniqueId());
            } catch (SQLException e) {
                this.warn("Problem with Chat Manager: " + player.getUniqueId());
            }
        }

        this.scoreboardHandler.init();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TablistExpansion(this).register();
        }

        // Cube.main(new String[0]);

        /*
        try {
            Asset asset = assetsRegistry.get("bekvwj");
            RenderableAsset renderableAsset = new RenderableAsset(this, asset);
            renderableAsset.generateGIF();
        } catch (IOException e) {
            e.printStackTrace();
        }
         */
    }

    @Override
    public void onDisable() {
        try {
            terramapServer.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setTitle("El servidor se ha apagado.");
        embedBuilder.setDescription("Te esperamos cuando vuelva a estar disponible.");
        MessageEmbed embed = embedBuilder.build();
        for (Country country : countryManager.getAllCountries()) {
            country.getGlobalChatChannel().sendMessageEmbeds(embed).queue();
            country.getCountryChatChannel().sendMessageEmbeds(embed).queue();
        }

        for (InteractionHook hook : messagesToDelete) {
            hook.deleteOriginal().queue();
        }

        bot.shutdown();
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(this.getPrefix() + message);
    }

    public void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "§e" + message);
    }

    public void error(String message) {
        Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "§c" + message);
    }

    private void registerListeners(BTEConoSur plugin, @NotNull Listener ... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }

    @Nullable
    public Player getOnlinePlayer(String partialName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(partialName.toLowerCase())) {
                return player;
            }
        }
        return null;
    }

    public final Set<InteractionHook> messagesToDelete = new HashSet<>();

    @Override
    public String getPrefix() {
        return "§f[§2CONO §aSUR§f] §7>> §f";
    }

    @Override
    public String getScoreboardTitle() {
        return "§2§lBTE §a§lCono Sur";
    }

    @Override
    public List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();
        lines.add("§fOnline: §7" + Bukkit.getOnlinePlayers().size());
        lines.add(" ");

        Map<Country, Integer> playersPerCountry = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Country c = countryManager.getCountryAt(p.getLocation());
            int count = playersPerCountry.getOrDefault(c, 0);
            count++;
            playersPerCountry.put(c, count);
        }

        for (Country country : this.getCountryManager().getAllCountries()) {
            lines.add("§f" + country.getDisplayName() + ": §7" + playersPerCountry.getOrDefault(country, 0));
        }

        return lines;
    }

    @Override
    public String getScoreboardType() {
        return "server";
    }

    @Override
    public String getScoreboardID() {
        return "server";
    }

    public Set<ProtectedRegion> getApplicableRegions(Location loc) {

        Set<ProtectedRegion> result = new HashSet<>(this.regionManager.getApplicableRegions(loc).getRegions());
        for (ProtectedRegion region : this.cityManager.regions.values()) {
            if (region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                result.add(region);
            }
        }

        return result;

    }
}

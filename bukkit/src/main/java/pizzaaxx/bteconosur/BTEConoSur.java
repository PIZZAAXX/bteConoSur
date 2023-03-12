package pizzaaxx.bteconosur;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.Chat.*;
import pizzaaxx.bteconosur.Chat.Commands.ChatCommand;
import pizzaaxx.bteconosur.Chat.Commands.NicknameCommand;
import pizzaaxx.bteconosur.Cities.CityManager;
import pizzaaxx.bteconosur.Cities.Commands.CitiesCommand;
import pizzaaxx.bteconosur.Cities.Events.CityEnterEvent;
import pizzaaxx.bteconosur.Commands.*;
import pizzaaxx.bteconosur.Commands.Custom.CustomCommandsManager;
import pizzaaxx.bteconosur.Commands.Managing.DeletePlayerDataCommand;
import pizzaaxx.bteconosur.Configuration.Configuration;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Countries.CountryManager;
import pizzaaxx.bteconosur.Discord.DiscordHandler;
import pizzaaxx.bteconosur.Discord.Link.LinkCommand;
import pizzaaxx.bteconosur.Discord.Link.LinksRegistry;
import pizzaaxx.bteconosur.Discord.SlashCommands.*;
import pizzaaxx.bteconosur.Events.JoinEvent;
import pizzaaxx.bteconosur.Events.PreLoginEvent;
import pizzaaxx.bteconosur.Events.QuitEvent;
import pizzaaxx.bteconosur.Events.TeleportEvent;
import pizzaaxx.bteconosur.Help.HelpCommand;
import pizzaaxx.bteconosur.Inventory.InventoryHandler;
import pizzaaxx.bteconosur.Player.Managers.ChatManager;
import pizzaaxx.bteconosur.Player.Notifications.NotificationsService;
import pizzaaxx.bteconosur.Player.PlayerRegistry;
import pizzaaxx.bteconosur.Posts.Commands.ProjectPostCommand;
import pizzaaxx.bteconosur.Posts.Listener.PostsListener;
import pizzaaxx.bteconosur.Posts.PostsRegistry;
import pizzaaxx.bteconosur.Projects.Commands.Listeners.ProjectCreationRequestListener;
import pizzaaxx.bteconosur.Projects.Commands.ProjectsCommand;
import pizzaaxx.bteconosur.Projects.Finished.FinishedProjectsRegistry;
import pizzaaxx.bteconosur.Projects.Listeners.ActionBarListener;
import pizzaaxx.bteconosur.Projects.ProjectRegistry;
import pizzaaxx.bteconosur.Regions.RegionListenersHandler;
import pizzaaxx.bteconosur.SQL.SQLManager;
import pizzaaxx.bteconosur.Terramap.TerramapHandler;
import pizzaaxx.bteconosur.Terramap.TerramapServer;
import pizzaaxx.bteconosur.Terramap.Testing.DrawPolygonCommand;
import pizzaaxx.bteconosur.Utils.FuzzyMatching.FuzzyMatcher;
import pizzaaxx.bteconosur.Utils.SatMapHandler;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetFillCommand;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetsRegistry;
import pizzaaxx.bteconosur.WorldEdit.Assets.Commands.AssetGroupCommand;
import pizzaaxx.bteconosur.WorldEdit.Assets.Commands.AssetsCommand;
import pizzaaxx.bteconosur.WorldEdit.Assets.Listener.AssetInventoryListener;
import pizzaaxx.bteconosur.WorldEdit.Assets.Listener.AssetListener;
import pizzaaxx.bteconosur.WorldEdit.Commands.*;
import pizzaaxx.bteconosur.WorldEdit.Presets.PresetsCommand;
import pizzaaxx.bteconosur.WorldEdit.Presets.PresetsListener;
import pizzaaxx.bteconosur.WorldEdit.Selection.SelUndoRedoCommand;
import pizzaaxx.bteconosur.WorldEdit.Shortcuts;
import pizzaaxx.bteconosur.WorldEdit.WorldEditHandler;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.dv8tion.jda.api.interactions.commands.Command.Type.USER;

public class BTEConoSur extends JavaPlugin implements Prefixable {

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

    private final PostsRegistry postsRegistry = new PostsRegistry(this);

    public PostsRegistry getPostsRegistry() {
        return postsRegistry;
    }

    private final LinksRegistry linksRegistry = new LinksRegistry(this);

    public LinksRegistry getLinksRegistry() {
        return linksRegistry;
    }

    private CustomCommandsManager customCommandsManager = new CustomCommandsManager(this);

    public CustomCommandsManager getCustomCommandsManager() {
        return customCommandsManager;
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
        regionListenersHandler.registerEnter(
                input -> input.matches("project_[a-z]{6}"),
                new ActionBarListener(this)
        );

        GetCommand getCommand = new GetCommand(this);

        ProjectsCommand projectsCommand = new ProjectsCommand(this);

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
                projectsCommand
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
        } catch (SQLException e) {
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

        // --- POSTS ---
        this.log("Starting posts registry...");
        try {
            postsRegistry.init();
        } catch (SQLException e) {
            this.error("Plugin starting stopped. Posts registry startup failed.");
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

        LinkCommand linkCommand = new LinkCommand(this);

        // --- DISCORD ---
        Configuration discordConfig = new Configuration(this, "discord/token");
        String token = discordConfig.getString("token");

        JDABuilder jdaBuilder = JDABuilder.createDefault(token);
        HelpCommand helpCommand = new HelpCommand(this);

        jdaBuilder.enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.DIRECT_MESSAGES
        );
        jdaBuilder.addEventListeners(
                linkCommand,
                new ProjectCreationRequestListener(this),
                chatHandler,
                new CreateCityCommand(this),
                new PostsListener(this),
                new ProjectPostCommand(this),
                helpCommand,
                discordHandler,
                new CityCommand(this),
                new ScoreboardCommand(this),
                new PlayerCommand(this)
        );
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
        getCommand("drawPolygon").setExecutor(new DrawPolygonCommand(this));
        getCommand("project").setExecutor(projectsCommand);
        getCommand("chat").setExecutor(new ChatCommand(this));
        getCommand("nickname").setExecutor(new NicknameCommand(this));
        getCommand("runnableCommand").setExecutor(customCommandsManager);

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
            ChatManager chatManager = this.getPlayerRegistry().get(player.getUniqueId()).getChatManager();
            try {
                Chat chat = chatManager.getCurrentChat();
                chat.addPlayer(player.getUniqueId());
            } catch (SQLException e) {
                this.warn("Problem with Chat Manager: " + player.getUniqueId());
            }
        }
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
}

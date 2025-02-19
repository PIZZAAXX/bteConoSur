package pizzaaxx.bteconosur.Discord.Link;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Discord.SlashCommands.SlashCommandContainer;
import pizzaaxx.bteconosur.Player.Managers.DiscordManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static pizzaaxx.bteconosur.Utils.StringUtils.DIGITS;
import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class LinkCommand extends ListenerAdapter implements CommandExecutor, Prefixable, SlashCommandContainer {

    private final BTEConoSur plugin;

    public LinkCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    private final Map<String, String> discordCodes = new HashMap<>();
    private final Map<String, UUID> minecraftCodes = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "link": {

                OptionMapping option = event.getOption("código");
                if (option != null) {
                    String code = option.getAsString();
                    if (!minecraftCodes.containsKey(code)) {
                        DiscordUtils.respondError(event, "El código introducido no existe.");
                        return;
                    }

                    ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(minecraftCodes.get(code));
                    serverPlayer.getDiscordManager().link(event.getUser().getId());
                    DiscordUtils.respondSuccess(event, "Cuenta conectada exitosamente a la cuenta de Minecraft \"" + serverPlayer.getName() + "\".");
                } else {
                    String code = StringUtils.generateCode(6, discordCodes.keySet(), DIGITS, LOWER_CASE);

                    EmbedBuilder linkEmbed = new EmbedBuilder();
                    linkEmbed.setColor(new Color(0, 171, 238));
                    linkEmbed.setTitle("Tu código es \"" + code + "\".");
                    linkEmbed.setDescription("Usa `/link [código]` en Minecraft para terminar de conectar tus cuentas.");

                    event.getUser().openPrivateChannel().queue(
                            channel -> channel.sendMessageEmbeds(linkEmbed.build()).queue()
                    );

                    DiscordUtils.respondSuccess(event, "Código enviado a tus mensajes privados.");

                    discordCodes.values().remove(event.getUser().getName());
                    discordCodes.put(code, event.getUser().getId());
                    BukkitRunnable runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            discordCodes.remove(code);
                        }
                    };
                    runnable.runTaskLaterAsynchronously(plugin, 6000);
                }
                break;
            }
            case "unlink": {
                try {
                    if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                        DiscordUtils.respondError(event, "Tu cuenta de Discord no está conectada a ninguna cuenta de Minecraft.");
                        return;
                    }

                    ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));
                    DiscordManager discordManager = s.getDiscordManager();
                    discordManager.unlink();

                    DiscordUtils.respondSuccess(event, "Cuenta desconectada exitosamente.");

                } catch (SQLException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                }
                break;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(p.getUniqueId());

        switch (command.getName()) {
            case "link": {

                if (args.length > 0) {
                    String code = args[0];

                    if (!discordCodes.containsKey(code)) {
                        p.sendMessage(this.getPrefix() + "El código introducido no existe.");
                        return true;
                    }

                    DiscordManager discordManager = serverPlayer.getDiscordManager();
                    discordManager.link(discordCodes.get(code));
                    p.sendMessage(this.getPrefix() + "Cuenta conectada exitosamente a Discord.");
                } else {
                    String code = StringUtils.generateCode(6, minecraftCodes.keySet(), DIGITS, LOWER_CASE);
                    p.sendMessage(this.getPrefix() + "Tu código es §a" + code + "§f. Usa §a/link [código]§f en Discord para terminar de conectar tus cuentas.");
                    minecraftCodes.values().remove(p.getUniqueId());
                    minecraftCodes.put(code, p.getUniqueId());
                    BukkitRunnable runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            minecraftCodes.remove(code);
                        }
                    };
                    runnable.runTaskLaterAsynchronously(plugin, 6000);
                }

                break;
            }
            case "unlink": {
                try {
                    DiscordManager discordManager = serverPlayer.getDiscordManager();
                    if (!discordManager.isLinked()) {
                        p.sendMessage(this.getPrefix() + "Tu cuenta de Minecraft no está conectada a ninguna cuenta de Discord.");
                        return true;
                    }

                    discordManager.unlink();

                    p.sendMessage(this.getPrefix() + "Cuenta desconectada exitosamente.");
                } catch (SQLException e) {
                    p.sendMessage(this.getPrefix() + "Ha ocurrido un error en la base de datos.");
                }
                break;
            }
        }
        return true;
    }

    @Override
    public String getPrefix() {
        return "§f[§bDISCORD§f] §7>> §f";
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {
                Commands.slash("link", "Conecta tu cuenta de Minecraft y tu cuenta de Discord.")
                        .addOption(
                                OptionType.STRING,
                        "código",
                        "El código obtenido en Minecraft."
                ).setNameLocalization(DiscordLocale.SPANISH, "conectar"),
                Commands.slash("unlink", "Desconecta de cuenta de Discord de tu cuenta de Minecraft.")
                        .setNameLocalization(DiscordLocale.SPANISH, "desconectar")
        };
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

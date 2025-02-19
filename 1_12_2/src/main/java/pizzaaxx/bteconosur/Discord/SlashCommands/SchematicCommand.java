package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Utils.DiscordUtils;

import java.awt.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class SchematicCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public SchematicCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("schematic")) {

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Conecta tu cuenta de Discord para usar este comando.");
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            if (s.getBuilderRank() != ServerPlayer.BuilderRank.BUILDER) {
                DiscordUtils.respondError(event, "Solo jugadores con rango de constructor o mayor pueden descargar schematics.");
                return;
            }

            OptionMapping nameMapping = event.getOption("nombre");
            assert nameMapping != null;
            String name = nameMapping.getAsString();

            if (name.contains("../") || name.contains("/")) {
                DiscordUtils.respondError(event, "No puedes hacer esto.");
                return;
            }

            Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
            File schematic = new File(worldEdit.getDataFolder(), "schematics/" + name + ".schematic");

            if (!schematic.exists()) {
                DiscordUtils.respondError(event, "El schematic introducido no existe.");
                return;
            }

            event.getUser().openPrivateChannel().queue(
                    channel -> channel.sendFiles(FileUpload.fromData(schematic)).queue(
                            msg -> event.replyEmbeds(
                                    DiscordUtils.fastEmbed(Color.GREEN, "Se ha enviado el schematics a tus mensajes directos.")
                            ).setComponents(ActionRow.of(
                                    Button.of(
                                            ButtonStyle.LINK,
                                            msg.getJumpUrl(),
                                            "Ver archivo"
                                    )
                            )).queue(
                                    interactionHook -> interactionHook.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                            )
                    )
            );

        }

    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {
                Commands.slash("schematic", "Obtén un schematic del servidor.")
                        .addOption(
                                OptionType.STRING,
                                "nombre",
                                "El nombre del schematic que quieres descargar.",
                                true
                        )
        };
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

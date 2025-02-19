package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

import java.awt.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class ModsCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public ModsCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("mods")) {
            event.replyEmbeds(
                            new EmbedBuilder()
                                    .setColor(Color.GREEN)
                                    .setTitle("Modpack")
                                    .setDescription("Estos son los *mods* que usamos en el servidor. Si necesitas ayuda instalándolos, ve <#942838899863081010>.")
                                    .build()
                    )
                    .setFiles(
                            FileUpload.fromData(new File(plugin.getDataFolder(), "modsBTECS.zip"), "mods.zip")
                    )
                    .addActionRow(
                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                    )
                    .queue(
                            msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                    );
        }
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash("mods", "Obtén los mods del servidor.")};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}

package pizzaaxx.bteconosur.Discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Discord.SlashCommands.SlashCommandContainer;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.util.Map;

public class DiscordHandler extends ListenerAdapter {

    public Button getDeleteButton(@NotNull User user) {
        return Button.of(
                ButtonStyle.DANGER,
                "deleteButton?user=" + user.getId(),
                "Eliminar",
                Emoji.fromUnicode("U+1F5D1")
        );
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        assert id != null;
        if (id.startsWith("deleteButton")) {
            Map<String, String> query = StringUtils.getQuery(id.split("\\?")[1]);
            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "No puedes usar esto.");
                return;
            }
            event.getMessage().delete().queue();
        }
    }

    public void checkCommand(@NotNull SlashCommandContainer container) {

        CommandData[] data = container.getCommandData();

        container.getJDA().retrieveCommands().queue(
                commands -> {

                    boolean found = false;
                    for (Command command : commands) {
                        for (CommandData c2 : data) {
                            if (this.compareCommands(command, c2)) {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        for (CommandData command : data) {
                            container.getJDA().upsertCommand(command).queue();
                        }
                    }
                }
        );
    }

    public boolean compareCommands(@NotNull Command command, @NotNull CommandData data) {

        if (command.isGuildOnly() != data.isGuildOnly()) {
            return false;
        }

        if (command.isNSFW() != data.isNSFW()) {
            return false;
        }

        if (command.getType() != data.getType()) {
            return false;
        }

        if (!command.getName().equals(data.getName())) {
            return false;
        }

        if (command.getType() == Command.Type.SLASH) {

            SlashCommandData slashData = (SlashCommandData) data;

            if (!command.getDescription().equals(slashData.getDescription())) {
                return false;
            }

            boolean equalsGroups = true;
            boolean equalsSubcommands = true;
            boolean equalsOptions = true;

            for (Command.SubcommandGroup group1 : command.getSubcommandGroups()) {
                boolean found = false;
                for (SubcommandGroupData group2Data : slashData.getSubcommandGroups()) {
                    Command.SubcommandGroup group2 = new Command.SubcommandGroup(command, group2Data.toData());

                    if (group1.equals(group2)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    equalsGroups = false;
                    break;
                }
            }

            for (Command.Subcommand subcommand1 : command.getSubcommands()) {
                boolean found = false;
                for (SubcommandData subcommand2Data : slashData.getSubcommands()) {
                    Command.Subcommand subcommand2 = new Command.Subcommand(command, subcommand2Data.toData());

                    if (subcommand1.equals(subcommand2)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    equalsSubcommands = false;
                    break;
                }
            }

            for (Command.Option option1 : command.getOptions()) {

                boolean found = false;
                for (OptionData option2Data : slashData.getOptions()) {
                    Command.Option option2 = new Command.Option(option2Data.toData());

                    if (option1.equals(option2)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    equalsOptions = false;
                    break;
                }

            }



            return equalsGroups && equalsSubcommands && equalsOptions;

        }

        return true;

    }

}

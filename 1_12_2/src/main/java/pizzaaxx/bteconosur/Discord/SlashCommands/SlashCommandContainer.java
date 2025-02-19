package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommandContainer {

    CommandData[] getCommandData();

    JDA getJDA();

}

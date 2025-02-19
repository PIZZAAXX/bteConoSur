package pizzaaxx.bteconosur.Discord.FuzzyMatching;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface FuzzyMatchCondition {

    boolean validate(MessageReceivedEvent event, String message);

}

package pizzaaxx.bteconosur.Discord.FuzzyMatching;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface FuzzyMatcher {

    void onFuzzyMatch(String message, String match, MessageReceivedEvent event);

}

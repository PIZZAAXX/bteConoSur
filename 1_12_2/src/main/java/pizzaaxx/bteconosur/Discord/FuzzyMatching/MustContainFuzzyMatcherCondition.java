package pizzaaxx.bteconosur.Discord.FuzzyMatching;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MustContainFuzzyMatcherCondition implements FuzzyMatchCondition {

    private final String[] strings;

    public MustContainFuzzyMatcherCondition(String... strings) {
        this.strings = strings;
    }

    @Override
    public boolean validate(MessageReceivedEvent event, String message) {
        for (String string : strings) if (!message.contains(string)) return false;
        return true;
    }
}

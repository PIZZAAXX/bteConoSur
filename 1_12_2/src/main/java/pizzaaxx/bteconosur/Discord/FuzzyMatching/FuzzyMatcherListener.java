package pizzaaxx.bteconosur.Discord.FuzzyMatching;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuzzyMatcherListener extends ListenerAdapter {

    public enum MatchingMethod {
        COMPLETE, PARTIAL
    }

    private final BTEConoSur plugin;
    private final Map<String[], Pair<Pair<MatchingMethod, Integer>, Pair<List<FuzzyMatchCondition>, FuzzyMatcher>>> map = new HashMap<>();

    public FuzzyMatcherListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void register(String string, MatchingMethod method, Integer maxDistance, FuzzyMatcher matcher, FuzzyMatchCondition... conditions) {
        Pair<MatchingMethod, Integer> p1 = new Pair<>(method, maxDistance);
        Pair<List<FuzzyMatchCondition>, FuzzyMatcher> p2 = new Pair<>(Arrays.asList(conditions), matcher);
        map.put(new String[] {string}, new Pair<>(p1, p2));
    }

    public void register(String[] strings, MatchingMethod method, Integer maxDistance, FuzzyMatcher matcher, FuzzyMatchCondition... conditions) {
        Pair<MatchingMethod, Integer> p1 = new Pair<>(method, maxDistance);
        Pair<List<FuzzyMatchCondition>, FuzzyMatcher> p2 = new Pair<>(Arrays.asList(conditions), matcher);
        map.put(strings, new Pair<>(p1, p2));
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay().toLowerCase();
        for (Map.Entry<String[], Pair<Pair<MatchingMethod, Integer>, Pair<List<FuzzyMatchCondition>, FuzzyMatcher>>> entry : map.entrySet()) {
            MatchingMethod method = entry.getValue().getKey().getKey();
            int maxDistance = entry.getValue().getKey().getValue();
            boolean found = false;
            String match = message;
            for (String string : entry.getKey()) {
                int distance;
                if (method == MatchingMethod.COMPLETE) {
                    distance = plugin.getFuzzyMatcher().getDistance(string, message);
                    match = message;
                } else {
                    distance = plugin.getFuzzyMatcher().getMinimumDistance(message, string);
                    match = plugin.getFuzzyMatcher().getMinimumDistanceMatch(message, string);
                }

                if (distance <= maxDistance) {
                    found = true;
                    break;
                }
            }

            if (found) {
                for (FuzzyMatchCondition condition : entry.getValue().getValue().getKey()) {
                    if (!condition.validate(event, message)) {
                        return;
                    }
                }

                entry.getValue().getValue().getValue().onFuzzyMatch(message, match, event);
            }
        }
    }
}

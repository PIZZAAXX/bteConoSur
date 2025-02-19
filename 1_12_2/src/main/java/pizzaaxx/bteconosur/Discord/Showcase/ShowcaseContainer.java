package pizzaaxx.bteconosur.Discord.Showcase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BTEConoSur;

import java.util.Set;
import java.util.UUID;

public interface ShowcaseContainer {

    static ShowcaseContainer getFromData(BTEConoSur plugin, String id, @NotNull String type) {
        switch (type) {
            case "project":
                return plugin.getProjectRegistry().get(id);
            case "finished":
                return plugin.getFinishedProjectsRegistry().get(id);
            default:
                return plugin.getBuildEventsRegistry().get(id);
        }
    }

    String getOptionName();

    @Nullable
    String getOptionDescription();

    boolean isMember(UUID uuid);

    Set<String> getCities();

}

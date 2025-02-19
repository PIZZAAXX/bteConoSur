package pizzaaxx.bteconosur.Projects.RegionSelectors;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Projects.Project;

import java.util.UUID;

public class NonMemberProjectSelector implements ProjectRegionSelector {

    private final UUID uuid;

    public NonMemberProjectSelector(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean applies(@NotNull Project project) {
        return !project.getAllMembers().contains(uuid);
    }
}

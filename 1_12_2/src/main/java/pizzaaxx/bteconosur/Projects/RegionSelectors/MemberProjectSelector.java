package pizzaaxx.bteconosur.Projects.RegionSelectors;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Projects.Project;

import java.util.UUID;

public class MemberProjectSelector implements ProjectRegionSelector {

    private final UUID uuid;

    public MemberProjectSelector(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean applies(@NotNull Project project) {
        return project.getAllMembers().contains(uuid);
    }
}

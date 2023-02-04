package pizzaaxx.bteconosur.Projects.RegionSelectors;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Projects.Project;

public class NotClaimedProjectSelector implements ProjectRegionSelector {
    @Override
    public boolean applies(@NotNull Project project) {
        return !project.isClaimed();
    }
}

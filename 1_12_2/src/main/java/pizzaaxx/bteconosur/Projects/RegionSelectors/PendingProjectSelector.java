package pizzaaxx.bteconosur.Projects.RegionSelectors;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Projects.Project;

public class PendingProjectSelector implements ProjectRegionSelector {

    private final boolean pending;

    public PendingProjectSelector(boolean pending) {
        this.pending = pending;
    }

    @Override
    public boolean applies(@NotNull Project project) {
        return project.isPending() == pending;
    }
}

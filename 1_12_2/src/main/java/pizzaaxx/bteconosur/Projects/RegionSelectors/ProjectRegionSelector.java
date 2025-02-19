package pizzaaxx.bteconosur.Projects.RegionSelectors;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Projects.Project;

public interface ProjectRegionSelector {

    boolean applies(@NotNull Project project);

}

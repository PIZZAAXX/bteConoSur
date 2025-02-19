package pizzaaxx.bteconosur.Projects.RegionSelectors;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Projects.Project;

import java.util.UUID;

public class OwnerProjectSelector implements ProjectRegionSelector {

    private final UUID owner;

    public OwnerProjectSelector(UUID owner) {
        this.owner = owner;
    }

    @Override
    public boolean applies(@NotNull Project project) {
        return project.getOwner() != null && project.getOwner().equals(owner);
    }
}

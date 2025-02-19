package pizzaaxx.bteconosur.Projects;

import com.sk89q.worldedit.BlockVector2D;
import org.bukkit.Location;
import pizzaaxx.bteconosur.Countries.Country;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProjectWrapper {

    String getId();

    UUID getOwner();

    Set<UUID> getMembers();

    Country getCountry();

    Set<String> getCities();

    ProjectTag getTag();

    ProjectType getType();

    int getPoints();

    List<BlockVector2D> getRegionPoints();

    String getDisplayName();

    Location getTeleportLocation();

    boolean isClaimed();
}

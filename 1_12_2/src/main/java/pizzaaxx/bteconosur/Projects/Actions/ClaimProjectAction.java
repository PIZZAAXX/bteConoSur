package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Cities.City;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.SatMapHandler;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

public class ClaimProjectAction {

    private final BTEConoSur plugin;
    private final Project project;
    private final UUID owner;

    public ClaimProjectAction(BTEConoSur plugin, Project project, UUID owner) {
        this.plugin = plugin;
        this.project = project;
        this.owner = owner;
    }

    public void execute() throws SQLException, IOException {
        new SetOwnerProjectAction(plugin, project, owner).execute();

        plugin.getPlayerRegistry().get(owner).getProjectManager().addProject(project);

        plugin.getTerramapHandler().deletePolygon(project.getId());
        List<Coords2D> coords = new ArrayList<>();
        for (BlockVector2D vector2D : project.getRegion().getPoints()) {
            coords.add(new Coords2D(plugin, vector2D));
        }
        plugin.getTerramapHandler().drawPolygon(coords, new Color(255, 200, 0), project.getId());

        DefaultDomain domain = new DefaultDomain();
        domain.addPlayer(owner);
        ProtectedPolygonalRegion region = project.getRegion();
        region.setMembers(domain);
        plugin.getRegionManager().addRegion(region);

        for (City city : project.getCitiesResolved()) {
            plugin.getScoreboardHandler().update(city);
        }

        project.getCountry().getLogsChannel().sendMessage(":inbox_tray: **" + plugin.getPlayerRegistry().get(owner).getName() + "** ha reclamado el proyecto `" + project.getId() + "`.").queue();
    }
}

package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public class AddMemberProjectAction {

    private final BTEConoSur plugin;

    private final Project project;
    private final UUID member;

    public AddMemberProjectAction(BTEConoSur plugin, Project project, UUID member) {
        this.plugin = plugin;
        this.project = project;
        this.member = member;
    }

    public void execute() throws SQLException, IOException {
        Set<UUID> members = project.getMembers();
        members.add(member);

        plugin.getSqlManager().update(
                "projects",
                new SQLValuesSet(
                        new SQLValue(
                                "members", members
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", project.getId()
                        )
                )
        ).execute();

        plugin.getPlayerRegistry().get(member).getProjectManager().addProject(project);

        DefaultDomain domain = new DefaultDomain(project.getRegion().getMembers());
        domain.addPlayer(member);
        ProtectedPolygonalRegion region = project.getRegion();
        region.setMembers(domain);
        plugin.getRegionManager().addRegion(region);

        project.update();

        project.getCountry().getLogsChannel().sendMessage(":pencil: **" + plugin.getPlayerRegistry().get(project.getOwner()).getName() + "** ha agregado a **" + plugin.getPlayerRegistry().get(member).getName() + "** al proyecto `" + project.getId() + "`.").queue();
    }
}

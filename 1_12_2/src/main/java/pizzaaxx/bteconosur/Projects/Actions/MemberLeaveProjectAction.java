package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public class MemberLeaveProjectAction {

    private final BTEConoSur plugin;
    private final Project project;
    private final UUID member;

    public MemberLeaveProjectAction(BTEConoSur plugin, Project project, UUID member) {
        this.plugin = plugin;
        this.project = project;
        this.member = member;
    }

    public void execute() throws SQLException, IOException {
        Set<UUID> members = project.getMembers();
        members.remove(member);

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

        ServerPlayer target = plugin.getPlayerRegistry().get(member);

        target.getProjectManager().removeProject(project);

        DefaultDomain domain = new DefaultDomain(project.getRegion().getMembers());
        domain.removePlayer(member);
        ProtectedPolygonalRegion region = project.getRegion();
        region.setMembers(domain);
        plugin.getRegionManager().addRegion(region);

        project.update();
        project.getCountry().getLogsChannel().sendMessage(":outbox_tray: **" + target.getName() + "** ha abandonado el proyecto `" + project.getId() + "`.").queue();
    }

}

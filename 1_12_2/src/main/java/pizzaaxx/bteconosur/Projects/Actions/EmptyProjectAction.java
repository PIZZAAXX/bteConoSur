package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;

public class EmptyProjectAction {

    private final BTEConoSur plugin;
    private final Project project;

    public EmptyProjectAction(BTEConoSur plugin, Project project) {
        this.plugin = plugin;
        this.project = project;
    }

    public void execute() throws SQLException, IOException {

        for (UUID memberUUID : project.getAllMembers()) {
            ServerPlayer member = plugin.getPlayerRegistry().get(memberUUID);
            member.getProjectManager().removeProject(project);
        }

        plugin.getSqlManager().update(
                "projects",
                new SQLValuesSet(
                        new SQLValue(
                                "owner", null
                        ),
                        new SQLValue(
                                "members", new HashSet<>()
                        ),
                        new SQLValue(
                                "pending", null
                        ),
                        new SQLValue(
                                "name", null
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", project.getId()
                        )
                )
        ).execute();

        project.update();

        ProtectedRegion region = project.getRegion();
        region.setMembers(new DefaultDomain());
        plugin.getRegionManager().addRegion(region);

        project.getCountry().getLogsChannel().sendMessage(":book: El proyecto `" + project.getId() + "` está disponible de nuevo.").queue();

        plugin.getSqlManager().delete(
                "project_join_requests",
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "project_id", "=", project.getId()
                        )
                )
        ).execute();
    }
}

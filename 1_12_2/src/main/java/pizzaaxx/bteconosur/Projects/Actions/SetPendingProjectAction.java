package pizzaaxx.bteconosur.Projects.Actions;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLExpression;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class SetPendingProjectAction {

    private final BTEConoSur plugin;

    private final Project project;
    private final boolean pending;

    public SetPendingProjectAction(BTEConoSur plugin, Project project, boolean pending) {
        this.plugin = plugin;
        this.project = project;
        this.pending = pending;
    }

    public void execute() throws SQLException, IOException {

        plugin.getSqlManager().update(
                "projects",
                new SQLValuesSet(
                        new SQLValue(
                                "pending", (pending ? new SQLExpression("CURRENT_TIMESTAMP") : null)
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", project.getId()
                        )
                )
        ).execute();

        ProtectedRegion region = project.getRegion();
        DefaultDomain domain = new DefaultDomain();
        if (!pending) {
            for (UUID memberUUID : project.getAllMembers()) {
                domain.addPlayer(memberUUID);
            }
        }
        region.setMembers(domain);
        plugin.getRegionManager().addRegion(region);

        project.update();

        if (pending) {
            project.getCountry().getLogsChannel().sendMessage(":lock: El proyecto `" + project.getId() + "` ha sido marcado como terminado.").queue();
        }
    }

}

package pizzaaxx.bteconosur.Projects.Actions;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.SQLException;
import java.util.UUID;

public class SetOwnerProjectAction {

    private final BTEConoSur plugin;
    private final Project project;
    private final UUID owner;

    public SetOwnerProjectAction(BTEConoSur plugin, Project project, UUID owner) {
        this.plugin = plugin;
        this.project = project;
        this.owner = owner;
    }

    public void execute() throws SQLException {
        plugin.getSqlManager().update(
                "project",
                new SQLValuesSet(
                        new SQLValue(
                                "owner", owner
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", project.getId()
                        )
                )
        ).execute();
    }
}

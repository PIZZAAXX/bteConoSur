package pizzaaxx.bteconosur.Projects.Actions;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.IOException;
import java.sql.SQLException;

public class SetPointsProjectAction {

    private final BTEConoSur plugin;

    private final Project project;
    private final int points;

    public SetPointsProjectAction(BTEConoSur plugin, Project project, int points) {
        this.plugin = plugin;
        this.project = project;
        this.points = points;
    }

    public void execute() throws SQLException, IOException {

        plugin.getSqlManager().update(
                "projects",
                new SQLValuesSet(
                        new SQLValue(
                                "points", points
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", project.getId()
                        )
                )
        ).execute();

        project.update();
    }

}

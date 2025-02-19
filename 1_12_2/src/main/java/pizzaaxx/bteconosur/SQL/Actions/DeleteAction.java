package pizzaaxx.bteconosur.SQL.Actions;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;

import java.sql.SQLException;

public class DeleteAction {

    private final BTEConoSur plugin;
    private final String tableName;
    private final SQLConditionSet conditions;

    public DeleteAction(BTEConoSur plugin, String tableName, SQLConditionSet conditions) {
        this.plugin = plugin;
        this.tableName = tableName;
        this.conditions = conditions;
    }

    public void addCondition(SQLCondition condition) {
        conditions.addCondition(condition);
    }

    public void execute() throws SQLException {

        if (conditions.isEmpty()) {
            throw new SQLException("No condition was set.");
        }

        String query = "DELETE FROM " + tableName + " WHERE " + conditions.getConditionSetString();

        plugin.getSqlManager().getConnection().prepareStatement(query).executeUpdate();
    }
}

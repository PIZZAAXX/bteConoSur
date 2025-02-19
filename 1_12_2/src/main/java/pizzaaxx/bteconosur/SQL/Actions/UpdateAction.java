package pizzaaxx.bteconosur.SQL.Actions;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.SQLException;

public class UpdateAction {

    // UPDATE table_name SET name1 = value1, name2 = value2 WHERE condition;

    private final BTEConoSur plugin;
    private final String tableName;
    private final SQLValuesSet values;
    private final SQLConditionSet conditions;

    public UpdateAction(BTEConoSur plugin, String tableName, SQLValuesSet values, SQLConditionSet conditions) {
        this.plugin = plugin;
        this.tableName = tableName;
        this.values = values;
        this.conditions = conditions;
    }

    public void addValue(SQLValue value) {
        values.addValue(value);
    }

    public void addCondition(SQLCondition condition) {
        conditions.addCondition(condition);
    }

    public void execute() throws SQLException {

        if (conditions.isEmpty()) {
            throw new SQLException("No condition was set.");
        }

        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(tableName).append(" SET ");

        int size = values.getValues().size();
        int counter = 1;
        for (SQLValue value : values.getValues()) {
            query.append(value.getString());
            if (counter < size) {
                query.append(", ");
            }
            counter++;
        }

        query.append(" WHERE ").append(conditions.getConditionSetString());
        plugin.getSqlManager().getConnection().prepareStatement(query.toString()).executeUpdate();
    }

}

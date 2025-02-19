package pizzaaxx.bteconosur.SQL.Actions;

import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SelectAction {

    private final BTEConoSur plugin;
    private final String tableName;
    private final SQLColumnSet columns;
    private final SQLConditionSet conditions;
    private final StringBuilder additionalText;
    private final SQLOrderSet order;

    public SelectAction(BTEConoSur plugin, String tableName, SQLColumnSet columns, SQLConditionSet conditions, @Nullable SQLOrderSet order) {
        this.plugin = plugin;
        this.tableName = tableName;
        this.columns = columns;
        this.conditions = conditions;
        this.order = order;
        this.additionalText = new StringBuilder();
    }

    public void addCondition(SQLCondition condition) {
        this.conditions.addCondition(condition);
    }

    public void addColumn(String column) {
        this.columns.addColumn(column);
    }

    public SelectAction addText(String text) {
        additionalText.append(text);
        return this;
    }

    public ResultSet retrieve() throws SQLException {
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(columns.getString()).append(" FROM ").append(tableName);
        if (!conditions.isEmpty()) {
            query.append(" WHERE ").append(conditions.getConditionSetString());
        }

        if (order != null) {
            query.append(" ORDER BY ").append(order.getString());
        }

        query.append(additionalText);
        try {
            return plugin.getSqlManager().getConnection().createStatement().executeQuery(query.toString());
        } catch (SQLException e) {
            throw new SQLException(query.toString());
        }
    }
}

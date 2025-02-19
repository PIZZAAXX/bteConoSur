package pizzaaxx.bteconosur.SQL.Actions;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InsertAction {

    private final BTEConoSur plugin;
    private final String tableName;
    private final SQLValuesSet values;

    public InsertAction(BTEConoSur plugin, String tableName, SQLValuesSet values) {
        this.plugin = plugin;
        this.tableName = tableName;
        this.values = values;
    }

    public void addValue(SQLValue value) {
        values.addValue(value);
    }

    public void execute() throws SQLException {
        StringBuilder query = new StringBuilder("INSERT INTO ").append(tableName);
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (SQLValue value : this.values.getValues()) {
            columns.add(value.getColumn());
            values.add(value.getValueString());
        }

        query.append("(").append(String.join(",", columns)).append(")");
        query.append(" VALUES ").append("(").append(String.join(",", values)).append(")");
        plugin.getSqlManager().getConnection().prepareStatement(query.toString()).executeUpdate();
    }
}

package pizzaaxx.bteconosur.SQL.Conditions;

import pizzaaxx.bteconosur.SQL.SQLParser;

public class SQLJSONArrayCondition implements SQLCondition {

    private final String column;
    private final Object value;

    public SQLJSONArrayCondition(String column, Object value) {
        this.column = column;
        this.value = value;
    }

    @Override
    public String getString() {
        return "JSON_CONTAINS(" + column + ", '" + SQLParser.getString(value, true) + "')";
    }
}

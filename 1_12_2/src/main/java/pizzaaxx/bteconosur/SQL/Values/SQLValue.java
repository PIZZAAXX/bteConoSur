package pizzaaxx.bteconosur.SQL.Values;

import pizzaaxx.bteconosur.SQL.SQLParser;

public class SQLValue {

    private final String column;
    private final Object value;

    public SQLValue(String column, Object value) {
        this.column = column;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public Object getValue() {
        return value;
    }

    public String getValueString() {
        return SQLParser.getString(value);
    }

    public String getString() {
        return column + " = " + SQLParser.getString(value);
    }
}

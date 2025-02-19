package pizzaaxx.bteconosur.SQL.Conditions;

import pizzaaxx.bteconosur.SQL.SQLParser;

public class SQLBetweenCondition implements SQLCondition {

    private final String column;
    private final Object from;
    private final Object to;

    public SQLBetweenCondition(String column, Object from, Object to) {
        this.column = column;
        this.from = from;
        this.to = to;
    }

    @Override
    public String getString() {
        return column + " BETWEEN " + SQLParser.getString(from) + " AND " + SQLParser.getString(to);
    }
}

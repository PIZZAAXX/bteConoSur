package pizzaaxx.bteconosur.SQL.Values;

import pizzaaxx.bteconosur.SQL.JSONParsable;

public class SQLExpression implements JSONParsable {

    private final String expression;

    public SQLExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public String getJSON(boolean insideJSON) {
        return expression;
    }
}

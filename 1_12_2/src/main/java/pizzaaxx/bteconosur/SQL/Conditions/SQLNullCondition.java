package pizzaaxx.bteconosur.SQL.Conditions;

public class SQLNullCondition implements SQLCondition {

    private final String column;
    private final boolean isNull;

    public SQLNullCondition(String column, boolean isNull) {
        this.column = column;
        this.isNull = isNull;
    }

    @Override
    public String getString() {
        return column + " " + (isNull ? "IS NULL" : "IS NOT NULL");
    }
}

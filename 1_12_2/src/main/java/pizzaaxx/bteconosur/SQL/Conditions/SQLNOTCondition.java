package pizzaaxx.bteconosur.SQL.Conditions;

public class SQLNOTCondition implements SQLCondition {

    private final SQLCondition condition;

    public SQLNOTCondition(SQLCondition condition) {
        this.condition = condition;
    }

    @Override
    public String getString() {
        return "NOT " + condition.getString();
    }
}

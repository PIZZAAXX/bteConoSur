package pizzaaxx.bteconosur.SQL.Conditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SQLORConditionSet implements SQLCondition, SQLConditionSet {

    private final Set<SQLCondition> conditions;

    public SQLORConditionSet(SQLCondition... conditions) {
        this.conditions = new HashSet<>(Arrays.asList(conditions));
    }

    public Set<SQLCondition> getConditions() {
        return conditions;
    }

    @Override
    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    @Override
    public void addCondition(SQLCondition condition) {
        conditions.add(condition);
    }

    @Override
    public String getString() {
        StringBuilder builder = new StringBuilder("(");

        int size = conditions.size();
        int counter = 1;
        for (SQLCondition condition : conditions) {
            builder.append(condition.getString());

            if (counter < size) {
                builder.append(" OR ");
            }
            counter++;
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String getConditionSetString() {
        StringBuilder builder = new StringBuilder();

        int size = conditions.size();
        int counter = 1;
        for (SQLCondition condition : conditions) {
            builder.append(condition.getString());

            if (counter < size) {
                builder.append(" OR ");
            }
            counter++;
        }

        return builder.toString();
    }
}

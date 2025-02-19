package pizzaaxx.bteconosur.SQL.Ordering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SQLOrderSet {

    private final List<SQLOrderExpression> orderExpressions = new ArrayList<>();

    public SQLOrderSet(SQLOrderExpression... expressions) {
        orderExpressions.addAll(Arrays.asList(expressions));
    }

    public void addExpression(SQLOrderExpression expression) {
        orderExpressions.add(expression);
    }

    public String getString() {
        StringBuilder builder = new StringBuilder();
        List<String> expressions = new ArrayList<>();
        for (SQLOrderExpression expression : orderExpressions) {
            expressions.add(expression.getString());
        }
        builder.append(String.join(", ", expressions));
        return builder.toString();
    }

}

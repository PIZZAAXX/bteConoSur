package pizzaaxx.bteconosur.SQL.Conditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SQLJSONContainsPathCondition implements SQLCondition {

    public enum Quantity {
        ALL, ONE
    }

    private final String column;
    private final Quantity quantity;
    private final Set<String> paths;

    public SQLJSONContainsPathCondition(
            String column,
            Quantity quantity,
            String... paths
    ) {
        this.column = column;
        this.quantity = quantity;
        this.paths = new HashSet<>(Arrays.asList(paths));
    }

    @Override
    public String getString() {
        return "JSON_CONTAINS_PATH(" + column + ", '" + quantity.toString().toLowerCase() + "', '" + String.join("', '", paths) + "')";
    }
}

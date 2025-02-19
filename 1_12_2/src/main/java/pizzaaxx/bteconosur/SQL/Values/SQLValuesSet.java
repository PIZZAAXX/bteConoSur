package pizzaaxx.bteconosur.SQL.Values;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SQLValuesSet {

    private final Set<SQLValue> values;

    public SQLValuesSet(SQLValue... values) {
        this.values = new HashSet<>(Arrays.asList(values));
    }

    public Set<SQLValue> getValues() {
        return values;
    }

    public void addValue(SQLValue value) {
        values.add(value);
    }
}

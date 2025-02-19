package pizzaaxx.bteconosur.SQL.Columns;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SQLColumnSet {

    private final List<String> columns;

    public SQLColumnSet(String... columns) {
        this.columns = Arrays.asList(columns);
    }

    public @NotNull String getString() {
        return String.join(", ", columns);
    }

    public void addColumn(String column) {
        columns.add(column);
    }
}

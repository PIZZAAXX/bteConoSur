package pizzaaxx.bteconosur.SQL.Conditions;

import pizzaaxx.bteconosur.SQL.SQLParser;

import java.util.Collection;

public class SQLContainedCondition<K> implements SQLCondition {

    private final String column;
    private final Collection<K> values;
    private final boolean contained;

    public SQLContainedCondition(String column, Collection<K> values, boolean contained) {
        this.column = column;
        this.values = values;
        this.contained = contained;
    }

    @Override
    public String getString() {
        if (values.isEmpty()) {
            if (contained) {
                return "1 = 2";
            } else {
                return "1 = 1";
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append(column).append((contained?" IN ":" NOT IN ")).append("(");
        int counter = 0;
        for (K value : values) {
            builder.append(SQLParser.getString(value));
            if (counter <  values.size() - 1) {
                builder.append(", ");
            }
            counter++;
        }
        builder.append(")");
        return builder.toString();
    }

}

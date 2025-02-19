package pizzaaxx.bteconosur.SQL.Conditions;

public class SQLLikeCondition implements SQLCondition {

    private final String column;
    private final boolean like;
    private final String pattern;

    public SQLLikeCondition(String column, boolean like, String pattern) {
        this.column = column;
        this.like = like;
        this.pattern = pattern;
    }

    @Override
    public String getString() {
        return column + (like ? " LIKE '" : " NOT LIKE '") + pattern + "'";
    }
}

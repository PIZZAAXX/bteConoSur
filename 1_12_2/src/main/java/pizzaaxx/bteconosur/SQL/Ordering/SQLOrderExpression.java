package pizzaaxx.bteconosur.SQL.Ordering;

public class SQLOrderExpression {

    public SQLOrderExpression(String column, Order order) {
        this.column = column;
        this.order = order;
    }

    public enum Order {
        ASC, DESC
    }

    private final String column;
    private final Order order;

    public String getString() {
        return column + " " + order.toString();
    }

}

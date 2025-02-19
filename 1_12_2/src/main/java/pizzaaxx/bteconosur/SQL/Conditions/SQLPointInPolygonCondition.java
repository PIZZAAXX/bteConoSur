package pizzaaxx.bteconosur.SQL.Conditions;

public class SQLPointInPolygonCondition implements SQLCondition {

    private final Double[] coordinates;
    private final String polygonColumn;

    public SQLPointInPolygonCondition(double x, double y, String polygonColumn) {
        this.coordinates = new Double[] {x, y};
        this.polygonColumn = polygonColumn;
    }

    @Override
    public String getString() {
        return "ST_CONTAINS(" + polygonColumn + ", PointFromText('POINT(" + coordinates[0] + " " + coordinates[1] + ")'))";
    }
}

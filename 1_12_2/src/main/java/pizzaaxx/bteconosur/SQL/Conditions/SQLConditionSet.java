package pizzaaxx.bteconosur.SQL.Conditions;

public interface SQLConditionSet {

    String getConditionSetString();

    void addCondition(SQLCondition condition);

    boolean isEmpty();
}

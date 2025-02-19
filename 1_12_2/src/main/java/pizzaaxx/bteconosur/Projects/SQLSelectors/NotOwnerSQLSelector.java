package pizzaaxx.bteconosur.Projects.SQLSelectors;

import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

import java.util.UUID;

public class NotOwnerSQLSelector implements ProjectSQLSelector {

    private final UUID owner;

    public NotOwnerSQLSelector(UUID owner) {
        this.owner = owner;
    }

    @Override
    public SQLCondition getCondition() {
        return new SQLOperatorCondition(
                "owner", "!=", owner
        );
    }
}

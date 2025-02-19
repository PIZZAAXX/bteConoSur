package pizzaaxx.bteconosur.Projects.SQLSelectors;

import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

import java.util.UUID;

public class OwnerSQLSelector implements ProjectSQLSelector {

    private final UUID owner;

    public OwnerSQLSelector(UUID owner) {
        this.owner = owner;
    }

    @Override
    public SQLCondition getCondition() {
        return new SQLOperatorCondition(
                "owner", "=", owner
        );
    }
}

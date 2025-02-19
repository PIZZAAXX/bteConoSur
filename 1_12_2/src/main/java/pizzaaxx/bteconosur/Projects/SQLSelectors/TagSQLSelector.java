package pizzaaxx.bteconosur.Projects.SQLSelectors;

import pizzaaxx.bteconosur.Projects.ProjectTag;
import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

public class TagSQLSelector implements ProjectSQLSelector {

    private final ProjectTag tag;

    public TagSQLSelector(ProjectTag tag) {
        this.tag = tag;
    }

    @Override
    public SQLCondition getCondition() {
        return new SQLOperatorCondition(
                "tag", "=", tag.toString()
        );
    }
}

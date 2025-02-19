package pizzaaxx.bteconosur.Projects.SQLSelectors;

import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.SQL.Conditions.SQLCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

public class CountrySQLSelector implements ProjectSQLSelector {

    private final Country country;

    public CountrySQLSelector(Country country) {
        this.country = country;
    }

    @Override
    public SQLCondition getCondition() {
        return new SQLOperatorCondition(
                "country", "=", country.getName()
        );
    }
}

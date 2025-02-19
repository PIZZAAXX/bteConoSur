package pizzaaxx.bteconosur.Countries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLORConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CountryManager {

    private final BTEConoSur plugin;
    private final Map<String, Country> countries = new HashMap<>();
    private final Map<String, String> abbreviations = new HashMap<>();
    public final Map<String, Country> countryChannels = new HashMap<>();
    public final Map<String, Country> guilds = new HashMap<>();
    public final Set<String> globalChannels = new HashSet<>();
    public final Set<String> projectForumChannels = new HashSet<>();

    public CountryManager(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException, JsonProcessingException {
        ResultSet set = plugin.getSqlManager().select(
                "countries",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            Country country = new Country(plugin, set);
            countries.put(set.getString("name"), country);
            abbreviations.put(set.getString("abbreviation"), set.getString("name"));
            countryChannels.put(set.getString("country_chat_id"), country);
            guilds.put(set.getString("guild_id"), country);
            globalChannels.add(set.getString("global_chat_id"));
            projectForumChannels.add(set.getString("projects_forum_channel_id"));
        }
    }

    public boolean exists(@NotNull String nameOrAbbreviation) {
        if (nameOrAbbreviation.length() == 2) {
            return abbreviations.containsKey(nameOrAbbreviation);
        } else {
            return countries.containsKey(nameOrAbbreviation);
        }
    }

    public Country get(@NotNull String nameOrAbbreviation) {
        if (nameOrAbbreviation.length() == 2) {
            if (abbreviations.containsKey(nameOrAbbreviation)) {
                return countries.get(abbreviations.get(nameOrAbbreviation));
            }
            return null;
        } else {
            return countries.get(nameOrAbbreviation);
        }
    }

    public boolean isInsideCountry(Location loc) {
        for (ProtectedRegion region : plugin.getRegionManager().getApplicableRegions(loc)) {
            if (region.getId().startsWith("country")) {
                return true;
            }
        }
        return false;
    }

    public Country getCountryAt(Location loc) {
        for (ProtectedRegion region : plugin.getRegionManager().getApplicableRegions(loc)) {
            if (region.getId().startsWith("country")) {
                // country_chile_1
                String name = region.getId().split("_")[1];
                return this.get(name);
            }
        }
        return null;
    }

    public List<Country> getAllCountries() {
        List<Map.Entry<String, Country>> entries = new ArrayList<>(countries.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        List<Country> result = new ArrayList<>();
        for (Map.Entry<String, Country> entry : entries) {
            result.add(entry.getValue());
        }
        return result;
    }
}

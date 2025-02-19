package pizzaaxx.bteconosur.Help;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class HelpRecord {

    private final BTEConoSur plugin;

    public enum Platform {
        MINECRAFT, DISCORD;
    }

    @NotNull
    @Contract("_, _, _ -> new")
    public static HelpRecord from(@NotNull BTEConoSur plugin, String path, @NotNull Platform platform) throws IllegalArgumentException, SQLException, JsonProcessingException {

        ResultSet set = plugin.getSqlManager().select(
                "commands",
                new SQLColumnSet("*"),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "platform", "=", platform.toString().toLowerCase()
                        ),
                        new SQLOperatorCondition(
                                "path", "=", path
                        )
                )
        ).retrieve();

        if (set.next()) {
            return new HelpRecord(plugin, set, platform);
        } else {
            throw new IllegalArgumentException();
        }

    }

    private final Platform platform;

    private final String path;

    private final String description;
    private final String commandUsage;
    private final String permission;
    private final List<String> aliases;
    private final List<String> examples;
    private final String note;
    private final LinkedHashMap<String, String> parameters;
    private final List<String> subcommands;
    private HelpRecord(BTEConoSur plugin, @NotNull ResultSet set, Platform platform) throws SQLException, JsonProcessingException {

        this.plugin = plugin;
        this.platform = platform;

        path = set.getString("path");
        description = set.getString("description");
        commandUsage = set.getString("command_usage");
        permission = set.getString("permission");

        String aliasesString = set.getString("aliases");
        aliases = (aliasesString != null ? plugin.getJSONMapper().readValue(aliasesString, ArrayList.class) : null);

        String examplesString = set.getString("examples");
        examples = (examplesString != null ? plugin.getJSONMapper().readValue(examplesString, ArrayList.class) : null);

        note = set.getString("note");

        String parametersString = set.getString("parameters");
        if (parametersString != null) {
            Map<String, Object> rawParametersMap = plugin.getJSONMapper().readValue(parametersString, HashMap.class);
            parameters = new LinkedHashMap<>();
            for (String key : rawParametersMap.keySet()) {
                parameters.put(key, rawParametersMap.get(key).toString());
            }
        } else {
            parameters = null;
        }

        String subcommandsString = set.getString("subcommands");
        subcommands = (subcommandsString != null ? plugin.getJSONMapper().readValue(subcommandsString, ArrayList.class) : null);
        if (subcommands != null) {
            Collections.sort(subcommands);
        }

    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public String getCommandUsage() {
        return commandUsage;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getExamples() {
        return examples;
    }

    public String getNote() {
        return note;
    }

    public LinkedHashMap<String, String> getParameters() {
        return parameters;
    }

    public List<String> getSubcommands() {
        return subcommands;
    }

    public List<String> getSubcommandsWithParameters() throws SQLException {
        List<String> result = new ArrayList<>();
        for (String subcommand : subcommands) {

            ResultSet set = plugin.getSqlManager().select(
                    "commands",
                    new SQLColumnSet("command_usage"),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "platform", "=", platform.toString().toLowerCase()
                            ),
                            new SQLOperatorCondition(
                                    "path", "=", path + " " + subcommand
                            )
                    )
            ).retrieve();

            if (set.next()) {
                result.add(set.getString("command_usage").split(path + " ", 2)[1]);
            }
        }
        return result;
    }
}

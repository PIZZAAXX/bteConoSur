package pizzaaxx.bteconosur.SQL;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Configuration.Configuration;
import pizzaaxx.bteconosur.SQL.Actions.DeleteAction;
import pizzaaxx.bteconosur.SQL.Actions.InsertAction;
import pizzaaxx.bteconosur.SQL.Actions.SelectAction;
import pizzaaxx.bteconosur.SQL.Actions.UpdateAction;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.UUIDUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SQLManager {

    private final BTEConoSur plugin;
    private Connection connection;

    private static final String AUTO_RECONNECT_PROPERTY = "?autoReconnect=true&useUnicode=yes";
    private static final String CLASS_NAME = "com.mysql.jdbc.Driver";

    public SQLManager(@NotNull BTEConoSur plugin) throws SQLException, ClassNotFoundException {
        this.plugin = plugin;

        Class.forName(CLASS_NAME);

        Configuration databaseConfig = new Configuration(plugin, "database");

        String url = databaseConfig.getString("url") + AUTO_RECONNECT_PROPERTY;

        connection = DriverManager.getConnection(
                url,
                databaseConfig.getString("username"),
                databaseConfig.getString("password")
        );

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Configuration databaseConfig = new Configuration(plugin, "database");

                String url = databaseConfig.getString("url") + AUTO_RECONNECT_PROPERTY;

                try {
                    connection = DriverManager.getConnection(
                            url,
                            databaseConfig.getString("username"),
                            databaseConfig.getString("password")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        runnable.runTaskTimer(plugin, 0, 18000);
    }

    public Connection getConnection() {
        return connection;
    }

    @CheckReturnValue
    public UpdateAction update(String tableName, SQLValuesSet values, SQLConditionSet conditions) {
        return new UpdateAction(plugin, tableName, values, conditions);
    }

    @CheckReturnValue
    public InsertAction insert(String tableName, SQLValuesSet values) {
        return new InsertAction(plugin, tableName, values);
    }

    @CheckReturnValue
    public SelectAction select(String tableName, SQLColumnSet columns, SQLConditionSet conditions) {
        return new SelectAction(plugin, tableName, columns, conditions, null);
    }

    @CheckReturnValue
    public SelectAction select(String tableName, SQLColumnSet columns, SQLConditionSet conditions, SQLOrderSet order) {
        return new SelectAction(plugin, tableName, columns, conditions, order);
    }

    @CheckReturnValue
    public DeleteAction delete(String tableName, SQLConditionSet conditions) {
        return new DeleteAction(plugin, tableName, conditions);
    }

    @Nullable
    public UUID getUUID(@NotNull ResultSet set, String column) throws SQLException, IOException {
        InputStream stream = set.getBinaryStream(column);
        if (stream != null) {
            return UUIDUtils.getFromInputStream(set.getBinaryStream(column));
        }
        return null;
    }

}

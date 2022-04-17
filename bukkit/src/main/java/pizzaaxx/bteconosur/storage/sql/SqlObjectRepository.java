package pizzaaxx.bteconosur.storage.sql;

import pizzaaxx.bteconosur.server.player.Identifiable;
import pizzaaxx.bteconosur.storage.ObjectRepository;
import pizzaaxx.bteconosur.storage.query.CompoundQuery;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SqlObjectRepository<O extends Identifiable> implements ObjectRepository<O> {

    private String fundamentalQuery = "SELECT * FROM ";
    private final SqlQueryInterpreter sqlQueryInterpreter = new SqlQueryInterpreter();

    private final Class<O> clazz;

    private final SqlQueryWrapper queries;
    private final SqlAdapterRegistry sqlAdapterRegistry;
    private  final SqlResponse sqlResponse;
    private final String table;

    public SqlObjectRepository(Class<O> clazz,
                               SqlQueryWrapper queries,
                               SqlAdapterRegistry sqlAdapterRegistry,
                               String table) {
        this.clazz = clazz;
        this.queries = queries;
        this.sqlAdapterRegistry = sqlAdapterRegistry;
        this.table = table;

        sqlResponse = new SqlResponse(sqlAdapterRegistry);
        fundamentalQuery = fundamentalQuery.concat(table);
    }

    @Override
    public void save(O object) {
        delete(object);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(
                        "INSERT INTO "
                ).append(table)
                .append(" (");

        SqlObjectAdapter<O>
                sqlObjectAdapter = sqlAdapterRegistry.get(object.getClass());

        QueryStatement queryStatement =
                sqlObjectAdapter.write(object);

        for (FieldStatement<?> field : queryStatement.fields()) {
            queryBuilder.append(field.getName())
                    .append(",");
        }

        queryBuilder.deleteCharAt(
                queryBuilder.length() - 1
        );

        for (int i = 0; i < queryStatement.fields().size(); i++) {
            queryBuilder.append("?");

            if (i < queryStatement.fields().size()) {
                queryBuilder.append(",");
            }
        }

        queries.preparedQuery(
                queryStatement, queryBuilder.toString()
        );

    }

    @Override
    public void delete(O object) {

        String deleteQuery = "DELETE FROM * "
                + table + " WHERE id = ?";

        queries.preparedQuery(
                QueryStatement.create()
                        .insert(FieldStatement.of("id", object.getId(), String.class)),
                deleteQuery
        );

    }


    @Override
    public boolean exists(O object) {
        ResultSet resultSet = queries.preparedQuery(
                QueryStatement.create()
                        .insert(FieldStatement.of("id", object.getId(), String.class)),
                fundamentalQuery.concat("WHERE id = ?")
        );

        try {
            return resultSet.first();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public CompletableFuture<O> query(CompoundQuery queries) {
        return CompletableFuture.supplyAsync(() -> {
            String conditions = sqlQueryInterpreter.interpret(queries);
            String query = fundamentalQuery + conditions;

            ResultSet resultSet =
                    this.queries.query(query);

            return sqlResponse.response(clazz, resultSet);
        });
    }

    @Override
    public CompletableFuture<List<O>> queryAll(CompoundQuery queries) {
        return CompletableFuture.supplyAsync(() -> {
            String conditions = sqlQueryInterpreter.interpret(queries);
            String query = fundamentalQuery + conditions;

            ResultSet resultSet =
                    this.queries.query(query);

            return sqlResponse.responseAll(clazz, resultSet);
        });
    }
}
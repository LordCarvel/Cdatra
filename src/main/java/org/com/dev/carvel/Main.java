package org.com.dev.carvel;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.mapper.ObjectMapper;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlExecutor;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.table.Table;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        Analysis analysis = new Analysis();

        List<ColumnDefinition> columns =
                analysis.analize(User.class);

        String tableName =
                analysis.analyzeTableName(User.class);

        SchemaBuilder schemaBuilder =
                new SchemaBuilder();

        Table table =
                schemaBuilder.build(
                        tableName,
                        columns
                );

        SqlGenerator sqlGenerator =
                new SqlGenerator();

        String createTableSql =
                sqlGenerator.createTable(table);

        DatabaseConnection databaseConnection =
                new DatabaseConnection();

        Connection connection =
                databaseConnection.connection(
                        "jdbc:h2:mem:cdatra",
                        "sa",
                        ""
                );

        SqlExecutor sqlExecutor =
                new SqlExecutor();

        sqlExecutor.execute(
                connection,
                createTableSql
        );

        User user1 =
                new User(
                        0,
                        "Carvel",
                        "carvel@gmail.com",
                        new Address(
                                "Some street",
                                67,
                                "Some city"
                        ),
                        "temporary"
                );

        User user2 =
                new User(
                        1,
                        "João",
                        "joao@gmail.com",
                        new Address(
                                "Another street",
                                20,
                                "Another city"
                        ),
                        "temporary2"
                );

        ValueAnalysis valueAnalysis =
                new ValueAnalysis();

        List<Row> user1Rows =
                valueAnalysis.analyze(user1);

        List<Row> user2Rows =
                valueAnalysis.analyze(user2);

        sqlExecutor.execute(
                connection,
                sqlGenerator.insert(
                        table,
                        user1Rows
                )
        );

        sqlExecutor.execute(
                connection,
                sqlGenerator.insert(
                        table,
                        user2Rows
                )
        );

        List<Row> updateRows =
                new ArrayList<>();

        updateRows.add(
                new Row(
                        "name",
                        "Carvel Novo"
                )
        );

        updateRows.add(
                new Row(
                        "user_email",
                        "novo@gmail.com"
                )
        );

        Row idRow =
                new Row(
                        "id",
                        1
                );

        String updateSql =
                sqlGenerator.update(
                        table,
                        updateRows,
                        idRow
                );

        System.out.println(updateSql);

        sqlExecutor.execute(
                connection,
                updateSql
        );

        String selectSql =
                sqlGenerator.selectAll(table);

        List<List<Row>> result =
                sqlExecutor.query(
                        connection,
                        selectSql
                );

        ObjectMapper objectMapper =
                new ObjectMapper();

        for (List<Row> record : result) {

            User loadedUser =
                    (User) objectMapper.map(
                            record,
                            User.class
                    );

            System.out.println(
                    loadedUser.getId()
                            + " | "
                            + loadedUser.getName()
                            + " | "
                            + loadedUser.getEmail()
            );
        }

        connection.close();
    }
}
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

        // 8. Extrai os valores do primeiro usuário
        ValueAnalysis valueAnalysis =
                new ValueAnalysis();

        List<Row> user1Rows =
                valueAnalysis.analyze(user1);

        String user1InsertSql =
                sqlGenerator.insert(
                        table,
                        user1Rows
                );

        sqlExecutor.execute(
                connection,
                user1InsertSql
        );

        List<Row> user2Rows =
                valueAnalysis.analyze(user2);

        String user2InsertSql =
                sqlGenerator.insert(
                        table,
                        user2Rows
                );

        sqlExecutor.execute(
                connection,
                user2InsertSql
        );

        String selectSql =
                sqlGenerator.selectAll(table);

        System.out.println(selectSql);

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

            Row idRow =
                    valueAnalysis.analyzeId(loadedUser);

            System.out.println(
                    idRow.getColumnName()
                            + " = "
                            + idRow.getValue()
            );
        }

        connection.close();
    }
}
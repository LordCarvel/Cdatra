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

        SchemaBuilder schemaBuilder = new SchemaBuilder();

        Table table =
                schemaBuilder.build("users", columns);

        SqlGenerator sqlGenerator = new SqlGenerator();

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

        User user = new User(
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

        ValueAnalysis valueAnalysis =
                new ValueAnalysis();

        List<Row> rows =
                valueAnalysis.analyze(user);

        String insertSql =
                sqlGenerator.insert(table, rows);

        sqlExecutor.execute(
                connection,
                insertSql
        );

        List<List<Row>> result =
                sqlExecutor.query(
                        connection,
                        "SELECT * FROM users"
                );

        ObjectMapper objectMapper =
                new ObjectMapper();

        User loadedUser =
                (User) objectMapper.map(
                        result.get(0),
                        User.class
                );

        System.out.println("ID: " + loadedUser.getId());
        System.out.println("Name: " + loadedUser.getName());
        System.out.println("Email: " + loadedUser.getEmail());

        connection.close();
    }
}
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

        // 1. Analisa a entidade
        Analysis analysis = new Analysis();

        List<ColumnDefinition> columns =
                analysis.analize(User.class);

        String tableName =
                analysis.analizeTableName(User.class);

        // 2. Constrói a representação da tabela
        SchemaBuilder schemaBuilder =
                new SchemaBuilder();

        Table table =
                schemaBuilder.build(
                        tableName,
                        columns
                );

        // 3. Gera o CREATE TABLE
        SqlGenerator sqlGenerator =
                new SqlGenerator();

        String createTableSql =
                sqlGenerator.createTable(table);

        // 4. Abre a conexão
        DatabaseConnection databaseConnection =
                new DatabaseConnection();

        Connection connection =
                databaseConnection.connection(
                        "jdbc:h2:mem:cdatra",
                        "sa",
                        ""
                );

        // 5. Cria a tabela
        SqlExecutor sqlExecutor =
                new SqlExecutor();

        sqlExecutor.execute(
                connection,
                createTableSql
        );

        // 6. Cria primeiro usuário
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

        // 7. Cria segundo usuário
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

        // 9. Gera e executa INSERT
        String user1InsertSql =
                sqlGenerator.insert(
                        table,
                        user1Rows
                );

        sqlExecutor.execute(
                connection,
                user1InsertSql
        );

        // 10. Extrai os valores do segundo usuário
        List<Row> user2Rows =
                valueAnalysis.analyze(user2);

        // 11. Gera e executa INSERT
        String user2InsertSql =
                sqlGenerator.insert(
                        table,
                        user2Rows
                );

        sqlExecutor.execute(
                connection,
                user2InsertSql
        );

        // 12. Gera SELECT automaticamente
        String selectSql =
                sqlGenerator.selectAll(table);

        System.out.println(selectSql);

        // 13. Executa o SELECT
        List<List<Row>> result =
                sqlExecutor.query(
                        connection,
                        selectSql
                );

        // 14. Converte cada registro do banco em User
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
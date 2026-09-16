package org.com.dev.carvel;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlExecutor;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.sql.TypeMapper;
import org.com.dev.carvel.table.Table;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException {

        SchemaBuilder schemaBuilder = new SchemaBuilder();
        Analysis analize = new Analysis();

        List<ColumnDefinition> columnDefinitions = analize.analize(User.class);
        Table table = schemaBuilder.build("users", columnDefinitions);

        SqlGenerator sqlGenerator = new SqlGenerator();

        String sql = sqlGenerator.createTable(table);

        DatabaseConnection databaseConnection = new DatabaseConnection();

        Connection connection = databaseConnection.connection("jdbc:h2:mem:cdatra", "sa", "");

        System.out.println(connection);


        SqlExecutor sqlExecutor = new SqlExecutor();
        sqlExecutor.execute(connection, sql);

        DatabaseMetaData databaseMetaData = connection.getMetaData();

        ResultSet resultSet = databaseMetaData.getTables(null, null, "USERS", new String[]{"TABLE"});

        System.out.println(resultSet.next());

        User user = new User(0, "Carvel", "carveltest1@gmail.com", new Address("alguma rua", 67, "alguma cidade"), "temporary");

        ValueAnalysis valueAnalysis = new ValueAnalysis();
        List<Row> rows = valueAnalysis.analyze(user);

        for (Row row : rows) {
            System.out.println(row.getColumnName() + " + " + row.getValue());
        }

        String sqlInsert = sqlGenerator.insert(table, rows);

        System.out.println(sqlInsert);
    }
}
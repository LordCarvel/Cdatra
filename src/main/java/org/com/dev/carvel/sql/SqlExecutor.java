package org.com.dev.carvel.sql;

import org.com.dev.carvel.row.Row;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqlExecutor {
    public void execute (Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();

        statement.execute(sql);
        statement.close();
    }

    public List<List<Row>> query (Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        List<List<Row>> lists = new ArrayList<>();

        while (resultSet.next()) {
            List<Row> rows = new ArrayList<>();

            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); ) {
                rows.add(new Row(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i)));

                i = i + 1;
            }

            lists.add(rows);
        }

        resultSet.close();
        statement.close();

        return lists;
    }
}

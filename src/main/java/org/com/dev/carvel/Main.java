package org.com.dev.carvel;

import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.table.Table;
import org.com.dev.carvel.user.User;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        Analysis analysis = new Analysis();

        List<ColumnDefinition> columns =
                analysis.analize(User.class);

        SchemaBuilder schemaBuilder =
                new SchemaBuilder();

        Table table =
                schemaBuilder.build(
                        "users",
                        columns
                );

        List<Row> rows = new ArrayList<>();

        rows.add(new Row("name", "joão"));
        rows.add(new Row("id", 0));
        rows.add(new Row("name", "carvel "));

        SqlGenerator sqlGenerator =
                new SqlGenerator();

        String sqlInsert = sqlGenerator.insert(table, rows);

        System.out.println(sqlInsert);
    }
}
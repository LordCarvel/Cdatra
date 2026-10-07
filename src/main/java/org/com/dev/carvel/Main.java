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

        SchemaBuilder schemaBuilder = new SchemaBuilder();

        List<Row> rows = new ArrayList<>();

        rows.add(new Row("name", "Carvel"));


        SqlGenerator sqlGenerator =  new SqlGenerator();

        Table table = new Table(null, columns);
        System.out.println(sqlGenerator.createTable(table));
    }
}
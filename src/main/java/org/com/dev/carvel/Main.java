package org.com.dev.carvel;

import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.table.Table;
import org.com.dev.carvel.user.User;

import java.util.List;

public class Main {

    public static void main(String[] args) {

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

        System.out.println(createTableSql);
    }
}
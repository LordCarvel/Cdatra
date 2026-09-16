package org.com.dev.carvel;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.sql.TypeMapper;
import org.com.dev.carvel.table.Table;
import org.com.dev.carvel.user.User;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        SchemaBuilder schemaBuilder = new SchemaBuilder();
        Analysis analize = new Analysis();

        List<ColumnDefinition> columnDefinitions = analize.analize(User.class);
        Table table = schemaBuilder.build("user", columnDefinitions);
        User user = new User(1, "carvel", "carveltest1@gmail.com", new Address("rua josé perine", 0,"quatro barras"), "sla");

        SqlGenerator sqlGenerator = new SqlGenerator();

        sqlGenerator.createTable(table);
    }
}
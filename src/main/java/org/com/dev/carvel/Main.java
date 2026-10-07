package org.com.dev.carvel;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.sql.TypeMapper;
import org.com.dev.carvel.table.Table;
import org.com.dev.carvel.user.User;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        Analysis analysis = new Analysis();

        System.out.println(
                analysis.analizeTableName(null)
        );
    }
}
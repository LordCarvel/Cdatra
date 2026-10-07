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

        TypeMapper typeMapper = new TypeMapper();

        System.out.println(
                typeMapper.map(null)
        );
    }
}
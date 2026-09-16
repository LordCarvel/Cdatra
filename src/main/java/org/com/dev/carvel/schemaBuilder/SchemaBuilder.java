package org.com.dev.carvel.schemaBuilder;

import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.table.Table;

import java.util.List;

public class SchemaBuilder {

    public Table build (String name, List<ColumnDefinition> columnDefinitionList) {
            return new Table(name, columnDefinitionList);
    }
}

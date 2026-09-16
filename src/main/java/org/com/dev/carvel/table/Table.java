package org.com.dev.carvel.table;

import org.com.dev.carvel.columnDefinition.ColumnDefinition;

import java.util.List;

public class Table {
    private String name;
    private List<ColumnDefinition> columnDefinitions;

    public Table (String name, List<ColumnDefinition> columnDefinitions) {
        this.name = name;
        this.columnDefinitions = columnDefinitions;
    }

    public String getName() {
        return name;
    }

    public List<ColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }
}
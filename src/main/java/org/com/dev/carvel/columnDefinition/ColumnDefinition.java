package org.com.dev.carvel.columnDefinition;

public class ColumnDefinition {
    private String name;
    private Class<?> type;

    public ColumnDefinition(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }
}

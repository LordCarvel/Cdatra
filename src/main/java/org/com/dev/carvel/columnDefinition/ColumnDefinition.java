package org.com.dev.carvel.columnDefinition;

public class ColumnDefinition {
    private String name;
    private Class<?> type;
    private boolean id;

    public ColumnDefinition(String name, Class<?> type, boolean id) {
        this.name = name;
        this.type = type;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isId() {
        return id;
    }
}

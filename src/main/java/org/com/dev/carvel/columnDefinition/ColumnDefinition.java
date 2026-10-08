package org.com.dev.carvel.columnDefinition;

public class ColumnDefinition {
    private String name;
    private Class<?> type;
    private boolean id;
    private boolean generatedValue;

    public ColumnDefinition(String name, Class<?> type, boolean id, boolean generatedValue) {
        this.name = name;
        this.type = type;
        this.id = id;
        this.generatedValue = generatedValue;
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

    public boolean isGeneratedValue() {
        return generatedValue;
    }
}

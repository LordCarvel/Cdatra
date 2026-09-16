package org.com.dev.carvel.row;

public class Row {
    private String columnName;
    private Object value;

    public Row (String columnName, Object value) {
        this.columnName = columnName;
        this.value = value;
    }

    public String getColumnName() {
        return columnName;
    }

    public Object getValue() {
        return value;
    }
}

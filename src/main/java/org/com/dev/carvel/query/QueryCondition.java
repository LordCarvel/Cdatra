package org.com.dev.carvel.query;

public class QueryCondition {

    private String columnName;
    private Object value;
    private Operator operator;

    public QueryCondition (String columnName, Object value, Operator operator) {

        this.columnName = columnName;
        this.value = value;
        this.operator = operator;
    }

    public String getColumnName() {
        return columnName;
    }

    public Object getValue() {
        return value;
    }

    public Operator getOperator() {
        return operator;
    }
}

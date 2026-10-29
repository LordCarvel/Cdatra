package io.github.lordcarvel.cdatra.sql;

import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.metadata.ColumnDefinition;
import io.github.lordcarvel.cdatra.metadata.Table;
import io.github.lordcarvel.cdatra.query.LogicalOperator;
import io.github.lordcarvel.cdatra.query.Operator;
import io.github.lordcarvel.cdatra.query.QueryCondition;
import io.github.lordcarvel.cdatra.query.QueryFilter;
import io.github.lordcarvel.cdatra.sql.dialect.H2Dialect;
import io.github.lordcarvel.cdatra.sql.dialect.SqlDialect;

import java.util.ArrayList;
import java.util.List;

public class SqlGenerator {

    private final TypeMapper typeMapper = new TypeMapper();

    private final SqlDialect dialect;

    public SqlGenerator () {

        this(new H2Dialect());
    }

    public SqlGenerator (SqlDialect dialect) {

        if (dialect == null) {

            throw new IllegalArgumentException("SQL dialect cannot be null");
        }

        this.dialect = dialect;
    }

    public SqlQuery createTable (Table table) {

        validateTable(table);

        StringBuilder sql = new StringBuilder();

        sql.append("CREATE TABLE ");
        sql.append(table.getName());
        sql.append(" (");

        List<ColumnDefinition> columns = table.getColumnDefinitions();

        for (int i = 0; i < columns.size(); i++) {

            ColumnDefinition column = columns.get(i);
            SqlType type = typeMapper.map(column.getType());
            String columnSql;

            if (column.isGeneratedValue()) {

                columnSql = dialect.generatedIdColumn(column.getName(), type);
            } else {

                columnSql = column.getName() + " " + dialect.typeName(type);

                if (column.isId()) {

                    columnSql = columnSql + " PRIMARY KEY";
                }
            }

            sql.append(columnSql);

            if (i < columns.size() - 1) {

                sql.append(", ");
            }
        }

        sql.append(");");

        return new SqlQuery(sql.toString(), List.of());
    }

    private void validateTable (Table table) {

        if (table == null) {

            throw new IllegalArgumentException("Table cannot be null");
        }

        String tableName = table.getName();

        if (tableName == null || tableName.isBlank()) {

            throw new IllegalArgumentException("Table name cannot be null or blank");
        }

        if (!tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {

            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        List<ColumnDefinition> columns = table.getColumnDefinitions();

        if (columns == null) {

            throw new IllegalArgumentException("Table columns cannot be null");
        }

        if (columns.isEmpty()) {

            throw new IllegalArgumentException("Table requires at least one column");
        }

        List<String> seenColumns = new ArrayList<>();
        int idCount = 0;

        for (ColumnDefinition columnDefinition : columns) {

            if (columnDefinition == null) {

                throw new IllegalArgumentException("Table column cannot be null");
            }

            String columnName = columnDefinition.getName();

            if (columnName == null || columnName.isBlank()) {

                throw new IllegalArgumentException("Column name cannot be null or blank");
            }

            if (!columnName.matches("[A-Za-z_][A-Za-z0-9_]*")) {

                throw new IllegalArgumentException("Invalid column name: " + columnName);
            }

            boolean duplicated = false;

            for (String seenColumn : seenColumns) {

                if (columnName.equalsIgnoreCase(seenColumn)) {

                    duplicated = true;
                }
            }

            if (duplicated) {

                throw new IllegalArgumentException("Duplicate column in table: " + columnName);
            }

            seenColumns.add(columnName);

            Class<?> javaType = columnDefinition.getType();

            if (javaType == null) {

                throw new IllegalArgumentException("Column type cannot be null");
            }

            SqlType sqlType = typeMapper.map(javaType);

            if (columnDefinition.isId()) {

                idCount++;
            }

            if (columnDefinition.isGeneratedValue() && (!columnDefinition.isId() || (sqlType != SqlType.INTEGER && sqlType != SqlType.BIGINT))) {

                throw new IllegalArgumentException("Generated column must be an INTEGER or BIGINT ID");
            }
        }

        if (idCount > 1) {

            throw new IllegalArgumentException("Table cannot contain more than one ID column");
        }
    }

    public SqlQuery insert (Table table, List<Row> rows) {

        if (table == null) {

            throw new IllegalArgumentException("Insert table cannot be null");
        }

        if (rows == null) {

            throw new IllegalArgumentException("Insert rows cannot be null");
        }

        validateTable(table);

        if (rows.isEmpty()) {

            if (table.getColumnDefinitions().stream().allMatch(ColumnDefinition::isGeneratedValue)) {

                return new SqlQuery("INSERT INTO " + table.getName() + " DEFAULT VALUES;", List.of());
            }

            throw new IllegalArgumentException("Insert requires at least one row");
        }

        for (Row row : rows) {

            if (row == null) {

                throw new IllegalArgumentException("Insert row cannot be null");
            }

            if (row.getColumnName() == null || row.getColumnName().isBlank()) {

                throw new IllegalArgumentException("Column name cannot be null or blank");
            }
        }

        StringBuilder sql = new StringBuilder();

        sql.append("INSERT INTO ");
        sql.append(table.getName());
        sql.append(" (");

        List<String> seenColumns = new ArrayList<>();

        for (Row row : rows) {

            boolean duplicated = false;

            for (String seenColumn : seenColumns) {

                if (row.getColumnName().equalsIgnoreCase(seenColumn)) {

                    duplicated = true;
                }
            }

            if (duplicated) {

                throw new IllegalArgumentException("Duplicate column in insert: " + row.getColumnName());
            }

            seenColumns.add(row.getColumnName());
        }

        for (Row row : rows) {

            boolean found = false;

            for (ColumnDefinition columnDefinition : table.getColumnDefinitions()) {

                if (row.getColumnName().equalsIgnoreCase(columnDefinition.getName())) {

                    found = true;

                    if (row.getValue() == null && columnDefinition.isId()) {

                        throw new IllegalArgumentException("ID value cannot be null");
                    }

                    if (row.getValue() != null) {

                        SqlType expectedType = typeMapper.map(columnDefinition.getType());
                        SqlType receivedType = typeMapper.map(row.getValue().getClass());

                        if (expectedType != receivedType) {

                            throw new IllegalArgumentException("Invalid value type for column: " + row.getColumnName());
                        }

                        if (row.getValue() instanceof Double && !Double.isFinite((Double) row.getValue())) {

                            throw new IllegalArgumentException("Non-finite value for column: " + row.getColumnName());
                        }
                    }
                }
            }

            if (!found) {

                throw new IllegalArgumentException("Column not found in table: " + row.getColumnName());
            }
        }

        for (int i = 0; i < rows.size();) {

            sql.append(rows.get(i).getColumnName());

            if (i < rows.size() - 1) {

                sql.append(", ");
            }

            i = i + 1;
        }

        sql.append(")");

        StringBuilder sqlValue = new StringBuilder();

        List<Object> parameters = new ArrayList<>();

        sqlValue.append(" VALUES (");

        for (int i = 0; i < rows.size();) {

            Object value = rows.get(i).getValue();

            sqlValue.append("?");
            parameters.add(value);

            if (i < rows.size() - 1) {

                sqlValue.append(", ");
            }

            i = i + 1;
        }

        sql.append(sqlValue);
        sql.append(");");

        return new SqlQuery(sql.toString(), parameters);
    }

    public SqlQuery selectAll (Table table) {

        if (table == null) {

            throw new IllegalArgumentException("Select table cannot be null");
        }

        if (table.getName() == null || table.getName().isBlank()) {

            throw new IllegalArgumentException("Table name cannot be null or blank");
        }

        validateTable(table);

        return new SqlQuery("SELECT * FROM " + table.getName() + ";", List.of());
    }

    public SqlQuery selectById (Table table, Row idRow) {

        if (table == null) {

            throw new IllegalArgumentException("Select table cannot be null");
        }

        if (idRow == null) {

            throw new IllegalArgumentException("Select ID row cannot be null");
        }

        if (idRow.getColumnName() == null || idRow.getColumnName().isBlank()) {

            throw new IllegalArgumentException("Select ID column name cannot be null or blank");
        }

        if (idRow.getValue() == null) {

            throw new IllegalArgumentException("Select ID value cannot be null");
        }

        SqlBuilder sql = new SqlBuilder();

        sql.append("SELECT * FROM " + table.getName() + " WHERE ");

        appendCondition(sql, table, idRow, Operator.EQUAL);

        sql.append(";");

        return sql.build();
    }

    public SqlQuery update (Table table, List<Row> rows, Row idRow) {

        if (table == null) {

            throw new IllegalArgumentException("Update table cannot be null");
        }

        if (rows == null) {

            throw new IllegalArgumentException("Update rows cannot be null");
        }

        if (rows.isEmpty()) {

            throw new IllegalArgumentException("Update requires at least one row");
        }

        if (idRow == null) {

            throw new IllegalArgumentException("Update ID row cannot be null");
        }

        if (idRow.getColumnName() == null || idRow.getColumnName().isBlank()) {

            throw new IllegalArgumentException("Update ID column name cannot be null or blank");
        }

        if (idRow.getValue() == null) {

            throw new IllegalArgumentException("Update ID value cannot be null");
        }

        validateTable(table);

        SqlBuilder sql = new SqlBuilder();

        sql.append("UPDATE " + table.getName() + " SET ");

        List<String> seenColumns = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {

            Row row = rows.get(i);

            if (row == null) {

                throw new IllegalArgumentException("Update row cannot be null");
            }

            appendAssignment(sql, table, row);

            if (row.getColumnName().equalsIgnoreCase(idRow.getColumnName())) {

                throw new IllegalArgumentException("ID column cannot be updated: " + row.getColumnName());
            }

            for (ColumnDefinition column : table.getColumnDefinitions()) {

                if (column.isId() && row.getColumnName().equalsIgnoreCase(column.getName())) {

                    throw new IllegalArgumentException("ID column cannot be updated: " + row.getColumnName());
                }
            }

            for (String seenColumn : seenColumns) {

                if (row.getColumnName().equalsIgnoreCase(seenColumn)) {

                    throw new IllegalArgumentException("Duplicate column in update: " + row.getColumnName());
                }
            }

            seenColumns.add(row.getColumnName());

            if (i < rows.size() - 1) {

                sql.append(", ");
            }
        }

        sql.append(" WHERE ");
        appendCondition(sql, table, idRow, Operator.EQUAL);
        sql.append(";");

        return sql.build();
    }

    public SqlQuery delete (Table table, Row idRow) {

        if (table == null) {

            throw new IllegalArgumentException("Delete table cannot be null");
        }

        if (idRow == null) {

            throw new IllegalArgumentException("Delete ID row cannot be null");
        }

        if (idRow.getValue() == null) {

            throw new IllegalArgumentException("Delete ID value cannot be null");
        }

        SqlBuilder sql = new SqlBuilder();

        sql.append("DELETE FROM " + table.getName() + " WHERE ");

        appendCondition(sql, table, idRow, Operator.EQUAL);

        sql.append(";");

        return sql.build();
    }

    public SqlQuery selectBy (Table table, Row row, Operator operator) {

        if (table == null) {

            throw new IllegalArgumentException("Select table cannot be null");
        }

        SqlBuilder sql = new SqlBuilder();

        sql.append("SELECT * FROM " + table.getName() + " WHERE ");

        appendCondition(sql, table, row, operator);

        sql.append(";");

        return sql.build();
    }

    public SqlQuery selectByConditions (Table table, List<QueryCondition> conditions) {

        if (table == null) {

            throw new IllegalArgumentException("Select table cannot be null");
        }

        if (conditions == null) {

            throw new IllegalArgumentException("Select conditions cannot be null");
        }

        if (conditions.isEmpty()) {

            throw new IllegalArgumentException("Select requires at least one condition");
        }

        validateTable(table);

        SqlBuilder sql = new SqlBuilder();

        sql.append("SELECT * FROM " + table.getName() + " WHERE ");

        for (int i = 0; i < conditions.size();) {

            QueryCondition condition = conditions.get(i);

            if (condition == null) {

                throw new IllegalArgumentException("Query condition cannot be null");
            }

            Row row = new Row(condition.getColumnName(), condition.getValue());
            appendCondition(sql, table, row, condition.getOperator());

            if (i < conditions.size() - 1) {

                sql.append(" AND ");
            }

            i = i + 1;
        }

        sql.append(";");

        return sql.build();
    }

    public SqlQuery selectByFilters (Table table, List<QueryFilter> filters) {

        if (table == null) {

            throw new IllegalArgumentException("Select table cannot be null");
        }

        if (filters == null) {

            throw new IllegalArgumentException("Select filters cannot be null");
        }

        if (filters.isEmpty()) {

            throw new IllegalArgumentException("Select requires at least one filter");
        }

        validateTable(table);

        SqlBuilder sql = new SqlBuilder();

        sql.append("SELECT * FROM " + table.getName() + " WHERE ");

        for (int i = 0; i < filters.size();) {

            QueryFilter filter = filters.get(i);

            if (filter == null) {

                throw new IllegalArgumentException("Query filter cannot be null");
            }

            QueryCondition condition = filter.getCondition();

            if (condition == null) {

                throw new IllegalArgumentException("Query condition cannot be null");
            }

            if (i > 0) {

                LogicalOperator logicalOperator = filter.getLogicalOperator();

                if (logicalOperator == null) {

                    throw new IllegalArgumentException("Logical operator cannot be null after the first condition");
                }

                if (logicalOperator == LogicalOperator.AND) {

                    sql.append(" AND ");
                } else if (logicalOperator == LogicalOperator.OR) {

                    sql.append(" OR ");
                }
            }

            Row row = new Row(condition.getColumnName(), condition.getValue());
            appendCondition(sql, table, row, condition.getOperator());

            i = i + 1;
        }

        sql.append(";");

        return sql.build();
    }

    private void appendCondition (SqlBuilder sql, Table table, Row row, Operator operator) {

        if (table == null) {

            throw new IllegalArgumentException("Select table cannot be null");
        }

        if (row == null) {

            throw new IllegalArgumentException("Select row cannot be null");
        }

        if (row.getColumnName() == null || row.getColumnName().isBlank()) {

            throw new IllegalArgumentException("Select column name cannot be null or blank");
        }

        if (operator == null) {

            throw new IllegalArgumentException("Select operator cannot be null");
        }

        validateTable(table);
        boolean columnFound = false;
        SqlType columnType = null;

        for (ColumnDefinition columnDefinition : table.getColumnDefinitions()) {

            if (row.getColumnName().equalsIgnoreCase(columnDefinition.getName())) {

                columnFound = true;
                columnType = typeMapper.map(columnDefinition.getType());
            }
        }

        if (!columnFound) {

            throw new IllegalArgumentException("Column not found in table: " + row.getColumnName());
        }

        String sqlOperator;

        if (operator == Operator.EQUAL) {

            sqlOperator = "=";

        } else if (operator == Operator.NOT_EQUAL) {

            sqlOperator = "!=";

        } else if (operator == Operator.GREATER_THAN) {

            sqlOperator = ">";

        } else if (operator == Operator.LESS_THAN) {

            sqlOperator = "<";

        } else if (operator == Operator.GREATER_THAN_OR_EQUAL) {

            sqlOperator = ">=";

        } else if (operator == Operator.LESS_THAN_OR_EQUAL) {

            sqlOperator = "<=";

        } else {

            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        sql.append(row.getColumnName());

        Object value = row.getValue();

        if (value == null) {

            if (operator == Operator.EQUAL) {

                sql.append(" IS NULL");

            } else if (operator == Operator.NOT_EQUAL) {

                sql.append(" IS NOT NULL");

            } else {

                throw new IllegalArgumentException("Null values only support EQUAL or NOT_EQUAL");
            }
        } else {

            SqlType sqlType = typeMapper.map(value.getClass());

            if (sqlType != columnType) {

                throw new IllegalArgumentException("Invalid value type for column: " + row.getColumnName());
            }

            if (value instanceof Double && !Double.isFinite((Double) value)) {

                throw new IllegalArgumentException("Non-finite value for column: " + row.getColumnName());
            }

            sql.append(" " + sqlOperator + " ");
            sql.appendParameter(value);
        }
    }

    private void appendAssignment (SqlBuilder sql, Table table, Row row) {

        if (row.getColumnName() == null || row.getColumnName().isBlank()) {

            throw new IllegalArgumentException("Update column name cannot be null or blank");
        }

        boolean columnFound = false;
        SqlType columnType = null;

        for (ColumnDefinition columnDefinition : table.getColumnDefinitions()) {

            if (row.getColumnName().equalsIgnoreCase(columnDefinition.getName())) {

                columnFound = true;
                columnType = typeMapper.map(columnDefinition.getType());
            }
        }

        if (!columnFound) {

            throw new IllegalArgumentException("Column not found in table: " + row.getColumnName());
        }

        Object value = row.getValue();

        if (value != null) {

            SqlType sqlType = typeMapper.map(value.getClass());

            if (sqlType != columnType) {

                throw new IllegalArgumentException("Invalid value type for column: " + row.getColumnName());
            }

            if (value instanceof Double && !Double.isFinite((Double) value)) {

                throw new IllegalArgumentException("Non-finite value for column: " + row.getColumnName());
            }
        }

        sql.append(row.getColumnName() + " = ");
        sql.appendParameter(value);
    }
}

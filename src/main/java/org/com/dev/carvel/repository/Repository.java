package org.com.dev.carvel.repository;

import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.mapper.ObjectMapper;
import org.com.dev.carvel.query.Operator;
import org.com.dev.carvel.query.QueryCondition;
import org.com.dev.carvel.query.QueryFilter;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlExecutor;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.table.Table;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Repository<T> {

    private final Class<T> type;
    private final Connection connection;

    private final Analysis analysis = new Analysis();
    private final ValueAnalysis valueAnalysis = new ValueAnalysis();
    private final SchemaBuilder schemaBuilder = new SchemaBuilder();
    private final SqlGenerator sqlGenerator = new SqlGenerator();
    private final SqlExecutor sqlExecutor = new SqlExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Repository (Class<T> type, Connection connection) {

        this.type = type;
        this.connection = connection;
    }

    public void createTable () throws SQLException {

        Table table = buildTable();
        String sql = sqlGenerator.createTable(table);

        sqlExecutor.execute(connection, sql);
    }

    public void save (T entity) throws IllegalAccessException, SQLException {

        List<Row> rows = valueAnalysis.analyze(entity);
        Table table = buildTable();
        String sql = sqlGenerator.insert(table, rows);

        Object generatedValue = sqlExecutor.executeAndReturnGeneratedKey(connection, sql);

        if (generatedValue != null) {

            valueAnalysis.setGeneratedValue(entity, generatedValue);
        }
    }

    public List<T> findAll () throws Exception {

        Table table = buildTable();
        String sql = sqlGenerator.selectAll(table);

        List<List<Row>> result = sqlExecutor.query(connection, sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = (T) objectMapper.map(record, type);
            entities.add(entity);
        }

        return entities;
    }

    public T findById (Object idValue) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();
        String idColumnName = analysis.analyzeIdColumnName(type);
        Row idRow = new Row(idColumnName, idValue);
        String sql = sqlGenerator.selectById(table, idRow);

        List<List<Row>> result = sqlExecutor.query(connection, sql);

        if (result.isEmpty()) {

            return null;
        }

        return (T) objectMapper.map(result.get(0), type);
    }

    public List<T> findBy (String columnName, Object value, Operator operator) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();
        Row row = new Row(columnName, value);
        String sql = sqlGenerator.selectBy(table, row, operator);

        List<List<Row>> result = sqlExecutor.query(connection, sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = (T) objectMapper.map(record, type);
            entities.add(entity);
        }

        return entities;
    }

    public List<T> findByConditions (List<QueryCondition> conditions) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();

        String sql = sqlGenerator.selectByConditions(table, conditions);

        List<List<Row>> result = sqlExecutor.query(connection, sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = (T) objectMapper.map(record, type);

            entities.add(entity);
        }

        return entities;
    }

    public List<T> findByFilters (List<QueryFilter> filters) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();

        String sql = sqlGenerator.selectByFilters(table, filters);

        List<List<Row>> result = sqlExecutor.query(connection, sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = (T) objectMapper.map(record, type);

            entities.add(entity);
        }

        return entities;
    }

    public void update (T entity) throws IllegalAccessException, SQLException {

        List<Row> rows = valueAnalysis.analyze(entity);
        Row idRow = valueAnalysis.analyzeId(entity);
        rows.removeIf(row -> row.getColumnName().equalsIgnoreCase(idRow.getColumnName()));

        Table table = buildTable();
        String sql = sqlGenerator.update(table, rows, idRow);

        sqlExecutor.execute(connection, sql);
    }

    public void delete (T entity) throws IllegalAccessException, SQLException {

        Row idRow = valueAnalysis.analyzeId(entity);
        Table table = buildTable();
        String sql = sqlGenerator.delete(table, idRow);

        sqlExecutor.execute(connection, sql);
    }

    private Table buildTable () {

        List<ColumnDefinition> columnDefinitions = analysis.analize(type);
        String tableName = analysis.analyzeTableName(type);

        return schemaBuilder.build(tableName, columnDefinitions);
    }
}

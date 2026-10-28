package io.github.lordcarvel.cdatra.repository;

import io.github.lordcarvel.cdatra.adapter.DatabaseAdapter;
import io.github.lordcarvel.cdatra.jdbc.JdbcDatabaseAdapter;
import io.github.lordcarvel.cdatra.mapping.ObjectMapper;
import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.mapping.ValueAnalysis;
import io.github.lordcarvel.cdatra.metadata.Analysis;
import io.github.lordcarvel.cdatra.metadata.ColumnDefinition;
import io.github.lordcarvel.cdatra.metadata.SchemaBuilder;
import io.github.lordcarvel.cdatra.metadata.Table;
import io.github.lordcarvel.cdatra.query.Operator;
import io.github.lordcarvel.cdatra.query.QueryCondition;
import io.github.lordcarvel.cdatra.query.QueryFilter;
import io.github.lordcarvel.cdatra.sql.SqlGenerator;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Repository<T> {

    private final Class<T> type;

    private final DatabaseAdapter databaseAdapter;

    private final Analysis analysis = new Analysis();
    private final ValueAnalysis valueAnalysis = new ValueAnalysis();
    private final SchemaBuilder schemaBuilder = new SchemaBuilder();
    private final SqlGenerator sqlGenerator = new SqlGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Repository (Class<T> type, Connection connection) {

        this(type, new JdbcDatabaseAdapter(connection));
    }

    public Repository (Class<T> type, DatabaseAdapter databaseAdapter) {

        if (type == null) {

            throw new IllegalArgumentException("Repository type cannot be null");
        }

        if (databaseAdapter == null) {

            throw new IllegalArgumentException("DatabaseAdapter cannot be null");
        }

        this.type = type;
        this.databaseAdapter = databaseAdapter;
    }

    public void createTable () throws SQLException {

        Table table = buildTable();
        String sql = sqlGenerator.createTable(table);

        databaseAdapter.execute(sql);
    }

    public void save (T entity) throws IllegalAccessException, SQLException {

        List<Row> rows = valueAnalysis.analyze(entity);
        Table table = buildTable();
        String sql = sqlGenerator.insert(table, rows);

        Object generatedValue = databaseAdapter.executeAndReturnGeneratedKey(sql);

        if (generatedValue != null) {

            valueAnalysis.setGeneratedValue(entity, generatedValue);
        }
    }

    public List<T> findAll () throws Exception {

        Table table = buildTable();
        String sql = sqlGenerator.selectAll(table);

        List<List<Row>> result = databaseAdapter.query(sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = type.cast(objectMapper.map(record, type));
            entities.add(entity);
        }

        return entities;
    }

    public T findById (Object idValue) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();
        String idColumnName = analysis.analyzeIdColumnName(type);
        Row idRow = new Row(idColumnName, idValue);
        String sql = sqlGenerator.selectById(table, idRow);

        List<List<Row>> result = databaseAdapter.query(sql);

        if (result.isEmpty()) {

            return null;
        }

        return type.cast(objectMapper.map(result.get(0), type));
    }

    public List<T> findBy (String columnName, Object value, Operator operator) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();
        Row row = new Row(columnName, value);
        String sql = sqlGenerator.selectBy(table, row, operator);

        List<List<Row>> result = databaseAdapter.query(sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = type.cast(objectMapper.map(record, type));
            entities.add(entity);
        }

        return entities;
    }

    public List<T> findByConditions (List<QueryCondition> conditions) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();

        String sql = sqlGenerator.selectByConditions(table, conditions);

        List<List<Row>> result = databaseAdapter.query(sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = type.cast(objectMapper.map(record, type));

            entities.add(entity);
        }

        return entities;
    }

    public List<T> findByFilters (List<QueryFilter> filters) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Table table = buildTable();

        String sql = sqlGenerator.selectByFilters(table, filters);

        List<List<Row>> result = databaseAdapter.query(sql);

        List<T> entities = new ArrayList<>();

        for (List<Row> record : result) {

            T entity = type.cast(objectMapper.map(record, type));

            entities.add(entity);
        }

        return entities;
    }

    public void update (T entity) throws IllegalAccessException, SQLException {

        List<Row> rows = valueAnalysis.analyze(entity);
        Row idRow = valueAnalysis.analyzeId(entity);
        rows.removeIf(row -> row.getColumnName().equalsIgnoreCase(idRow.getColumnName()));

        Table table = buildTable();

        if (rows.isEmpty()) {

            sqlGenerator.selectById(table, idRow);

            return;
        }

        String sql = sqlGenerator.update(table, rows, idRow);

        databaseAdapter.execute(sql);
    }

    public void delete (T entity) throws IllegalAccessException, SQLException {

        Row idRow = valueAnalysis.analyzeId(entity);
        Table table = buildTable();
        String sql = sqlGenerator.delete(table, idRow);

        databaseAdapter.execute(sql);
    }

    private Table buildTable () {

        List<ColumnDefinition> columnDefinitions = analysis.analize(type);
        String tableName = analysis.analyzeTableName(type);

        return schemaBuilder.build(tableName, columnDefinitions);
    }
}

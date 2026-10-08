package org.com.dev.carvel.repository;

import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.mapper.ObjectMapper;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlExecutor;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.table.Table;

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

        List<ColumnDefinition> columnDefinitions = analysis.analize(type);
        String tableName = analysis.analyzeTableName(type);
        Table table = schemaBuilder.build(tableName, columnDefinitions);

        String sql = sqlGenerator.createTable(table);

        sqlExecutor.execute(connection, sql);
    }

    public void save (T entity) throws IllegalAccessException, SQLException {

        List<Row> rows = valueAnalysis.analyze(entity);

        List<ColumnDefinition> columnDefinitions = analysis.analize(type);
        String tableName = analysis.analyzeTableName(type);
        Table table = schemaBuilder.build(tableName, columnDefinitions);

        String sql = sqlGenerator.insert(table, rows);

        sqlExecutor.execute(connection, sql);
    }

    public List<T> findAll () throws Exception {

        List<ColumnDefinition> columnDefinitions = analysis.analize(type);

        String tableName = analysis.analyzeTableName(type);

        Table table = schemaBuilder.build(tableName, columnDefinitions);

        String sql = sqlGenerator.selectAll(table);

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

        List<ColumnDefinition> columnDefinitions = analysis.analize(type);
        String tableName = analysis.analyzeTableName(type);
        Table table = schemaBuilder.build(tableName, columnDefinitions);

        String sql = sqlGenerator.update(table, rows, idRow);

        sqlExecutor.execute(connection, sql);
    }
}

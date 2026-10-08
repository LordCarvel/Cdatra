package org.com.dev.carvel.repository;

import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.mapper.ObjectMapper;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.sql.SqlExecutor;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.table.Table;

import java.sql.Connection;
import java.sql.SQLException;
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
}

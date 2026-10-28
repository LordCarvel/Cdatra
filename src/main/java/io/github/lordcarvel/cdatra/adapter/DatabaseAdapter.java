package io.github.lordcarvel.cdatra.adapter;

import io.github.lordcarvel.cdatra.mapping.Row;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseAdapter {

    void execute (String sql) throws SQLException;

    List<List<Row>> query (String sql) throws SQLException;

    Object executeAndReturnGeneratedKey (String sql) throws SQLException;
}

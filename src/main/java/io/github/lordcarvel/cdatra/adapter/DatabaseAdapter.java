package io.github.lordcarvel.cdatra.adapter;

import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.sql.dialect.SqlDialect;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseAdapter {

    void execute (SqlQuery query) throws SQLException;

    List<List<Row>> query (SqlQuery query) throws SQLException;

    Object executeAndReturnGeneratedKey (SqlQuery query) throws SQLException;

    SqlDialect dialect ();
}

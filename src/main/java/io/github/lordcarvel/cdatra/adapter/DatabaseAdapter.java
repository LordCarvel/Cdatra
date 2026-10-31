package io.github.lordcarvel.cdatra.adapter;

import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.sql.dialect.SqlDialect;

import java.util.List;

public interface DatabaseAdapter {

    void execute (SqlQuery query);

    List<List<Row>> query (SqlQuery query);

    Object executeAndReturnGeneratedKey (SqlQuery query);

    SqlDialect dialect ();
}

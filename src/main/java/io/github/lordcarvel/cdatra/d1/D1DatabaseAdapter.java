package io.github.lordcarvel.cdatra.d1;

import io.github.lordcarvel.cdatra.adapter.DatabaseAdapter;
import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.sql.dialect.D1Dialect;
import io.github.lordcarvel.cdatra.sql.dialect.SqlDialect;

import java.util.List;

public class D1DatabaseAdapter implements DatabaseAdapter {

    private final String url;

    private final String secret;

    public D1DatabaseAdapter (String url, String secret) {

        if (url == null || url.isBlank()) {

            throw new IllegalArgumentException("Worker URL cannot be null or blank");
        }

        if (secret == null || secret.isBlank()) {

            throw new IllegalArgumentException("Worker secret cannot be null or blank");
        }

        this.url = url;
        this.secret = secret;
    }

    @Override
    public void execute (SqlQuery query) {

        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<List<Row>> query (SqlQuery query) {

        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Object executeAndReturnGeneratedKey (SqlQuery query) {

        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public SqlDialect dialect () {

        return new D1Dialect();
    }
}

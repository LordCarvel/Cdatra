package io.github.lordcarvel.cdatra.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SqlQuery {

    private final String sql;

    private final List<Object> parameters;

    public SqlQuery (String sql, List<Object> parameters) {

        if (sql == null || sql.isBlank()) {

            throw new IllegalArgumentException("SQL cannot be null or blank");
        }

        if (parameters == null) {

            throw new IllegalArgumentException("SQL parameters cannot be null");
        }

        this.sql = sql;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public String getSql () {

        return sql;
    }

    public List<Object> getParameters () {

        return parameters;
    }
}

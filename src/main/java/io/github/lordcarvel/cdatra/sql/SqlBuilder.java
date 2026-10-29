package io.github.lordcarvel.cdatra.sql;

import java.util.ArrayList;
import java.util.List;

public class SqlBuilder {

    private final StringBuilder sql = new StringBuilder();

    private final List<Object> parameters = new ArrayList<>();

    public SqlBuilder append (String text) {

        if (text == null) {

            throw new IllegalArgumentException("SQL text cannot be null");
        }

        sql.append(text);

        return this;
    }

    public SqlBuilder appendParameter (Object value) {

        sql.append("?");
        parameters.add(value);

        return this;
    }

    public SqlQuery build () {

        return new SqlQuery(sql.toString(), parameters);
    }
}

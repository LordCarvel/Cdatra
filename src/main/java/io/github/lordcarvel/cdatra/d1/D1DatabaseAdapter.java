package io.github.lordcarvel.cdatra.d1;

import io.github.lordcarvel.cdatra.adapter.DatabaseAdapter;
import io.github.lordcarvel.cdatra.internal.json.JsonReader;
import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.sql.dialect.D1Dialect;
import io.github.lordcarvel.cdatra.sql.dialect.SqlDialect;

import io.github.lordcarvel.cdatra.exception.CdatraException;
import io.github.lordcarvel.cdatra.internal.json.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.List;

public class D1DatabaseAdapter implements DatabaseAdapter {

    private final String url;
    private final String secret;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final JsonWriter jsonWriter = new JsonWriter();

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

        readResult(send(query));
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

    private HttpResponse<String> send (SqlQuery query) {

        if (query == null) {

            throw new IllegalArgumentException("SQL query cannot be null");
        }

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("sql", query.getSql());
        body.put("params", query.getParameters());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/v1/query"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + secret)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonWriter.write(body)))
                .build();

        try {

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {

            throw new CdatraException("Failed to reach the D1 Worker", e);
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new CdatraException("Interrupted while calling the D1 Worker", e);
        }
    }

    private Map<?, ?> asMap (Object value, String name) {

        if (value instanceof Map) {

            return (Map<?, ?>) value;
        }

        throw new CdatraException("Invalid D1 Worker response: expected " + name);
    }

    private Map<?, ?> readResult (HttpResponse<String> response) {

        Object parsed;

        try {

            parsed = new JsonReader(response.body()).read();
        } catch (IllegalArgumentException e) {

            throw new CdatraException("Invalid D1 Worker response (HTTP " + response.statusCode() + ")", e);
        }

        Map<?, ?> body = asMap(parsed, "a JSON object");

        if (Boolean.TRUE.equals(body.get("ok"))) {

            return asMap(body.get("result"), "result");
        }

        Map<?, ?> error = asMap(body.get("error"), "error");

        throw new CdatraException(error.get("code") + ": " + error.get("message"));
    }
}

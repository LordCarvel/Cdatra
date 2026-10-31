package io.github.lordcarvel.cdatra.d1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lordcarvel.cdatra.exception.CdatraException;
import io.github.lordcarvel.cdatra.internal.json.JsonReader;
import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.repository.Repository;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.support.TestEntities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class D1DatabaseAdapterWorkerTest {

    private static final String SECRET = "test-secret";

    private static final String EMPTY_RESULT = "{\"ok\":true,\"result\":{\"columns\":[],\"rows\":[],\"meta\":{\"last_row_id\":0}}}";

    private HttpServer worker;

    private D1DatabaseAdapter adapter;

    private volatile int requests = 0;

    private volatile String method;

    private volatile String path;

    private volatile String authorization;

    private volatile String contentType;

    private volatile String requestBody;

    private volatile int responseStatus = 200;

    private volatile String responseBody = EMPTY_RESULT;

    @BeforeEach
    void startWorker () throws IOException {

        worker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        worker.createContext("/", this::answer);
        worker.start();

        adapter = new D1DatabaseAdapter("http://127.0.0.1:" + worker.getAddress().getPort(), SECRET);
    }

    @AfterEach
    void stopWorker () {

        worker.stop(0);
    }

    private void answer (HttpExchange exchange) throws IOException {

        requests = requests + 1;
        method = exchange.getRequestMethod();
        path = exchange.getRequestURI().getPath();
        authorization = exchange.getRequestHeaders().getFirst("Authorization");
        contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(responseStatus, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private SqlQuery sql (String text, Object... parameters) {

        return new SqlQuery(text, Arrays.asList(parameters));
    }

    private Object sentParameters () {

        return ((Map<?, ?>) new JsonReader(requestBody).read()).get("params");
    }

    @Test
    void postsToTheQueryRouteWithTheSecret () {

        adapter.execute(sql("DELETE FROM users"));

        assertEquals("POST", method);
        assertEquals("/v1/query", path);
        assertEquals("Bearer " + SECRET, authorization);
        assertEquals("application/json", contentType);
    }

    @Test
    void sendsTheSqlAndTheParameters () {

        adapter.execute(sql("DELETE FROM users WHERE id = ? AND name = ?", 1L, "Ana"));

        assertEquals(Map.of("sql", "DELETE FROM users WHERE id = ? AND name = ?", "params", List.of(1L, "Ana")), new JsonReader(requestBody).read());
    }

    @Test
    void keepsTextOutsideAsciiInTheRequest () {

        adapter.execute(sql("DELETE FROM users WHERE name = ?", "João ✓"));

        assertEquals(List.of("João ✓"), sentParameters());
    }

    @Test
    void rejectsNullQueryWithoutCallingTheWorker () {

        assertThrows(IllegalArgumentException.class, () -> adapter.execute(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.query(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.executeAndReturnGeneratedKey(null));
        assertEquals(0, requests);
    }

    @Test
    void reportsAnUnreachableWorkerKeepingTheCause () {

        worker.stop(0);

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT 1")));

        assertEquals("Failed to reach the D1 Worker", error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    void keepsTheInterruptionOfTheThread () {

        Thread.currentThread().interrupt();

        try {

            CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT 1")));

            assertEquals("Interrupted while calling the D1 Worker", error.getMessage());
            assertInstanceOf(InterruptedException.class, error.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {

            Thread.interrupted();
        }
    }

    @Test
    void reportsTheErrorFromTheWorker () {

        responseStatus = 400;
        responseBody = "{\"ok\":false,\"error\":{\"code\":\"SQL_ERROR\",\"message\":\"no such table: users\"}}";

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT * FROM users")));

        assertEquals("SQL_ERROR: no such table: users", error.getMessage());
    }

    @Test
    void everyOperationReportsTheErrorFromTheWorker () {

        responseStatus = 401;
        responseBody = "{\"ok\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"bad secret\"}}";

        assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT 1")));
        assertThrows(CdatraException.class, () -> adapter.query(sql("SELECT 1")));
        assertThrows(CdatraException.class, () -> adapter.executeAndReturnGeneratedKey(sql("SELECT 1")));
    }

    @Test
    void errorMessageNeverContainsTheSecret () {

        responseStatus = 401;
        responseBody = "<html>Unauthorized</html>";

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT 1")));

        assertFalse(error.getMessage().contains(SECRET));
    }

    @Test
    void reportsABodyThatIsNotJsonWithTheHttpStatus () {

        responseStatus = 502;
        responseBody = "<html>Bad Gateway</html>";

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT 1")));

        assertEquals("Invalid D1 Worker response (HTTP 502)", error.getMessage());
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1]", "1", "null", "{\"ok\":true}", "{\"ok\":true,\"result\":[]}", "{\"ok\":false}", "{}", "{\"ok\":\"true\"}"})
    void rejectsResponsesOutsideTheProtocol (String body) {

        responseBody = body;

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(sql("SELECT 1")));

        assertTrue(error.getMessage().startsWith("Invalid D1 Worker response: expected"));
    }

    @Test
    void ignoresFieldsItDoesNotKnow () {

        responseBody = "{\"ok\":true,\"version\":2,\"result\":{\"columns\":[\"id\"],\"rows\":[[1]],\"meta\":{\"last_row_id\":0,\"duration\":3},\"cached\":false}}";

        assertEquals(1, adapter.query(sql("SELECT id FROM users")).size());
    }

    @Test
    void joinsColumnsAndValuesInEachRow () {

        responseBody = "{\"ok\":true,\"result\":{\"columns\":[\"id\",\"name\"],\"rows\":[[1,\"Ana\"],[2,\"Bia\"]],\"meta\":{}}}";

        List<List<Row>> records = adapter.query(sql("SELECT id, name FROM users"));

        assertEquals(2, records.size());
        assertEquals("id", records.get(0).get(0).getColumnName());
        assertEquals(1L, records.get(0).get(0).getValue());
        assertEquals("name", records.get(0).get(1).getColumnName());
        assertEquals("Ana", records.get(0).get(1).getValue());
        assertEquals(2L, records.get(1).get(0).getValue());
        assertEquals("Bia", records.get(1).get(1).getValue());
    }

    @Test
    void readsAQueryWithoutRows () {

        assertEquals(List.of(), adapter.query(sql("SELECT id FROM users")));
    }

    @Test
    void keepsNullValuesAndColumnsWithTheSameName () {

        responseBody = "{\"ok\":true,\"result\":{\"columns\":[\"id\",\"id\"],\"rows\":[[1,null]],\"meta\":{}}}";

        List<Row> record = adapter.query(sql("SELECT a.id, b.id FROM a JOIN b")).get(0);

        assertEquals(2, record.size());
        assertEquals("id", record.get(1).getColumnName());
        assertNull(record.get(1).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"ok\":true,\"result\":{\"columns\":[\"id\",\"name\"],\"rows\":[[1]]}}",
            "{\"ok\":true,\"result\":{\"columns\":[\"id\"],\"rows\":[[1,2]]}}"
    })
    void rejectsRowsOfTheWrongSize (String body) {

        responseBody = body;

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.query(sql("SELECT id FROM users")));

        assertEquals("Invalid D1 Worker response: row size does not match columns", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"ok\":true,\"result\":{\"rows\":[]}}",
            "{\"ok\":true,\"result\":{\"columns\":[]}}",
            "{\"ok\":true,\"result\":{\"columns\":[\"id\"],\"rows\":[1]}}"
    })
    void rejectsMissingColumnsAndRows (String body) {

        responseBody = body;

        assertThrows(CdatraException.class, () -> adapter.query(sql("SELECT id FROM users")));
    }

    @Test
    void readsTheGeneratedKey () {

        responseBody = "{\"ok\":true,\"result\":{\"columns\":[],\"rows\":[],\"meta\":{\"last_row_id\":7}}}";

        assertEquals(7L, adapter.executeAndReturnGeneratedKey(sql("INSERT INTO users (name) VALUES (?)", "Ana")));
    }

    @Test
    void rejectsAResponseWithoutMeta () {

        responseBody = "{\"ok\":true,\"result\":{\"columns\":[],\"rows\":[]}}";

        assertThrows(CdatraException.class, () -> adapter.executeAndReturnGeneratedKey(sql("INSERT INTO users (name) VALUES (?)", "Ana")));
    }

    @Test
    void sendsBooleansAsOneAndZero () {

        adapter.execute(sql("UPDATE users SET active = ?, blocked = ?", true, false));

        assertEquals(List.of(1L, 0L), sentParameters());
    }

    @Test
    void keepsTheOtherParameterTypesAndTheirOrder () {

        adapter.execute(sql("INSERT INTO users VALUES (?, ?, ?, ?)", 1, "Ana", 1.5, null));

        assertEquals(Arrays.asList(1L, "Ana", 1.5, null), sentParameters());
    }

    @ParameterizedTest
    @ValueSource(longs = {9007199254740991L, -9007199254740991L})
    void acceptsTheLimitsOfTheJavaScriptIntegers (long value) {

        adapter.execute(sql("DELETE FROM users WHERE id = ?", value));

        assertEquals(List.of(value), sentParameters());
    }

    @ParameterizedTest
    @ValueSource(longs = {9007199254740992L, -9007199254740992L, Long.MAX_VALUE, Long.MIN_VALUE})
    void rejectsLongsThatWouldLosePrecisionWithoutCallingTheWorker (long value) {

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> adapter.execute(sql("DELETE FROM users WHERE id = ?", value)));

        assertEquals("Long value would lose precision in D1: " + value, error.getMessage());
        assertEquals(0, requests);
    }

    @Test
    void repositorySavesThroughTheWorkerAndReceivesTheGeneratedId () {

        responseBody = "{\"ok\":true,\"result\":{\"columns\":[],\"rows\":[],\"meta\":{\"last_row_id\":7}}}";

        Repository<TestEntities.BoxedRecord> repository = new Repository<>(TestEntities.BoxedRecord.class, adapter);

        TestEntities.BoxedRecord record = new TestEntities.BoxedRecord();
        record.total = 5L;
        record.active = true;
        record.score = 2.0;

        repository.save(record);

        assertEquals(7, record.id);
        assertEquals(List.of(5L, 1L, 2.0), sentParameters());
    }

    @Test
    void repositoryMapsTheValuesThatTheWorkerReturns () {

        responseBody = "{\"ok\":true,\"result\":{\"columns\":[\"id\",\"total\",\"active\",\"score\"],\"rows\":[[7,5,1,2]],\"meta\":{}}}";

        Repository<TestEntities.BoxedRecord> repository = new Repository<>(TestEntities.BoxedRecord.class, adapter);

        TestEntities.BoxedRecord record = repository.findById(7);

        assertEquals(7, record.id);
        assertEquals(5L, record.total);
        assertTrue(record.active);
        assertEquals(2.0, record.score);
    }

    @Test
    void repositoryReturnsNothingWhenTheWorkerFindsNoRow () {

        Repository<TestEntities.BoxedRecord> repository = new Repository<>(TestEntities.BoxedRecord.class, adapter);

        assertNull(repository.findById(7));
    }
}

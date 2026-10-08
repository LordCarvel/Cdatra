package org.com.dev.carvel.repository;

import org.com.dev.carvel.query.LogicalOperator;
import org.com.dev.carvel.query.Operator;
import org.com.dev.carvel.query.QueryCondition;
import org.com.dev.carvel.query.QueryFilter;
import org.com.dev.carvel.support.TestEntities;
import org.com.dev.carvel.support.TestEntities.Record;
import org.com.dev.carvel.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryIntegrationTest {

    private Connection connection;
    private Repository<Record> repository;

    @BeforeEach
    void openIsolatedDatabase () throws Exception {

        connection = DriverManager.getConnection("jdbc:h2:mem:");
        repository = new Repository<>(Record.class, connection);
        repository.createTable();
    }

    @AfterEach
    void closeDatabase () throws Exception {

        connection.close();
    }

    @Test
    void completesCrudLifecycleWithGeneratedIdAndPreservesOtherRecords () throws Exception {

        assertTrue(repository.findAll().isEmpty());
        assertNull(repository.findById(999));
        var first = new Record("Carvel", "carvel@example.com", 10, true, 1.5);
        first.ignored = "temporary";
        var second = new Record("Maria", null, 20, false, -2.0);
        repository.save(first);
        repository.save(second);
        assertTrue(first.id > 0);
        assertTrue(second.id > first.id);

        var loaded = repository.findById(first.id);
        assertEquals(first.name, loaded.name);
        assertEquals(first.email, loaded.email);
        assertEquals(10, loaded.total);
        assertTrue(loaded.active);
        assertEquals(1.5, loaded.score);
        assertNull(loaded.optional);
        assertNull(loaded.ignored);
        assertNotSame(first, loaded);

        first.name = "D'Ávila";
        first.email = null;
        first.total = Long.MAX_VALUE;
        first.active = false;
        first.score = 2.75;
        first.optional = Integer.MIN_VALUE;
        repository.update(first);
        loaded = repository.findById(first.id);
        assertEquals("D'Ávila", loaded.name);
        assertNull(loaded.email);
        assertEquals(Long.MAX_VALUE, loaded.total);
        assertEquals(Integer.MIN_VALUE, loaded.optional);
        assertFalse(loaded.active);
        assertEquals(2.75, loaded.score);
        assertEquals("Maria", repository.findById(second.id).name);

        repository.delete(first);
        assertNull(repository.findById(first.id));
        assertEquals(Set.of(second.id), ids(repository.findAll()));
        repository.delete(first);
        assertEquals(1, repository.findAll().size());
        assertFalse(connection.isClosed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "D'Ávila", "São Paulo 日本語 😀", "line1\nline2\tend", "'; DROP TABLE records; --", "SELECT * FROM records WHERE ; 'quoted'"})
    void roundTripsDifficultTextThroughAllQueryApis (String text) throws Exception {

        var record = new Record(text, text, -7, true, 0.5);
        repository.save(record);
        assertEquals(text, repository.findById(record.id).name);
        assertEquals(Set.of(record.id), ids(repository.findBy("name", text, Operator.EQUAL)));
        assertEquals(Set.of(record.id), ids(repository.findByConditions(List.of(new QueryCondition("name", text, Operator.EQUAL)))));
        assertEquals(Set.of(record.id), ids(repository.findByFilters(List.of(new QueryFilter(new QueryCondition("name", text, Operator.EQUAL), null)))));
        record.email = text + "'updated";
        repository.update(record);
        assertEquals(text + "'updated", repository.findById(record.id).email);
        assertEquals(1, repository.findAll().size());
        repository.delete(record);
        assertTrue(repository.findAll().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"EQUAL, 2", "NOT_EQUAL, 1:3", "GREATER_THAN, 3", "LESS_THAN, 1", "GREATER_THAN_OR_EQUAL, 2:3", "LESS_THAN_OR_EQUAL, 1:2"})
    void evaluatesAllComparisonOperators (Operator operator, String expected) throws Exception {

        seed();
        Set<Integer> expectedIds = java.util.Arrays.stream(expected.split(":")).map(Integer::valueOf).collect(Collectors.toSet());
        assertEquals(expectedIds, ids(repository.findBy("id", 2, operator)));
    }

    @Test
    void evaluatesAndOrNullsAndNativeSqlPrecedence () throws Exception {

        seed();
        assertEquals(Set.of(2, 3), ids(repository.findBy("email", null, Operator.EQUAL)));
        assertEquals(Set.of(1), ids(repository.findBy("email", null, Operator.NOT_EQUAL)));
        assertEquals(Set.of(3), ids(repository.findByConditions(List.of(new QueryCondition("id", 1, Operator.GREATER_THAN),
                new QueryCondition("name", "Carvel", Operator.EQUAL)))));
        assertEquals(Set.of(1, 2), ids(repository.findByFilters(List.of(
                new QueryFilter(new QueryCondition("id", 1, Operator.EQUAL), null),
                new QueryFilter(new QueryCondition("name", "Maria", Operator.EQUAL), LogicalOperator.OR)))));
        assertEquals(Set.of(1, 3), ids(repository.findByFilters(List.of(
                new QueryFilter(new QueryCondition("id", 1, Operator.EQUAL), null),
                new QueryFilter(new QueryCondition("name", "Carvel", Operator.EQUAL), LogicalOperator.OR),
                new QueryFilter(new QueryCondition("id", 2, Operator.GREATER_THAN), LogicalOperator.AND)))));
        assertTrue(repository.findBy("name", "missing", Operator.EQUAL).isEmpty());
    }

    @Test
    void supportsManualKeysAndRejectsDuplicatePrimaryKeyWithoutLosingData () throws Exception {

        var manual = new Repository<>(TestEntities.ManualRecord.class, connection);
        manual.createTable();
        var item = new TestEntities.ManualRecord(42, "original");
        manual.save(item);
        assertEquals(42, item.id);
        assertThrows(SQLException.class, () -> manual.save(new TestEntities.ManualRecord(42, "duplicate")));
        assertEquals("original", manual.findById(42).label);
        item.label = "updated";
        manual.update(item);
        assertEquals("updated", manual.findById(42).label);
        manual.delete(item);
        assertTrue(manual.findAll().isEmpty());
    }

    @Test
    void supportsLongGeneratedKeysAndTextKeysWithoutQuotes () throws Exception {

        var longs = new Repository<>(TestEntities.LongRecord.class, connection);
        longs.createTable();
        var record = new TestEntities.LongRecord();
        record.label = "long key";
        longs.save(record);
        assertEquals(1L, record.id);
        assertEquals("long key", longs.findById(record.id).label);
        record.label = "updated";
        longs.update(record);
        assertEquals("updated", longs.findById(record.id).label);
        longs.delete(record);
        assertTrue(longs.findAll().isEmpty());

        var texts = new Repository<>(TestEntities.TextRecord.class, connection);
        texts.createTable();
        var text = new TestEntities.TextRecord("key-1", "label");
        texts.save(text);
        assertEquals("label", texts.findById(text.id).label);
        text.label = "updated";
        texts.update(text);
        assertEquals("updated", texts.findById(text.id).label);
        texts.delete(text);
        assertTrue(texts.findAll().isEmpty());
    }

    @Test
    void roundTripsBoundariesAndLongText () throws Exception {

        var record = new Record("á'".repeat(5000), "", Long.MIN_VALUE, false, Double.MAX_VALUE);
        record.optional = Integer.MAX_VALUE;
        repository.save(record);
        var loaded = repository.findById(record.id);
        assertEquals(record.name, loaded.name);
        assertEquals(Long.MIN_VALUE, loaded.total);
        assertEquals(Integer.MAX_VALUE, loaded.optional);
        assertEquals(Double.MAX_VALUE, loaded.score);
    }

    @Test
    void participatesInCallerManagedRollbackAndCommit () throws Exception {

        connection.setAutoCommit(false);
        var record = new Record("rollback", null, 1, true, 1);
        repository.save(record);
        assertEquals(1, repository.findAll().size());
        connection.rollback();
        assertTrue(repository.findAll().isEmpty());

        repository.save(record);
        connection.commit();
        record.name = "rolled back update";
        repository.update(record);
        connection.rollback();
        assertEquals("rollback", repository.findById(record.id).name);
        repository.delete(record);
        connection.rollback();
        assertNotNull(repository.findById(record.id));
        repository.delete(record);
        connection.commit();
        assertTrue(repository.findAll().isEmpty());
        assertFalse(connection.getAutoCommit());
    }

    @Test
    void committedDataBecomesVisibleToAnotherRepositoryOnAnotherConnection () throws Exception {

        String url = "jdbc:h2:mem:shared_" + UUID.randomUUID().toString().replace("-", "");

        try (var writer = DriverManager.getConnection(url); var reader = DriverManager.getConnection(url)) {

            var writingRepository = new Repository<>(Record.class, writer);
            var readingRepository = new Repository<>(Record.class, reader);
            writingRepository.createTable();
            writer.setAutoCommit(false);
            var record = new Record("committed", null, 0, true, 0);
            writingRepository.save(record);
            assertTrue(readingRepository.findAll().isEmpty());
            writer.commit();
            assertEquals("committed", readingRepository.findById(record.id).name);
            record.name = "not committed";
            writingRepository.update(record);
            assertEquals("committed", readingRepository.findById(record.id).name);
            writer.rollback();
            assertEquals("committed", readingRepository.findById(record.id).name);
        }
    }

    @Test
    void identicalEntitySchemasInDifferentDatabasesRemainIsolated () throws Exception {

        repository.save(new Record("first database", null, 1, true, 0));

        try (var otherConnection = DriverManager.getConnection("jdbc:h2:mem:")) {

            var other = new Repository<>(Record.class, otherConnection);
            other.createTable();
            assertTrue(other.findAll().isEmpty());
            other.save(new Record("second database", null, 2, false, 0));
            assertEquals("first database", repository.findById(1).name);
            assertEquals("second database", other.findById(1).name);
        }
    }

    @Test
    void supportsSaveAndSearchForEntityWithoutPrimaryKey () throws Exception {

        var noId = new Repository<>(TestEntities.NoId.class, connection);
        noId.createTable();
        var entity = new TestEntities.NoId();
        entity.label = "without key";
        noId.save(entity);
        assertEquals("without key", noId.findAll().get(0).label);
        assertEquals(1, noId.findBy("label", entity.label, Operator.EQUAL).size());
        assertThrows(IllegalArgumentException.class, () -> noId.findById(1));
        assertThrows(IllegalArgumentException.class, () -> noId.update(entity));
        assertThrows(IllegalArgumentException.class, () -> noId.delete(entity));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void roundTripsBoxedFieldsWithValuesOrNulls (boolean nulls) throws Exception {

        var boxed = new Repository<>(TestEntities.BoxedRecord.class, connection);
        boxed.createTable();
        var entity = new TestEntities.BoxedRecord();

        if (!nulls) {

            entity.total = Long.MAX_VALUE;
            entity.active = true;
            entity.score = -1.25;
        }

        boxed.save(entity);
        assertEquals(1, entity.id);
        var loaded = boxed.findById(entity.id);
        assertEquals(entity.total, loaded.total);
        assertEquals(entity.active, loaded.active);
        assertEquals(entity.score, loaded.score);
        entity.total = nulls ? 1L : null;
        boxed.update(entity);
        assertEquals(entity.total, boxed.findById(entity.id).total);
        boxed.delete(entity);
        assertTrue(boxed.findAll().isEmpty());
    }

    @Test
    void supportsTheProjectsUserEntity () throws Exception {

        var users = new Repository<>(User.class, connection);
        users.createTable();
        var user = new User(99, "Carvel", null, null, "ignored");
        users.save(user);
        assertEquals(1, user.getId());
        assertEquals("Carvel", users.findById(user.getId()).getName());
        assertNull(users.findById(user.getId()).getTemporary());
        users.update(new User(user.getId(), "updated", "email", null, null));
        assertEquals("updated", users.findById(user.getId()).getName());
        users.delete(user);
        assertTrue(users.findAll().isEmpty());
    }

    @Test
    void rejectsInvalidQueriesAndEntitiesWithoutChangingExistingRows () throws Exception {

        assertThrows(IllegalArgumentException.class, () -> new Repository<>(null, connection));
        assertThrows(IllegalArgumentException.class, () -> new Repository<>(Record.class, null));
        seed();
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        assertThrows(IllegalArgumentException.class, () -> repository.update(null));
        assertThrows(IllegalArgumentException.class, () -> repository.delete(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findById(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findBy("unknown", 1, Operator.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> repository.findBy("name", null, Operator.GREATER_THAN));
        assertThrows(IllegalArgumentException.class, () -> repository.findByConditions(List.of()));
        assertThrows(IllegalArgumentException.class, () -> repository.findByFilters(List.of()));
        assertThrows(SQLException.class, repository::createTable);
        assertEquals(Set.of(1, 2, 3), ids(repository.findAll()));
    }

    @Test
    void missingUpdateAndDeleteAreNoOpsAndClosedConnectionFails () throws Exception {

        seed();
        var missing = new Record("missing", null, 0, false, 0);
        missing.id = 999;
        repository.update(missing);
        repository.delete(missing);
        assertEquals(Set.of(1, 2, 3), ids(repository.findAll()));
        connection.close();
        assertThrows(SQLException.class, repository::findAll);
        assertThrows(SQLException.class, () -> repository.save(missing));
    }

    @Test
    @Timeout(30)
    void processesFiveHundredRecordsAndMaintainsIsolation () throws Exception {

        for (int i = 0; i < 500; i++) {

            repository.save(new Record("record-" + i, null, i, i % 2 == 0, i / 2.0));
        }

        var all = repository.findAll();
        assertEquals(500, all.size());
        assertEquals(500, ids(all).size());
        assertEquals(250, repository.findBy("active", true, Operator.EQUAL).size());
        var subset = repository.findBy("id", 250, Operator.GREATER_THAN);
        assertEquals(250, subset.size());

        for (Record record : subset) {

            record.name = "updated";
            repository.update(record);
        }

        assertEquals(250, repository.findBy("name", "updated", Operator.EQUAL).size());

        for (Record record : subset) {

            repository.delete(record);
        }

        assertEquals(250, repository.findAll().size());
        assertEquals(0, repository.findBy("name", "updated", Operator.EQUAL).size());
    }

    private void seed () throws Exception {

        repository.save(new Record("Carvel", "one@example.com", 10, true, 1));
        repository.save(new Record("Maria", null, 20, false, 2));
        repository.save(new Record("Carvel", null, 30, true, 3));
    }

    @Test
    @Timeout(30)
    void matchesAnIndependentJavaOracleForDeterministicMixedFilters () throws Exception {

        var random = new Random(20261008L);

        for (int i = 0; i < 40; i++) {

            repository.save(new Record("group-" + random.nextInt(4), null, random.nextInt(101) - 50, random.nextBoolean(), i));
        }

        var records = repository.findAll();

        for (int i = 0; i < 30; i++) {

            long threshold = random.nextInt(101) - 50;
            String name = "group-" + random.nextInt(4);
            boolean active = random.nextBoolean();
            Set<Integer> expectedAnd = records.stream().filter(record -> record.total >= threshold && record.name.equals(name))
                    .map(record -> record.id).collect(Collectors.toSet());
            var andConditions = List.of(new QueryCondition("total", threshold, Operator.GREATER_THAN_OR_EQUAL),
                    new QueryCondition("name", name, Operator.EQUAL));
            assertEquals(expectedAnd, ids(repository.findByConditions(andConditions)), "AND iteration " + i);
            Set<Integer> expectedMixed = records.stream().filter(record -> record.total >= threshold || record.name.equals(name) && record.active == active)
                    .map(record -> record.id).collect(Collectors.toSet());
            var mixed = List.of(new QueryFilter(andConditions.get(0), null), new QueryFilter(andConditions.get(1), LogicalOperator.OR),
                    new QueryFilter(new QueryCondition("active", active, Operator.EQUAL), LogicalOperator.AND));
            assertEquals(expectedMixed, ids(repository.findByFilters(mixed)), "OR/AND iteration " + i);
        }
    }

    private Set<Integer> ids (List<Record> records) {

        return records.stream().map(record -> record.id).collect(Collectors.toSet());
    }
}

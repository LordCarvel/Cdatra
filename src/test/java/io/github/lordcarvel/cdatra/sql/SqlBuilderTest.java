package io.github.lordcarvel.cdatra.sql;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlBuilderTest {

    @Test
    void buildsTextOnlyQueryWithoutParameters () {

        var query = new SqlBuilder().append("SELECT * FROM ").append("people").append(";").build();

        assertEquals("SELECT * FROM people;", query.getSql());
        assertTrue(query.getParameters().isEmpty());
    }

    @Test
    void keepsPlaceholdersAndParametersInTheSameOrder () {

        var query = new SqlBuilder()
                .append("UPDATE people SET name = ").appendParameter("D'Ávila")
                .append(", total = ").appendParameter(5L)
                .append(" WHERE id = ").appendParameter(7)
                .append(";")
                .build();

        assertEquals("UPDATE people SET name = ?, total = ? WHERE id = ?;", query.getSql());
        assertEquals(List.of("D'Ávila", 5L, 7), query.getParameters());
    }

    @Test
    void acceptsNullAsParameterValue () {

        var query = new SqlBuilder().append("UPDATE people SET email = ").appendParameter(null).append(";").build();

        assertEquals("UPDATE people SET email = ?;", query.getSql());
        assertEquals(Arrays.asList((Object) null), query.getParameters());
    }

    @Test
    void rejectsNullText () {

        assertThrows(IllegalArgumentException.class, () -> new SqlBuilder().append(null));
    }

    @Test
    void rejectsBuildingAnEmptyQuery () {

        assertThrows(IllegalArgumentException.class, () -> new SqlBuilder().build());
        assertThrows(IllegalArgumentException.class, () -> new SqlBuilder().append(" ").build());
    }

    @Test
    void builtQueryIsNotAffectedByLaterAppends () {

        var builder = new SqlBuilder().append("SELECT * FROM people WHERE id = ").appendParameter(1);
        var first = builder.build();

        builder.append(" OR id = ").appendParameter(2);

        assertEquals("SELECT * FROM people WHERE id = ?", first.getSql());
        assertEquals(List.of(1), first.getParameters());
        assertEquals(List.of(1, 2), builder.build().getParameters());
    }
}

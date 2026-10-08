package io.github.lordcarvel.cdatra.support;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.annotation.Entity;
import io.github.lordcarvel.cdatra.annotation.GeneratedValue;
import io.github.lordcarvel.cdatra.annotation.Id;

public final class TestEntities {

    private TestEntities () {

    }

    @Entity(tableNaame = "records")
    public static class Record {

        @Id @GeneratedValue @Column(columName = "id") public int id;
        @Column(columName = "name") public String name;
        @Column(columName = "email") public String email;
        @Column(columName = "total") public long total;
        @Column(columName = "active") public boolean active;
        @Column(columName = "score") public double score;
        @Column(columName = "optional") public Integer optional;
        public String ignored;

        public Record () {

        }

        public Record (String name, String email, long total, boolean active, double score) {

            this.name = name;
            this.email = email;
            this.total = total;
            this.active = active;
            this.score = score;
        }
    }

    @Entity(tableNaame = "manual_records")
    public static class ManualRecord {

        @Id @Column(columName = "id") public int id;
        @Column(columName = "label") public String label;

        public ManualRecord () {

        }

        public ManualRecord (int id, String label) {

            this.id = id;
            this.label = label;
        }
    }

    @Entity(tableNaame = "long_records")
    public static class LongRecord {

        @Id @GeneratedValue @Column(columName = "id") public long id;
        @Column(columName = "label") public String label;

        public LongRecord () {

        }
    }

    @Entity(tableNaame = "text_records")
    public static class TextRecord {

        @Id @Column(columName = "id") public String id;
        @Column(columName = "label") public String label;

        public TextRecord () {

        }

        public TextRecord (String id, String label) {

            this.id = id;
            this.label = label;
        }
    }

    @Entity(tableNaame = "only_id")
    public static class OnlyId {

        @Id @GeneratedValue @Column(columName = "id") public int id;

        public OnlyId () {

        }
    }

    @Entity(tableNaame = "no_id_records")
    public static class NoId {

        @Column(columName = "label") public String label;

        public NoId () {

        }
    }

    @Entity(tableNaame = "boxed_records")
    public static class BoxedRecord {

        @Id @GeneratedValue @Column(columName = "id") public Integer id;
        @Column(columName = "total") public Long total;
        @Column(columName = "active") public Boolean active;
        @Column(columName = "score") public Double score;

        public BoxedRecord () {

        }
    }

    public static class Parent {

        @Id @Column(columName = "id") public int id;
    }

    @Entity(tableNaame = "inherited_records")
    public static class Child extends Parent {

        @Column(columName = "label") public String label;

        public Child () {

        }
    }
}

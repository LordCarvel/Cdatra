package io.github.lordcarvel.cdatra.metadata;


import java.util.List;

public class SchemaBuilder {

    public Table build (String name, List<ColumnDefinition> columnDefinitionList) {

        return new Table(name, columnDefinitionList);
    }
}

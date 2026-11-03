package io.github.lordcarvel.cdatra.query;

import java.util.List;

public class QueryFilter {

    private QueryCondition condition;
    private LogicalOperator logicalOperator;
    private List<QueryFilter> group;

    public QueryFilter (QueryCondition condition, LogicalOperator logicalOperator) {

        this.condition = condition;
        this.logicalOperator = logicalOperator;
    }

    public static QueryFilter group (List<QueryFilter> group, LogicalOperator logicalOperator) {

        QueryFilter filter = new QueryFilter(null, logicalOperator);

        filter.group = group;

        return filter;
    }

    public QueryCondition getCondition () {

        return condition;
    }

    public LogicalOperator getLogicalOperator () {

        return logicalOperator;
    }

    public List<QueryFilter> getGroup () {

        return group;
    }
}
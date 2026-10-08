package org.com.dev.carvel.query;

public class QueryFilter {

    private QueryCondition condition;
    private LogicalOperator logicalOperator;

    public QueryFilter (QueryCondition condition, LogicalOperator logicalOperator) {

        this.condition = condition;
        this.logicalOperator = logicalOperator;
    }

    public QueryCondition getCondition () {

        return condition;
    }

    public LogicalOperator getLogicalOperator () {

        return logicalOperator;
    }
}

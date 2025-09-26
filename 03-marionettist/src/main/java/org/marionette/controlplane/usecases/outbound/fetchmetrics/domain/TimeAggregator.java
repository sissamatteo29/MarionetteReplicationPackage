package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

public enum TimeAggregator {
    SUM("sum"),
    AVERAGE("avg"),
    MIN("min"),
    MAX("max"),
    RATE("rate"),
    INCREASE("increase");

    private final String operator;

    TimeAggregator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public static TimeAggregator fromString(String timeAggregator) {
        for (TimeAggregator t : TimeAggregator.values()) {
            if (t.operator.equalsIgnoreCase(timeAggregator)) {
                return t;
            }
        }
        throw new IllegalArgumentException("There is no time aggregator called " + timeAggregator);
    }

    @Override
    public String toString() {
        return operator;
    }

}
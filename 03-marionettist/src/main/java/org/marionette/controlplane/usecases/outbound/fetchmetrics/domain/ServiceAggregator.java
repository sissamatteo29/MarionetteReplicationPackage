package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

/*
 * Specifies aggregation at the level of service, so for multiple instant points returned for the same service
 */
public enum ServiceAggregator {

    SUM("sum"),
    AVERAGE("avg"),
    MIN("min"),
    MAX("max");

    private final String operator;

    ServiceAggregator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public static ServiceAggregator fromString(String serviceAggregator) {
        for (ServiceAggregator t : ServiceAggregator.values()) {
            if (t.operator.equalsIgnoreCase(serviceAggregator)) {
                return t;
            }
        }
        throw new IllegalArgumentException("There is no service aggregator called " + serviceAggregator);
    }

    @Override
    public String toString() {
        return operator;
    }

}

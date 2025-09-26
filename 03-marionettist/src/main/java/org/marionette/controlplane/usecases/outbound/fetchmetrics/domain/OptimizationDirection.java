package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

public enum OptimizationDirection {

    HIGHER_IS_BETTER("higher"),
    LOWER_IS_BETTER("lower");

    private final String direction;

    OptimizationDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }

    public static OptimizationDirection fromString(String direction) {
        for (OptimizationDirection o : OptimizationDirection.values()) {
            if (o.direction.equalsIgnoreCase(direction)) {
                return o;
            }
        }
        throw new IllegalArgumentException("There is no optimization direction called " + direction);
    }

    @Override
    public String toString() {
        return direction;
    }

}

package org.marionette.controlplane.usecases.outbound.servicemanipulation;

public record ChangeBehaviourData (String serviceName, String className, String methodName, String newBehaviourId) {}

package org.marionette.controlplane.usecases.inbound.abntest.domain;

import org.marionette.controlplane.domain.values.BehaviourIdSet;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;

/**
 * Supporting data structure to represent a possible variation in the whole system
 */
public record VariationPoint (ServiceName serviceName, ClassName className, MethodName methodName, BehaviourIdSet behaviours) {}

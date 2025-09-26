package org.marionette.controlplane.usecases.inbound.abntest.engine;

import java.time.Duration;
import java.util.List;

import org.marionette.controlplane.usecases.inbound.abntest.domain.GlobalMetricsRegistry;
import org.marionette.controlplane.usecases.inbound.abntest.domain.SystemBehaviourConfiguration;

public interface AbnTestExecutor {

    public GlobalMetricsRegistry executeAbnTest(List<SystemBehaviourConfiguration> systemConfigurations, Duration totalTime);
    
}

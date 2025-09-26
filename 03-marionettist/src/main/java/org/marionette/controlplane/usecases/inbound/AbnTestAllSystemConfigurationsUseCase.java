package org.marionette.controlplane.usecases.inbound;

import java.time.Duration;

public interface AbnTestAllSystemConfigurationsUseCase {

    public AbnTestResult execute();
    
    public AbnTestResult execute(Duration totalDuration);
    
}

package org.marionette.controlplane.usecases.inbound;

public interface TriggerServiceRediscoveryUseCase {

    /**
     * Triggers a complete service rediscovery:
     * 1. Flushes the current ConfigRegistry
     * 2. Restarts the full discovery process
     * 3. Repopulates the registry with fresh data
     */
    void execute();
    
}

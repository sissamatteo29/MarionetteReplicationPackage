package org.marionette.controlplane.usecases.inbound.rediscovery;

import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.usecases.inbound.FullMarionetteServiceConfigDiscoveryUseCase;
import org.marionette.controlplane.usecases.inbound.TriggerServiceRediscoveryUseCase;
import org.springframework.stereotype.Service;

import static java.util.Objects.requireNonNull;

@Service
public class TriggerServiceRediscoveryUseCaseImpl implements TriggerServiceRediscoveryUseCase {

    private final ConfigRegistry configRegistry;
    private final FullMarionetteServiceConfigDiscoveryUseCase discoveryUseCase;

    public TriggerServiceRediscoveryUseCaseImpl(
            ConfigRegistry configRegistry,
            FullMarionetteServiceConfigDiscoveryUseCase discoveryUseCase) {
        this.configRegistry = requireNonNull(configRegistry, "ConfigRegistry cannot be null");
        this.discoveryUseCase = requireNonNull(discoveryUseCase, "FullMarionetteServiceConfigDiscoveryUseCase cannot be null");
    }

    @Override
    public void execute() {
        System.out.println("🔄 Starting service rediscovery process...");
        System.out.println("=" .repeat(50));
        
        try {
            // Step 1: Flush the current registry
            configRegistry.flushAll();
            
            // Step 2: Restart the discovery process
            System.out.println("🔍 Restarting service discovery...");
            discoveryUseCase.execute();
            
            System.out.println("✅ Service rediscovery completed successfully");
            System.out.println("=" .repeat(50));
            
        } catch (Exception e) {
            System.err.println("❌ Service rediscovery failed: " + e.getMessage());
            throw new RuntimeException("Failed to complete service rediscovery", e);
        }
    }
}

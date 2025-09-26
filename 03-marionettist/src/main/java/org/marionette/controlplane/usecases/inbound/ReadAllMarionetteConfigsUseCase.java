package org.marionette.controlplane.usecases.inbound;

import org.marionette.controlplane.usecases.inbound.readconfigs.ReadAllMarionetteConfigsResponse;

public interface ReadAllMarionetteConfigsUseCase {

    public ReadAllMarionetteConfigsResponse execute();
    
}

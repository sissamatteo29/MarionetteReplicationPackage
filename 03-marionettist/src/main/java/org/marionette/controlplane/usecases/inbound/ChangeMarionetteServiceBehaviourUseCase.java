package org.marionette.controlplane.usecases.inbound;

import org.marionette.controlplane.usecases.inbound.changebehaviour.ChangeMarionetteServiceBehaviourRequest;

public interface ChangeMarionetteServiceBehaviourUseCase {

    public void execute(ChangeMarionetteServiceBehaviourRequest request);
    
}

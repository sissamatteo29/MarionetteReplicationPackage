package org.marionette.controlplane.usecases.outbound.servicemanipulation;

public interface ControlMarionetteServiceBehaviourGateway {

    public void changeMarionetteServiceBehaviour(String serviceEndpoint, ChangeBehaviourData changeBehaviourData);
    
}

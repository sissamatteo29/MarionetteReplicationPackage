package org.marionette.controlplane.usecases.inbound.changebehaviour;

import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;
import org.marionette.controlplane.usecases.inbound.ChangeMarionetteServiceBehaviourUseCase;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ChangeBehaviourData;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ControlMarionetteServiceBehaviourGateway;

import static java.util.Objects.requireNonNull;

import java.net.URI;

public class ChangeMarionetteServiceBehaviourUseCaseImpl implements ChangeMarionetteServiceBehaviourUseCase {

    private final ConfigRegistry globalRegistry;
    private final ControlMarionetteServiceBehaviourGateway controlMarionetteBehaviourGateway;

    public ChangeMarionetteServiceBehaviourUseCaseImpl(ConfigRegistry globalRegistry, ControlMarionetteServiceBehaviourGateway controlMarionetteBehaviourGateway) {

        requireNonNull(globalRegistry, "The global registry cannot be null");
        requireNonNull(controlMarionetteBehaviourGateway, "The mariontte gateway to contact marionette nodes cannot be null");

        this.globalRegistry = globalRegistry;
        this.controlMarionetteBehaviourGateway = controlMarionetteBehaviourGateway;
    }

    @Override
    public void execute(ChangeMarionetteServiceBehaviourRequest request) {

        requireNonNull(request, "The request object is null for this use case");

        ServiceName serviceName = new ServiceName(request.serviceName());
        ClassName className = new ClassName(request.className());
        MethodName methodName = new MethodName(request.methodName());
        BehaviourId newBehaviourId = new BehaviourId(request.newBehaviourId());
        
        // TODO: modify validation practice, validate before asking entity to change
        
        globalRegistry.modifyCurrentBehaviourForMethod(serviceName, className, methodName, newBehaviourId);

        System.out.println("Updated the global configuration registry with the change of behaviour: \n"
            + "     serviceName: " + serviceName + "\n"
            + "     className: " + className + "\n"
            + "     beahviour: " + newBehaviourId);

        URI serviceEndpoint = globalRegistry.getEndpointOfService(serviceName);

        System.out.println("Sending request out to service at " + serviceEndpoint);

        controlMarionetteBehaviourGateway.changeMarionetteServiceBehaviour(
            serviceEndpoint.toString(),
            new ChangeBehaviourData(
                request.serviceName(),
                request.className(),
                request.methodName(),
                request.newBehaviourId()
        ));

    }
    
}

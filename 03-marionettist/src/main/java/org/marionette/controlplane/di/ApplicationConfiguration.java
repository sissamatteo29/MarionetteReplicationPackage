package org.marionette.controlplane.di;

import org.marionette.controlplane.adapters.inbound.controllers.ConfigurationController;
import org.marionette.controlplane.adapters.outbound.changeconfig.ControlMarionetteServiceBehaviourAdapter;
import org.marionette.controlplane.adapters.outbound.fetchconfig.HttpFetchMarionetteConfigAdapter;
import org.marionette.controlplane.adapters.outbound.servicediscovery.HttpValidateMarionetteServiceAdapter;
import org.marionette.controlplane.adapters.outbound.servicediscovery.KubernetesFindServicesAdapter;
import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.usecases.inbound.AbnTestAllSystemConfigurationsUseCase;
import org.marionette.controlplane.usecases.inbound.ChangeMarionetteServiceBehaviourUseCase;
import org.marionette.controlplane.usecases.inbound.FullMarionetteServiceConfigDiscoveryUseCase;
import org.marionette.controlplane.usecases.inbound.ReadAllMarionetteConfigsUseCase;
import org.marionette.controlplane.usecases.inbound.TriggerServiceRediscoveryUseCase;
import org.marionette.controlplane.usecases.inbound.changebehaviour.ChangeMarionetteServiceBehaviourUseCaseImpl;
import org.marionette.controlplane.usecases.inbound.fulldiscovery.FullMarionetteServiceConfigDiscoveryUseCaseImpl;
import org.marionette.controlplane.usecases.inbound.readconfigs.ReadAllMarionetteConfigsUseCaseImpl;
import org.marionette.controlplane.usecases.outbound.fetchconfig.FetchMarionetteConfigurationGateway;
import org.marionette.controlplane.usecases.outbound.servicediscovery.FindCandidateServicesPort;
import org.marionette.controlplane.usecases.outbound.servicediscovery.ValidateMarionetteServicePort;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ControlMarionetteServiceBehaviourGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @Scope("singleton")
    public ConfigRegistry configRegistry() {
        return new ConfigRegistry();
    }

    @Bean
    public FindCandidateServicesPort createFindServicesPort() {
        return new KubernetesFindServicesAdapter();
    }

    @Bean 
    public ConfigurationController configurationController(
            ReadAllMarionetteConfigsUseCase readAllMarionetteConfigsUseCase, 
            ChangeMarionetteServiceBehaviourUseCase changeMarionetteServiceBehaviourUseCase,
            TriggerServiceRediscoveryUseCase triggerServiceRediscoveryUseCase,
            AbnTestAllSystemConfigurationsUseCase abnTestUseCase) {
        return new ConfigurationController(readAllMarionetteConfigsUseCase, changeMarionetteServiceBehaviourUseCase, triggerServiceRediscoveryUseCase, abnTestUseCase);
    }

    @Bean
    public ReadAllMarionetteConfigsUseCase readMarionetteConfigsUseCase(ConfigRegistry globalRegistry) {
        return new ReadAllMarionetteConfigsUseCaseImpl(globalRegistry);

    }

    @Bean
    public FetchMarionetteConfigurationGateway createNodeConfigGateway() {
        return new HttpFetchMarionetteConfigAdapter();
    }

    @Bean
    public FullMarionetteServiceConfigDiscoveryUseCase fullDiscoveryUseCase(
        FindCandidateServicesPort findServicesPort, 
        ValidateMarionetteServicePort marionetteServiceValidator, 
        FetchMarionetteConfigurationGateway fetchMarionetteConfigurationGateway, 
        ConfigRegistry globalRegistry) {

        return new FullMarionetteServiceConfigDiscoveryUseCaseImpl(
            findServicesPort,
            marionetteServiceValidator,
            fetchMarionetteConfigurationGateway,
            globalRegistry
        );

    }

    @Bean
    public ValidateMarionetteServicePort validateMarionetteServicePort() {
        return new HttpValidateMarionetteServiceAdapter();
    }

    @Bean 
    ChangeMarionetteServiceBehaviourUseCase changeMarionetteServiceBehaviourUseCase(
        ConfigRegistry globalRegistry, 
        ControlMarionetteServiceBehaviourGateway controlMarionetteBehaviourGateway) {
        
        return new ChangeMarionetteServiceBehaviourUseCaseImpl(
            globalRegistry, 
            controlMarionetteBehaviourGateway);
    
    }

    @Bean
    ControlMarionetteServiceBehaviourGateway controlMarionetteServiceBehaviourGateway() {
        return new ControlMarionetteServiceBehaviourAdapter();
    }



}

package org.marionette.controlplane.usecases.inbound.fulldiscovery;

import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.domain.entities.ServiceConfig;
import org.marionette.controlplane.domain.values.ServiceName;
import org.marionette.controlplane.exceptions.infrastructure.checked.FetchMarionetteConfigurationException;
import org.marionette.controlplane.usecases.domain.dto.DiscoveredServiceMetadata;
import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;
import org.marionette.controlplane.usecases.domain.mappers.ServiceConfigDataMapper;
import org.marionette.controlplane.usecases.inbound.FullMarionetteServiceConfigDiscoveryUseCase;
import org.marionette.controlplane.usecases.outbound.fetchconfig.FetchMarionetteConfigurationGateway;
import org.marionette.controlplane.usecases.outbound.servicediscovery.FindCandidateServicesPort;
import org.marionette.controlplane.usecases.outbound.servicediscovery.ValidateMarionetteServicePort;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class FullMarionetteServiceConfigDiscoveryUseCaseImpl implements FullMarionetteServiceConfigDiscoveryUseCase {

    private final FindCandidateServicesPort findServicesPort;
    private final ValidateMarionetteServicePort marionetteServiceValidator;
    private final FetchMarionetteConfigurationGateway fetchMarionetteConfigurationGateway;
    private final ConfigRegistry globalRegistry;

    public FullMarionetteServiceConfigDiscoveryUseCaseImpl(FindCandidateServicesPort findServicesPort,
            ValidateMarionetteServicePort marionetteServiceValidator,
            FetchMarionetteConfigurationGateway fetchMarionetteConfigurationGateway, ConfigRegistry globalRegistry) {

        requireNonNull(findServicesPort, "The outbound port to find candidate services cannot be null");
        requireNonNull(marionetteServiceValidator, "The outbound port to verify marionette services cannot be null");
        requireNonNull(fetchMarionetteConfigurationGateway,
                "The outbound port to fetch a marionette configuration cannot be null");
        requireNonNull(globalRegistry, "The global registry cannot be null");

        this.findServicesPort = findServicesPort;
        this.marionetteServiceValidator = marionetteServiceValidator;
        this.fetchMarionetteConfigurationGateway = fetchMarionetteConfigurationGateway;
        this.globalRegistry = globalRegistry;
    }

    @Override
    public void execute() {

        List<DiscoveredServiceMetadata> candidateServices = findServicesPort.findCandidateServices();
        List<DiscoveredServiceMetadata> validatedServices = candidateServices.stream()
            .filter(marionetteServiceValidator::validateCandidateNode)
            .collect(Collectors.toList());

        logValidatedServices(validatedServices);

        for(DiscoveredServiceMetadata serviceMetadata : validatedServices) {
            try {
                ServiceConfigData serviceConfigData = fetchMarionetteConfigurationGateway.fetchMarionetteConfiguration(serviceMetadata.endpoint());

                // Map to domain object
                ServiceName serviceName = new ServiceName(serviceConfigData.serviceName());
                ServiceConfig serviceConfig = ServiceConfigDataMapper.toDomainServiceConfig(serviceConfigData);
                URI serviceEndpoint = URI.create(serviceMetadata.endpoint());

                // Add it to the registry
                globalRegistry.addDiscoveredService(serviceName, serviceConfig, serviceEndpoint);

            } catch (FetchMarionetteConfigurationException e) {
                // Log it and go to next service
                System.out.println("There was a problem fetching the configuration for the marionette node " + serviceMetadata.endpoint());
            }
        }

    }

    private void logValidatedServices(List<DiscoveredServiceMetadata> validatedServices) {
        System.out.println("\n== MARIONETTE DISCOVERED SERVICES ==");
        for(DiscoveredServiceMetadata serviceMetadata : validatedServices) {
            System.out.println(serviceMetadata.endpoint());
        }
    }

}

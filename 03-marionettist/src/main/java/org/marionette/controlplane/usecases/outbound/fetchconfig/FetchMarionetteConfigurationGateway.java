package org.marionette.controlplane.usecases.outbound.fetchconfig;

import org.marionette.controlplane.exceptions.infrastructure.checked.FetchMarionetteConfigurationException;
import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;

public interface FetchMarionetteConfigurationGateway {

    public ServiceConfigData fetchMarionetteConfiguration(String marionetteServiceEndpoint) throws FetchMarionetteConfigurationException;

}
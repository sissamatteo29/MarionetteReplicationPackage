package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IncludedServicesParser {

    private static final String INCLUDED_SERVICES_ENV_VAR = "MARIONETTE_METRICS_INCLUDED_SERVICES";

    public List<String> parseIncludedServicesFromEnvironment() {
        String includedServicesEnv = System.getenv(INCLUDED_SERVICES_ENV_VAR);

        if (isValidServicesList(includedServicesEnv)) {
            return parseServicesList(includedServicesEnv);
        }

        return Collections.emptyList();
    }

    private boolean isValidServicesList(String servicesList) {
        return servicesList != null && !servicesList.trim().isEmpty();
    }

    private List<String> parseServicesList(String servicesList) {
        return Arrays.stream(servicesList.split(","))
                .map(String::trim)
                .filter(service -> !service.isEmpty())
                .collect(Collectors.toList());
    }

    public String getSupportedEnvironmentVariable() {
        return INCLUDED_SERVICES_ENV_VAR;
    }
}
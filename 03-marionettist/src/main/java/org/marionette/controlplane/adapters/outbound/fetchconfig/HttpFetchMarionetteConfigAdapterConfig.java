package org.marionette.controlplane.adapters.outbound.fetchconfig;

public record HttpFetchMarionetteConfigAdapterConfig (String marionetteEndpointPath) {

    public static HttpFetchMarionetteConfigAdapterConfig defaultConfig() {
        return new HttpFetchMarionetteConfigAdapterConfig("/marionette/api/getConfiguration");
    }

}

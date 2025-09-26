package org.marionette.controlplane.adapters.outbound.fetchconfig;

import org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto.MarionetteServiceConfigDTO;
import org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.mapping.MarionetteConfigMapper;
import org.marionette.controlplane.exceptions.infrastructure.checked.FetchMarionetteConfigurationException;
import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;
import org.marionette.controlplane.usecases.outbound.fetchconfig.FetchMarionetteConfigurationGateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class HttpFetchMarionetteConfigAdapter implements FetchMarionetteConfigurationGateway {

    private final HttpClient httpClient;
    private final HttpFetchMarionetteConfigAdapterConfig config;

    public HttpFetchMarionetteConfigAdapter() {
        this.httpClient = createHttpClient();
        this.config = HttpFetchMarionetteConfigAdapterConfig.defaultConfig();
    }

    public HttpFetchMarionetteConfigAdapter(HttpFetchMarionetteConfigAdapterConfig config) {
        requireNonNull(config, "The config object for the http fetch marionette config adapter is null");

        this.config = config;
        httpClient = createHttpClient();
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public ServiceConfigData fetchMarionetteConfiguration(String marionetteServiceEndpoint) throws FetchMarionetteConfigurationException {
        
        try {
            URI configFetchUri = buildFetchConfigEndpoint(marionetteServiceEndpoint); 
            HttpRequest request = buildHttpRequest(configFetchUri);
            HttpResponse<String> response = sendAndHandleExceptions(request);
            if (response.statusCode() == 200) {
                logResponseSuccessful(marionetteServiceEndpoint, response.body());

                MarionetteServiceConfigDTO dto = mapToDtoAndHandleExceptions(response.body(), configFetchUri.toString());
                ServiceConfigData serviceConfigData = mapToData(dto);                
                return serviceConfigData;
            } else {
                throw new FetchMarionetteConfigurationException(
                "Impossible to retrieve the configuration for the service " + marionetteServiceEndpoint + " because the server returned with code " + response.statusCode(),
                "Impossible to locate the service at the url "  + marionetteServiceEndpoint);
            }
        } catch (Exception e) {     // Global guard against external exceptions
            throw new FetchMarionetteConfigurationException("Unexpected exception when fetching configuration for node " + marionetteServiceEndpoint, 
            e, "Impossible to find configuration for " + marionetteServiceEndpoint);
        }
            
        
    }

    private ServiceConfigData mapToData(MarionetteServiceConfigDTO dto) {
        return MarionetteConfigMapper.toDomainServiceConfigData(dto);
    }

    private MarionetteServiceConfigDTO mapToDtoAndHandleExceptions(String httpResponseBody, String serviceEndpoint)
            throws FetchMarionetteConfigurationException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return (MarionetteServiceConfigDTO) mapper.readValue(httpResponseBody, MarionetteServiceConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new FetchMarionetteConfigurationException(
                    "Impossible to terminate fetch of marionette config for the node " + serviceEndpoint
                            + " because of a parsing error of the response",
                    e,
                    "Impossible to fetch the configuration of the service at the url " + serviceEndpoint);

        }
    }

    private HttpResponse<String> sendAndHandleExceptions(HttpRequest request)
            throws FetchMarionetteConfigurationException {

        try {

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IOException | InterruptedException e) {
            throw new FetchMarionetteConfigurationException(
                    "Impossible to retrieve the configuration for the service " + request.uri()
                            + " because of a network error",
                    e,
                    "Impossible to locate the service at the url " + request.uri());
        }
    }

    private HttpRequest buildHttpRequest(URI configFetchUri) {
        return HttpRequest.newBuilder()
                .uri(configFetchUri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
    }

    private URI buildFetchConfigEndpoint(String marionetteServiceEndpoint) {
        return URI.create(marionetteServiceEndpoint).resolve(URI.create(config.marionetteEndpointPath()));
    }

    private void logResponseSuccessful(String serviceEndpoint, String responseBody) {
        System.out.println("Obtained config from endpoint " + serviceEndpoint);
        System.out.println("### PRINTING CONFIG ###");
        System.out.println(responseBody);
    }

}

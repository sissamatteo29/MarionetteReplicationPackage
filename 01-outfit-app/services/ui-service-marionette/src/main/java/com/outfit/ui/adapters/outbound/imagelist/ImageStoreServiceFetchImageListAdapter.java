package com.outfit.ui.adapters.outbound.imagelist;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outfit.ui.usecases.outbound.imagelist.FetchImageListGateway;

@Component
public class ImageStoreServiceFetchImageListAdapter implements FetchImageListGateway {

    private final ImageStoreServiceFetchImageListAdapterConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ImageStoreServiceFetchImageListAdapter(ImageStoreServiceFetchImageListAdapterConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<String> fetchImageNamesList() {
        try {
            String uri = config.getImageStoreServiceUrl() + "/images";
            
            System.out.println("Fetching image list from: " + uri);

            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .build();

            // Get response as String (JSON)
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                System.out.println("Received JSON response: " + jsonResponse);
                
                // Parse JSON array to List<String>
                List<String> imageNames = objectMapper.readValue(
                    jsonResponse, 
                    new TypeReference<List<String>>() {}
                );
                
                System.out.println("Parsed " + imageNames.size() + " image names: " + imageNames);
                return imageNames;
                
            } else {
                System.err.println("Failed to fetch image list. HTTP Status: " + response.statusCode());
                System.err.println("Response body: " + response.body());
                return new ArrayList<>(); // Return empty list on error
            }
            
        } catch (IOException e) {
            System.err.println("IO error fetching image list: " + e.getMessage());
            return new ArrayList<>();
        } catch (InterruptedException e) {
            System.err.println("Request interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Unexpected error fetching image list: " + e.getMessage());
            return new ArrayList<>();
        }
    }

}
package com.outfit.processor.adapters.outbound.fetchimages;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.outfit.processor.usecases.outbound.fetchimages.FetchImageGateway;

@Component
public class ImageStoreServiceFetchImageAdapter implements FetchImageGateway {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ImageStoreServiceFetchImageAdapterConfig config;

    public ImageStoreServiceFetchImageAdapter(ImageStoreServiceFetchImageAdapterConfig config) {
        this.config = config;
    }

    @Override
    public byte[] fetchImage(String imageName) {
        try {
            // Build the full URL for the image
            String imageUrl = config.getUrl() + "/images/" + imageName;

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30)) // Longer timeout for image downloads
                    .header("Accept", "image/jpeg, image/png, image/*")
                    .build();

            // Send request and get response as byte array
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            // Check if request was successful
            if (response.statusCode() == 200) {
                System.out.println("Successfully fetched image: " + imageName +
                        " (size: " + formatFileSize(response.body().length) + ")");
                return response.body();
            } else if (response.statusCode() == 404) {
                System.out.println("Image not found: " + imageName);
                return null; // Or throw ImageNotFoundException
            } else {
                System.err.println("Failed to fetch image " + imageName +
                        ". HTTP Status: " + response.statusCode());
                throw new RuntimeException("Failed to fetch image: HTTP " + response.statusCode());
            }

        } catch (IOException e) {
            System.err.println("IO error fetching image " + imageName + ": " + e.getMessage());
            throw new RuntimeException("Network error fetching image: " + imageName, e);
        } catch (InterruptedException e) {
            System.err.println("Request interrupted for image " + imageName + ": " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new RuntimeException("Request interrupted: " + imageName, e);
        } catch (Exception e) {
            System.err.println("Unexpected error fetching image " + imageName + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch image: " + imageName, e);
        }
    }

    // Helper method for readable file sizes
    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

}

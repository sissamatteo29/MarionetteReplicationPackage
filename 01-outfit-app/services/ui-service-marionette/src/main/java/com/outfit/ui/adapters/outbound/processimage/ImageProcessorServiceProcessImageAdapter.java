package com.outfit.ui.adapters.outbound.processimage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.outfit.ui.usecases.outbound.processimage.ProcessImageGateway;

@Component
public class ImageProcessorServiceProcessImageAdapter implements ProcessImageGateway {

    private final ImageProcessorServiceProcessImageAdapterConfig config;
    private final HttpClient httpClient;

    public ImageProcessorServiceProcessImageAdapter(ImageProcessorServiceProcessImageAdapterConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public byte[] processImage(String imageId) {
        try {
            // Build the URL for the image processor service
            String processorUrl = config.getImageProcessorServiceUrl() + "/process/" + imageId;
            
            System.out.println("Requesting processed image from: " + processorUrl);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(processorUrl))
                .timeout(Duration.ofSeconds(30))  // Longer timeout for image processing
                .header("Accept", "image/jpeg, image/png, image/*")
                .build();

            // Send request and get response as byte array
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            // Check response status
            if (response.statusCode() == 200) {
                byte[] processedImageData = response.body();
                System.out.println("Successfully received processed image: " + imageId + 
                    " (size: " + formatFileSize(processedImageData.length) + ")");
                return processedImageData;
                
            } else if (response.statusCode() == 404) {
                System.out.println("Image not found in processor service: " + imageId);
                return null;
                
            } else {
                System.err.println("Failed to process image " + imageId + 
                    ". HTTP Status: " + response.statusCode());
                System.err.println("Response body: " + new String(response.body()));
                throw new RuntimeException("Failed to process image: HTTP " + response.statusCode());
            }
            
        } catch (IOException e) {
            System.err.println("IO error processing image " + imageId + ": " + e.getMessage());
            throw new RuntimeException("Network error processing image: " + imageId, e);
        } catch (InterruptedException e) {
            System.err.println("Request interrupted for image " + imageId + ": " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new RuntimeException("Request interrupted: " + imageId, e);
        } catch (Exception e) {
            System.err.println("Unexpected error processing image " + imageId + ": " + e.getMessage());
            throw new RuntimeException("Failed to process image: " + imageId, e);
        }
    }

    // Helper method for readable file sizes
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // Method to check if processor service is available
    public boolean isProcessorServiceAvailable() {
        try {
            String healthUrl = config.getImageProcessorServiceUrl() + "/actuator/health";
            
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(5))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            System.err.println("Processor service health check failed: " + e.getMessage());
            return false;
        }
    }
}
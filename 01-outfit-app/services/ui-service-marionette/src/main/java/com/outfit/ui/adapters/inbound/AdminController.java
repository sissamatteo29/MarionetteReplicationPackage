package com.outfit.ui.adapters.inbound;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Value("${imagestore.service.url:http://localhost:8082}")
    private String imageStoreServiceUrl;

    @Value("${image.processor.service.url:http://localhost:8081}")
    private String imageProcessorServiceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostMapping("/clear-repository")
    public ResponseEntity<Map<String, Object>> clearRepositoryAndCache() {
        Map<String, Object> results = new HashMap<>();
        boolean overallSuccess = true;

        // Clear imagestore repository
        try {
            String repositoryResult = clearImageStoreRepository();
            results.put("imagestore", Map.of(
                    "status", "success",
                    "message", repositoryResult));
        } catch (Exception e) {
            results.put("imagestore", Map.of(
                    "status", "error",
                    "message", "Failed to clear repository: " + e.getMessage()));
            overallSuccess = false;
        }

        // Clear image processor cache
        try {
            String cacheResult = clearImageProcessorCache();
            results.put("imageProcessor", Map.of(
                    "status", "success",
                    "message", cacheResult));
        } catch (Exception e) {
            results.put("imageProcessor", Map.of(
                    "status", "error",
                    "message", "Failed to clear cache: " + e.getMessage()));
            overallSuccess = false;
        }

        results.put("overall", overallSuccess ? "success" : "partial_failure");

        return ResponseEntity.ok(results);
    }

    @PostMapping("/clear-repository-only")
    public ResponseEntity<String> clearRepositoryOnly() {
        try {
            String result = clearImageStoreRepository();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error clearing repository: " + e.getMessage());
        }
    }

    @PostMapping("/clear-cache-only")
    public ResponseEntity<String> clearCacheOnly() {
        try {
            String result = clearImageProcessorCache();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error clearing cache: " + e.getMessage());
        }
    }

    private String clearImageStoreRepository() throws Exception {
        String uri = imageStoreServiceUrl + "/admin/clear-repository";

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private String clearImageProcessorCache() throws Exception {
        String uri = imageProcessorServiceUrl + "/admin/cache";

        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }
}

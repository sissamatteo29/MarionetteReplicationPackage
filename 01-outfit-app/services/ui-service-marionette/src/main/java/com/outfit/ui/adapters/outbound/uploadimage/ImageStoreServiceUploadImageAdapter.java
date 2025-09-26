package com.outfit.ui.adapters.outbound.uploadimage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.outfit.ui.usecases.outbound.uploadimage.UploadImageGateway;

@Component
public class ImageStoreServiceUploadImageAdapter implements UploadImageGateway {

    private final ImageStoreServiceUploadImageAdapterConfig config;
    private final HttpClient client;

    public ImageStoreServiceUploadImageAdapter(ImageStoreServiceUploadImageAdapterConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void uploadImage(byte[] imageData, String imageName) {
        String uri = config.getUploadImageBaseUrl() + "/upload";

        // Create multipart form data boundary
        String boundary = "----OutfitAppUpload" + System.currentTimeMillis();

        // Build multipart form data
        byte[] multipartBody = buildMultipartBody(imageData, imageName, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .timeout(Duration.ofSeconds(30))
                .uri(URI.create(uri))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .build();

        try {
            System.out.println("Uploading image " + imageName + " to imagestore service at: " + uri);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Successfully uploaded image: " + imageName +
                        " (status: " + response.statusCode() + ")");
            } else {
                System.err.println("Failed to upload image " + imageName +
                        ". Status: " + response.statusCode() +
                        ", Response: " + response.body());
                throw new RuntimeException("Upload failed with status: " + response.statusCode());
            }

        } catch (IOException e) {
            System.err.println("IO error while uploading image " + imageName + ": " + e.getMessage());
            throw new RuntimeException("Failed to upload image due to IO error", e);
        } catch (InterruptedException e) {
            System.err.println("Upload interrupted for image " + imageName);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload was interrupted", e);
        }
    }

    private byte[] buildMultipartBody(byte[] imageData, String imageName, String boundary) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Start boundary
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));

            // Content-Disposition header for the file
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + imageName + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            // Content-Type header
            outputStream.write(
                    ("Content-Type: " + determineContentType(imageName) + "\r\n").getBytes(StandardCharsets.UTF_8));

            // Empty line before file content
            outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // File content (binary data) - NO STRING CONVERSION!
            outputStream.write(imageData);

            // End of this part
            outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // End boundary
            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to build multipart body", e);
        }
    }

    private String determineContentType(String filename) {
        String lowercaseFilename = filename.toLowerCase();
        if (lowercaseFilename.endsWith(".jpg") || lowercaseFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowercaseFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowercaseFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowercaseFilename.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
package com.outfit.imagestore.adapters.outbound.imagerepo.marionette;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import com.outfit.imagestore.adapters.outbound.imagerepo.ImageRepositoryConfig;
import com.outfit.imagestore.adapters.outbound.imagerepo.processing.ImageProcessor;

/**
 * Simple image repository that stores images on the filesystem.
 * Uses ImageProcessor to process images before storage.
 */
@Component
public class ImageRepositoryAdapter_NoCompression {

    private final ImageRepositoryConfig config;
    private final ImageProcessor imageProcessor;
    private final AtomicInteger imageCounter;
    private final Path imagesDirectory;
    private final Pattern imageFilePattern;

    public ImageRepositoryAdapter_NoCompression(ImageRepositoryConfig config, ImageProcessor imageProcessor) {
        this.config = config;
        this.imageProcessor = imageProcessor;
        this.imageCounter = new AtomicInteger(0);
        this.imagesDirectory = Paths.get(config.getImageStoragePath());
        this.imageFilePattern = Pattern.compile("^(\\d+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);
    }

    public int putImage(byte[] imageData, String originalExtension) {
        try {
            // Validate image
            if (!imageProcessor.isValidImage(imageData)) {
                throw new IllegalArgumentException("Invalid image data");
            }

            // Save with next available ID
            int imageId = imageCounter.incrementAndGet();
            String filename = imageId + "." + originalExtension;      // Save with original format
            Path imagePath = imagesDirectory.resolve(filename);

            Files.write(imagePath, imageData);

            System.out.println("Saved image " + filename + " in original format " +
                    ", " + formatSize(imageData.length) + ")");

            return imageId;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
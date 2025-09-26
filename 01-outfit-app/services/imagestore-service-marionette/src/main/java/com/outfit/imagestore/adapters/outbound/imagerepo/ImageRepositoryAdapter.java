package com.outfit.imagestore.adapters.outbound.imagerepo;

import java.nio.file.Files;
import java.util.stream.Stream;
import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;
import java.time.Instant;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Comparator;
import com.outfit.imagestore.adapters.outbound.imagerepo.processing.ImageProcessor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.outfit.imagestore.adapters.outbound.imagerepo.ImageRepositoryConfig;
import java.util.List;
import com.outfit.imagestore.adapters.outbound.imagerepo.processing.ImageProcessingException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple image repository that keeps storage under 1GB by automatically
 * deleting oldest images when approaching the limit.
 */
@Component
public class ImageRepositoryAdapter implements ImageRepository {

    // 1GB
    private static final long MAX_STORAGE_BYTES = 1_000_000_000L;

    // 850MB - start cleanup
    private static final long CLEANUP_THRESHOLD_BYTES = 850_000_000L;

    private final ImageRepositoryConfig config;

    private final ImageProcessor imageProcessor;

    private final AtomicInteger imageCounter;

    private final Path imagesDirectory;

    private final Pattern imageFilePattern;

    public ImageRepositoryAdapter(ImageRepositoryConfig config, ImageProcessor imageProcessor) {
        this.config = config;
        this.imageProcessor = imageProcessor;
        this.imageCounter = new AtomicInteger(0);
        this.imagesDirectory = Paths.get(config.getImageStoragePath());
        this.imageFilePattern = Pattern.compile("^(\\d+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);
    }

    @PostConstruct
    public void initializeCounter() throws IOException {
        if (!Files.exists(imagesDirectory)) {
            Files.createDirectories(imagesDirectory);
            System.out.println("Created images directory: " + imagesDirectory.toAbsolutePath());
        }
        int maxCounter = findHighestImageId();
        imageCounter.set(maxCounter);
        long currentSize = calculateTotalSize();
        System.out.println("Initialized image counter to: " + maxCounter);
        System.out.println("Current storage usage: " + formatSize(currentSize) + " / " + formatSize(MAX_STORAGE_BYTES));
        // Clean up if we're already over the threshold
        if (currentSize > CLEANUP_THRESHOLD_BYTES) {
            cleanupOldImages();
        }
    }

    @Override
    public byte[] getImage(String imageName) {
        try {
            Path imagePath = imagesDirectory.resolve(imageName);
            if (Files.exists(imagePath)) {
                return Files.readAllBytes(imagePath);
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + imageName, e);
        }
    }

    @Override
    public int putImage(byte[] imageData, String originalExtension) {
        switch(com.outfit.imagestore.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/imagestore/adapters/outbound/imagerepo/ImageRepositoryAdapter.java", "putImage")) {
            default:
            case "with_compression":
                return putImage_with_compression(imageData, originalExtension);
            case "without_compression":
                return putImage_without_compression(imageData, originalExtension);
        }
    }

    @Override
    public void clearRepository() {
        try {
            if (Files.exists(imagesDirectory)) {
                Files.walk(imagesDirectory).filter(Files::isRegularFile).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
            }
            imageCounter.set(0);
            System.out.println("Repository cleared");
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear repository", e);
        }
    }

    @Override
    public Map<String, Object> getRepositoryStats() {
        try {
            long fileCount = countImages();
            long totalSize = calculateTotalSize();
            return Map.of("fileCount", fileCount, "totalSizeBytes", totalSize, "maxSizeBytes", MAX_STORAGE_BYTES, "usagePercent", (totalSize * 100.0) / MAX_STORAGE_BYTES, "storageDir", imagesDirectory.toString(), "currentCounter", imageCounter.get());
        } catch (Exception e) {
            return Map.of("error", "Failed to get stats");
        }
    }

    /**
     * Delete oldest images until we're under the cleanup threshold
     */
    private void cleanupOldImages() {
        try {
            // Target 100MB below threshold
            long targetSize = CLEANUP_THRESHOLD_BYTES - 100_000_000L;
            long currentSize = calculateTotalSize();
            if (currentSize <= targetSize) {
                return;
            }
            // Get all images sorted by creation time (oldest first)
            List<Path> allImages;
            try (Stream<Path> files = Files.list(imagesDirectory)) {
                allImages = files.filter(Files::isRegularFile).filter(path -> imageFilePattern.matcher(path.getFileName().toString()).matches()).sorted(Comparator.comparing(path -> {
                    try {
                        return Files.readAttributes(path, BasicFileAttributes.class).creationTime();
                    } catch (IOException e) {
                        return java.nio.file.attribute.FileTime.fromMillis(0);
                    }
                })).toList();
            }
            long deletedSize = 0;
            int deletedCount = 0;
            for (Path imagePath : allImages) {
                if (currentSize - deletedSize <= targetSize) {
                    break;
                }
                try {
                    long fileSize = Files.size(imagePath);
                    Files.delete(imagePath);
                    deletedSize += fileSize;
                    deletedCount++;
                } catch (IOException e) {
                    System.err.println("Failed to delete: " + imagePath);
                }
            }
            System.out.println("Cleanup: deleted " + deletedCount + " images, freed " + formatSize(deletedSize) + ". New size: " + formatSize(currentSize - deletedSize));
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }

    // Helper methods
    public List<String> getAllImages() throws IOException {
        try (Stream<Path> files = Files.list(imagesDirectory)) {
            return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).filter(fileName -> imageFilePattern.matcher(fileName).matches()).toList();
        }
    }

    public boolean deleteImage(int imageId) {
        try {
            String[] extensions = { "jpg", "jpeg", "png" };
            for (String ext : extensions) {
                Path imagePath = imagesDirectory.resolve(imageId + "." + ext);
                if (Files.exists(imagePath)) {
                    Files.delete(imagePath);
                    System.out.println("Deleted image: " + imagePath.getFileName());
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image: " + imageId, e);
        }
    }

    private int findHighestImageId() throws IOException {
        try (Stream<Path> files = Files.list(imagesDirectory)) {
            return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).map(imageFilePattern::matcher).filter(matcher -> matcher.matches()).mapToInt(matcher -> Integer.parseInt(matcher.group(1))).max().orElse(0);
        }
    }

    private long countImages() throws IOException {
        try (Stream<Path> files = Files.list(imagesDirectory)) {
            return files.filter(Files::isRegularFile).filter(path -> imageFilePattern.matcher(path.getFileName().toString()).matches()).count();
        }
    }

    private long calculateTotalSize() throws IOException {
        try (Stream<Path> files = Files.walk(imagesDirectory)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public int putImage_with_compression(byte[] imageData, String originalExtension) {
        try {
            // Check if we need to clean up before adding new image
            long currentSize = calculateTotalSize();
            if (currentSize > CLEANUP_THRESHOLD_BYTES) {
                System.out.println("Storage threshold exceeded (" + formatSize(currentSize) + "), cleaning up...");
                cleanupOldImages();
            }
            // Validate image
            if (!imageProcessor.isValidImage(imageData)) {
                throw new IllegalArgumentException("Invalid image data");
            }
            // Process image
            byte[] processedData = imageProcessor.processImage(imageData);
            // Final check - if still no space after cleanup, reject
            currentSize = calculateTotalSize();
            if (currentSize + processedData.length > MAX_STORAGE_BYTES) {
                throw new RuntimeException("Storage full - cannot save image. Current: " + formatSize(currentSize) + ", Required: " + formatSize(processedData.length));
            }
            // Save image
            int imageId = imageCounter.incrementAndGet();
            String filename = imageId + ".jpg";
            Path imagePath = imagesDirectory.resolve(filename);
            Files.write(imagePath, processedData);
            System.out.println("Saved image " + filename + " (" + formatSize(processedData.length) + ") - Total storage: " + formatSize(currentSize + processedData.length));
            return imageId;
        } catch (ImageProcessingException e) {
            throw new RuntimeException("Failed to process image", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

    public int putImage_without_compression(byte[] imageData, String originalExtension) {
        try {
            // Validate image
            if (!imageProcessor.isValidImage(imageData)) {
                throw new IllegalArgumentException("Invalid image data");
            }
            // Save with next available ID
            int imageId = imageCounter.incrementAndGet();
            // Save with original format
            String filename = imageId + "." + originalExtension;
            Path imagePath = imagesDirectory.resolve(filename);
            Files.write(imagePath, imageData);
            System.out.println("Saved image " + filename + " in original format " + ", " + formatSize(imageData.length) + ")");
            return imageId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }
}

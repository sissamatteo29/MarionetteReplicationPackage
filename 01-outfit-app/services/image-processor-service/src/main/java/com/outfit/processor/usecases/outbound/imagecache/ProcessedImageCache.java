package com.outfit.processor.usecases.outbound.imagecache;

/**
 * Cache interface for processed images.
 * This interface defines the contract for caching processed images,
 * following the dependency inversion principle of clean architecture.
 */
public interface ProcessedImageCache {
    
    /**
     * Retrieves a processed image from cache.
     * @param imageName the name of the image
     * @return the processed image data, or null if not found
     */
    byte[] getProcessedImage(String imageName);
    
    /**
     * Stores a processed image in cache.
     * @param imageName the name of the image
     * @param processedImageData the processed image data
     */
    void putProcessedImage(String imageName, byte[] processedImageData);
    
    /**
     * Clears all cached images.
     */
    void clearCache();
    
    /**
     * Gets cache statistics for monitoring and admin purposes.
     * @return cache statistics including hit rate, size, etc.
     */
    CacheStatistics getStatistics();
}
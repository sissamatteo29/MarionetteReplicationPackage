package com.outfit.processor.usecases.outbound.imagecache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration for the image cache with sensible defaults.
 * 
 * Default reasoning:
 * - 50 images max: Reasonable for a service handling moderate load
 * - 100MB max memory: ~2MB average per image (allows for larger images)
 * - These defaults work well for most scenarios without configuration
 */
@Component
public class InMemoryImageCacheConfig {
    
    /**
     * Maximum number of images to cache.
     * Default: 50 images (good balance between memory usage and hit rate)
     */
    @Value("${image.cache.max-size:50}")
    private int maxSize;
    
    /**
     * Maximum memory usage in MB.
     * Default: 100MB (allows for ~50-100 processed images depending on size)
     */
    @Value("${image.cache.max-memory-mb:100}")
    private int maxMemoryMb;
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public long getMaxMemoryBytes() {
        return maxMemoryMb * 1024L * 1024L; // Convert MB to bytes
    }
    
    /**
     * Returns a summary of the configuration for logging.
     */
    public String getConfigSummary() {
        return String.format("Cache config: max %d items, max %dMB memory", 
                           maxSize, maxMemoryMb);
    }
}

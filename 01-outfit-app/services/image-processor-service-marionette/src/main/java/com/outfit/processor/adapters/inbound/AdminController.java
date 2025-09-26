package com.outfit.processor.adapters.inbound;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outfit.processor.usecases.outbound.imagecache.CacheStatistics;
import com.outfit.processor.usecases.outbound.imagecache.ProcessedImageCache;


/**
 * Administrative controller for cache management.
 * 
 * This controller provides endpoints for:
 * - Viewing cache statistics
 * - Clearing the cache
 * - Monitoring cache performance
 * 
 * Following REST principles and clean architecture separation.
 */
@RestController
@RequestMapping("/admin/cache")
public class AdminController {
    
    private final ProcessedImageCache cache;
    
    public AdminController(ProcessedImageCache cache) {
        this.cache = cache;
    }
    
    /**
     * GET /admin/cache/stats
     * Returns comprehensive cache statistics for monitoring.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        try {
            CacheStatistics stats = cache.getStatistics();
            
            Map<String, Object> response = Map.of(
                "currentSize", stats.getCurrentSize(),
                "maxSize", stats.getMaxSize(),
                "totalRequests", stats.getTotalRequests(),
                "cacheHits", stats.getCacheHits(),
                "cacheMisses", stats.getCacheMisses(),
                "hitRate", Math.round(stats.getHitRate() * 10000) / 100.0, // percentage with 2 decimals
                "missRate", Math.round(stats.getMissRate() * 10000) / 100.0,
                "totalSizeBytes", stats.getTotalSizeBytes(),
                "totalSizeMB", Math.round(stats.getTotalSizeBytes() / (1024.0 * 1024.0) * 100) / 100.0,
                "utilizationPercent", Math.round((double) stats.getCurrentSize() / stats.getMaxSize() * 10000) / 100.0
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get cache statistics: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /admin/cache
     * Clears all cached images.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCache() {
        try {
            CacheStatistics beforeStats = cache.getStatistics();
            cache.clearCache();
            
            return ResponseEntity.ok(Map.of(
                "message", "Cache cleared successfully",
                "itemsRemoved", String.valueOf(beforeStats.getCurrentSize()),
                "memoryFreed", formatBytes(beforeStats.getTotalSizeBytes())
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to clear cache: " + e.getMessage()));
        }
    }
    
    /**
     * GET /admin/cache/health
     * Returns cache health information for monitoring systems.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        try {
            CacheStatistics stats = cache.getStatistics();
            
            // Determine health status based on hit rate and utilization
            String status = "healthy";
            String message = "Cache is operating normally";
            
            if (stats.getHitRate() < 0.5 && stats.getTotalRequests() > 100) {
                status = "warning";
                message = "Low cache hit rate detected";
            }
            
            if (stats.getCurrentSize() >= stats.getMaxSize()) {
                status = "warning";
                message = "Cache is at maximum capacity";
            }
            
            Map<String, Object> health = Map.of(
                "status", status,
                "message", message,
                "hitRate", stats.getHitRate(),
                "utilization", (double) stats.getCurrentSize() / stats.getMaxSize(),
                "totalRequests", stats.getTotalRequests()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to check cache health: " + e.getMessage()
                ));
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
package com.outfit.processor.usecases.outbound.imagecache;

/**
 * Value object containing cache statistics.
 * Immutable data structure following clean architecture principles.
 */
public class CacheStatistics {

    private final int currentSize;
    private final int maxSize;
    private final long totalRequests;
    private final long cacheHits;
    private final long cacheMisses;
    private final long totalSizeBytes;

    public CacheStatistics(int currentSize, int maxSize, long totalRequests,
            long cacheHits, long cacheMisses, long totalSizeBytes) {
        this.currentSize = currentSize;
        this.maxSize = maxSize;
        this.totalRequests = totalRequests;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.totalSizeBytes = totalSizeBytes;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    /**
     * Calculates hit rate as a percentage.
     * 
     * @return hit rate between 0.0 and 1.0, or 0.0 if no requests
     */
    public double getHitRate() {
        return totalRequests == 0 ? 0.0 : (double) cacheHits / totalRequests;
    }

    /**
     * Calculates miss rate as a percentage.
     * 
     * @return miss rate between 0.0 and 1.0, or 0.0 if no requests
     */
    public double getMissRate() {
        return totalRequests == 0 ? 0.0 : (double) cacheMisses / totalRequests;
    }

    @Override
    public String toString() {
        return String.format(
                "CacheStatistics{size=%d/%d, requests=%d, hits=%d (%.1f%%), misses=%d (%.1f%%), totalSize=%d bytes}",
                currentSize, maxSize, totalRequests, cacheHits,
                getHitRate() * 100, cacheMisses, getMissRate() * 100, totalSizeBytes);
    }
}

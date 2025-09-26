package com.outfit.processor.usecases.outbound.imagecache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Thread-safe in-memory cache with random eviction strategy and priority clearing.
 * 
 * Design decisions:
 * 1. Random eviction: Simple, no metadata overhead, good performance
 * 2. Dual limits: Both count and memory limits for safety
 * 3. Thread safety: ReadWriteLock for optimal read performance
 * 4. Priority clearing: Clear operations get absolute priority over read operations
 * 5. Defensive copying: Prevents external modification of cached data
 * 6. Comprehensive metrics: For monitoring and tuning
 * 
 * Priority Mechanism:
 * - When clearCache() is called, it sets a clearRequested flag
 * - New read operations (getProcessedImage/getStatistics) wait until clear completes
 * - Clear operation waits for active readers to finish, then executes immediately
 * - This ensures clear operations are not starved by continuous read operations
 */
@Component
public class InMemoryImageCache implements ProcessedImageCache {

    private final InMemoryImageCacheConfig config;
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final AtomicLong totalRequests;
    private final AtomicLong cacheHits;
    private final AtomicLong cacheMisses;
    private final ReentrantReadWriteLock lock;
    private final Random random;
    
    // Priority mechanism for clear operations
    private final ReentrantLock priorityLock;
    private final Condition clearWaiting;
    private final AtomicInteger activeReaders;
    private volatile boolean clearRequested;

    public InMemoryImageCache(InMemoryImageCacheConfig config) {
        this.config = config;
        this.cache = new ConcurrentHashMap<>();
        this.totalRequests = new AtomicLong(0);
        this.cacheHits = new AtomicLong(0);
        this.cacheMisses = new AtomicLong(0);
        this.lock = new ReentrantReadWriteLock();
        this.random = new Random();
        
        // Initialize priority mechanism components
        this.priorityLock = new ReentrantLock();
        this.clearWaiting = priorityLock.newCondition();
        this.activeReaders = new AtomicInteger(0);
        this.clearRequested = false;

        System.out.println("Initialized image cache: " + config.getConfigSummary());
    }

    @Override
    public byte[] getProcessedImage(String imageName) {
        totalRequests.incrementAndGet();

        // Priority-aware read: wait if clear operation is pending
        priorityLock.lock();
        try {
            // Wait while a clear operation is requested and still pending
            while (clearRequested) {
                try {
                    clearWaiting.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            // Register this thread as an active reader
            activeReaders.incrementAndGet();
        } finally {
            priorityLock.unlock();
        }

        try {
            lock.readLock().lock();
            try {
                CacheEntry entry = cache.get(imageName);
                if (entry != null) {
                    cacheHits.incrementAndGet();
                    return entry.data.clone(); // Return defensive copy
                } else {
                    cacheMisses.incrementAndGet();
                    return null;
                }
            } finally {
                lock.readLock().unlock();
            }
        } finally {
            // Unregister this thread as an active reader
            int remainingReaders = activeReaders.decrementAndGet();
            
            // If this was the last reader and a clear is waiting, notify the clear operation
            if (remainingReaders == 0 && clearRequested) {
                priorityLock.lock();
                try {
                    clearWaiting.signalAll();
                } finally {
                    priorityLock.unlock();
                }
            }
        }
    }

    @Override
    public void putProcessedImage(String imageName, byte[] processedImageData) {
        if (processedImageData == null || processedImageData.length == 0) {
            return;
        }

        // Wait if clear operation is pending (give priority to clear operations)
        priorityLock.lock();
        try {
            while (clearRequested) {
                try {
                    clearWaiting.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            priorityLock.unlock();
        }

        lock.writeLock().lock();
        try {
            // Evict items if necessary before adding new one
            while (shouldEvict(processedImageData.length)) {
                evictRandomEntry();
            }

            // Add new entry
            CacheEntry newEntry = new CacheEntry(processedImageData.clone()); // Store defensive copy
            cache.put(imageName, newEntry);

            logCacheOperation("Cached", imageName, processedImageData.length);

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clearCache() {
        System.out.println("Clear cache operation requested - setting priority mode");
        
        // Signal that a clear operation is requested (prevents new readers)
        priorityLock.lock();
        try {
            clearRequested = true;
            
            // Wait for all active readers to finish
            while (activeReaders.get() > 0) {
                try {
                    System.out.println("Waiting for " + activeReaders.get() + " active readers to complete before clearing cache");
                    clearWaiting.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    clearRequested = false;
                    clearWaiting.signalAll(); // Wake up any waiting readers
                    return;
                }
            }
        } finally {
            priorityLock.unlock();
        }

        // Now perform the actual clear operation
        lock.writeLock().lock();
        try {
            int clearedItems = cache.size();
            long clearedMemory = getCurrentMemoryUsage();
            cache.clear();

            System.out.println(String.format("Cache cleared: removed %d items, freed %s",
                    clearedItems, formatBytes(clearedMemory)));
        } finally {
            lock.writeLock().unlock();
            
            // Reset the clear request flag and notify waiting readers
            priorityLock.lock();
            try {
                clearRequested = false;
                clearWaiting.signalAll(); // Allow new readers to proceed
            } finally {
                priorityLock.unlock();
            }
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        // Priority-aware read: wait if clear operation is pending
        priorityLock.lock();
        try {
            // Wait while a clear operation is requested and still pending
            while (clearRequested) {
                try {
                    clearWaiting.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Return default statistics if interrupted
                    return new CacheStatistics(0, config.getMaxSize(), 
                            totalRequests.get(), cacheHits.get(), cacheMisses.get(), 0);
                }
            }
            // Register this thread as an active reader
            activeReaders.incrementAndGet();
        } finally {
            priorityLock.unlock();
        }

        try {
            lock.readLock().lock();
            try {
                long totalSizeBytes = getCurrentMemoryUsage();

                return new CacheStatistics(
                        cache.size(),
                        config.getMaxSize(),
                        totalRequests.get(),
                        cacheHits.get(),
                        cacheMisses.get(),
                        totalSizeBytes);
            } finally {
                lock.readLock().unlock();
            }
        } finally {
            // Unregister this thread as an active reader
            int remainingReaders = activeReaders.decrementAndGet();
            
            // If this was the last reader and a clear is waiting, notify the clear operation
            if (remainingReaders == 0 && clearRequested) {
                priorityLock.lock();
                try {
                    clearWaiting.signalAll();
                } finally {
                    priorityLock.unlock();
                }
            }
        }
    }

    /**
     * Determines if we need to evict items before adding a new one.
     * 
     * Evicts when either:
     * 1. We've reached the maximum number of items, OR
     * 2. Adding the new item would exceed memory limit
     */
    private boolean shouldEvict(int newItemSize) {
        // Check size limit
        if (cache.size() >= config.getMaxSize()) {
            return true;
        }

        // Check memory limit
        long currentMemory = getCurrentMemoryUsage();
        return (currentMemory + newItemSize) > config.getMaxMemoryBytes();
    }

    /**
     * Evicts a randomly selected cache entry.
     * 
     * Random eviction benefits:
     * - Simple implementation
     * - No metadata overhead
     * - Good performance
     * - Reasonable cache behavior for most workloads
     */
    private void evictRandomEntry() {
        if (cache.isEmpty()) {
            return;
        }

        // Get a random key from the cache
        List<String> keys = new ArrayList<>(cache.keySet());
        String keyToEvict = keys.get(random.nextInt(keys.size()));

        CacheEntry removed = cache.remove(keyToEvict);
        if (removed != null) {
            logCacheOperation("Evicted", keyToEvict, removed.data.length);
        }
    }

    /**
     * Calculates current memory usage of all cached items.
     */
    private long getCurrentMemoryUsage() {
        return cache.values().stream()
                .mapToLong(entry -> entry.data.length)
                .sum();
    }

    /**
     * Logs cache operations for monitoring and debugging.
     */
    private void logCacheOperation(String operation, String imageName, int size) {
        System.out.println(String.format("%s image: %s (size: %s, cache: %d/%d items, memory: %s/%dMB)",
                operation,
                imageName,
                formatBytes(size),
                cache.size(),
                config.getMaxSize(),
                formatBytes(getCurrentMemoryUsage()),
                config.getMaxMemoryBytes() / (1024 * 1024)));
    }

    /**
     * Formats byte sizes for human-readable logging.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Simple cache entry containing just the image data.
     * No metadata needed for random eviction strategy.
     */
    private static class CacheEntry {
        final byte[] data;

        CacheEntry(byte[] data) {
            this.data = data;
        }
    }
}

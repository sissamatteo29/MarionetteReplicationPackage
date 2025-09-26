package com.outfit.processor.usecases.inbound.processimage.marionette;

import com.outfit.processor.usecases.inbound.processimage.ImageEnhanceProcessor;
import com.outfit.processor.usecases.outbound.fetchimages.FetchImageGateway;
import com.outfit.processor.usecases.outbound.imagecache.ProcessedImageCache;

public class ProcessImageUseCaseImpl_WithCache {

    private final FetchImageGateway fetchImageGateway;
    private final ImageEnhanceProcessor imageProcessor;
    private final ProcessedImageCache cache;

    public ProcessImageUseCaseImpl_WithCache(FetchImageGateway fetchImageGateway, 
                                  ImageEnhanceProcessor imageProcessor,
                                  ProcessedImageCache cache) {
        this.fetchImageGateway = fetchImageGateway;
        this.imageProcessor = imageProcessor;
        this.cache = cache;
    }

    public byte[] execute(String imageName) {
        // First, try to get from cache
        byte[] cachedResult = cache.getProcessedImage(imageName);
        if (cachedResult != null) {
            System.out.println("Cache HIT for image: " + imageName);
            return cachedResult;
        }

        System.out.println("Cache MISS for image: " + imageName + " - processing...");

        // Cache miss - fetch, process, and cache
        byte[] originalImage = fetchImageGateway.fetchImage(imageName);
        if (originalImage == null) {
            return null; // Image not found
        }

        byte[] processedImage = imageProcessor.enhanceImage(originalImage);

        // Store in cache for future requests
        cache.putProcessedImage(imageName, processedImage);

        return processedImage;
    }

}

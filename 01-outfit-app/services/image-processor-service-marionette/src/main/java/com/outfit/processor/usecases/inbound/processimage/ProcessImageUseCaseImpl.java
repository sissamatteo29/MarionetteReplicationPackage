package com.outfit.processor.usecases.inbound.processimage;

import com.outfit.processor.usecases.outbound.fetchimages.FetchImageGateway;
import com.outfit.processor.usecases.inbound.ProcessImageUseCase;
import com.outfit.processor.usecases.outbound.imagecache.ProcessedImageCache;
import com.outfit.processor.usecases.inbound.processimage.ImageEnhanceProcessor;
import org.springframework.stereotype.Component;

@Component
public class ProcessImageUseCaseImpl implements ProcessImageUseCase {

    private final FetchImageGateway fetchImageGateway;

    private final ImageEnhanceProcessor imageProcessor;

    private final ProcessedImageCache cache;

    public ProcessImageUseCaseImpl(FetchImageGateway fetchImageGateway, ImageEnhanceProcessor imageProcessor, ProcessedImageCache cache) {
        this.fetchImageGateway = fetchImageGateway;
        this.imageProcessor = imageProcessor;
        this.cache = cache;
    }

    @Override
    public byte[] execute(String imageName) {
        switch(com.outfit.processor.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/processor/usecases/inbound/processimage/ProcessImageUseCaseImpl.java", "execute")) {
            default:
            case "without_cache":
                return execute_without_cache(imageName);
            case "with_cache":
                return execute_with_cache(imageName);
        }
    }

    public byte[] execute_without_cache(String imageName) {
        byte[] imageFromStore = fetchImageGateway.fetchImage(imageName);
        return imageProcessor.enhanceImage(imageFromStore);
    }

    public byte[] execute_with_cache(String imageName) {
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
            // Image not found
            return null;
        }
        byte[] processedImage = imageProcessor.enhanceImage(originalImage);
        // Store in cache for future requests
        cache.putProcessedImage(imageName, processedImage);
        return processedImage;
    }
}

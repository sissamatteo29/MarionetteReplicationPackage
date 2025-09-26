package com.outfit.processor.usecases.inbound.processimage;

import org.springframework.stereotype.Component;

import com.outfit.processor.usecases.inbound.ProcessImageUseCase;
import com.outfit.processor.usecases.outbound.fetchimages.FetchImageGateway;
import com.outfit.processor.usecases.outbound.imagecache.ProcessedImageCache;

@Component
public class ProcessImageUseCaseImpl implements ProcessImageUseCase {

    private final FetchImageGateway fetchImageGateway;
    private final ImageEnhanceProcessor imageProcessor;
    private final ProcessedImageCache cache;

    public ProcessImageUseCaseImpl(FetchImageGateway fetchImageGateway, 
                                  ImageEnhanceProcessor imageProcessor,
                                  ProcessedImageCache cache) {
        this.fetchImageGateway = fetchImageGateway;
        this.imageProcessor = imageProcessor;
        this.cache = cache;
    }

    @Override
    public byte[] execute(String imageName) {
        byte[] imageFromStore = fetchImageGateway.fetchImage(imageName);
        return imageProcessor.enhanceImage(imageFromStore);
    }

}

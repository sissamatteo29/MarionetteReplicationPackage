package com.outfit.imagestore.usecases.inbound.storeimage;

import org.springframework.stereotype.Component;

import com.outfit.imagestore.usecases.inbound.StoreImageUseCase;
import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

@Component
public class StoreImageUseCaseImpl implements StoreImageUseCase {

    private final ImageRepository imageRepository;

    public StoreImageUseCaseImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public void execute(byte[] imageData, String originalExtension) {
        imageRepository.putImage(imageData, originalExtension);
    }
    
}

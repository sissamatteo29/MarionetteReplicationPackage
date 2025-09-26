package com.outfit.imagestore.usecases.inbound.download;

import org.springframework.stereotype.Component;

import com.outfit.imagestore.usecases.inbound.DownloadImageUseCase;
import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

@Component
public class DownloadImageUseCaseImpl implements DownloadImageUseCase {

    private final ImageRepository imageRepository;

    public DownloadImageUseCaseImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public byte[] execute(String imageName) {
        return imageRepository.getImage(imageName);
    }
    
}

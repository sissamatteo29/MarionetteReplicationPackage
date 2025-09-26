package com.outfit.imagestore.usecases.inbound.imagenames;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.outfit.imagestore.usecases.inbound.FetchImageNamesUseCase;
import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

@Component
public class FetchImageNamesUseCaseImpl implements FetchImageNamesUseCase {

    private final ImageRepository imageRepository;

    public FetchImageNamesUseCaseImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public List<String> execute() throws IOException {
        return imageRepository.getAllImages();
    }
    
}

package com.outfit.ui.usecases.inbound.processimage;

import org.springframework.stereotype.Component;

import com.outfit.ui.usecases.inbound.FetchProcessedImageUseCase;
import com.outfit.ui.usecases.outbound.processimage.ProcessImageGateway;

@Component
public class FetchProcessedImageUseCaseImpl implements FetchProcessedImageUseCase {

    private final ProcessImageGateway processImageGateway;

    public FetchProcessedImageUseCaseImpl(ProcessImageGateway processImageGateway) {
        this.processImageGateway = processImageGateway;
    }

    @Override
    public byte[] execute(String imageName) {
        return processImageGateway.processImage(imageName);
    }
    
}

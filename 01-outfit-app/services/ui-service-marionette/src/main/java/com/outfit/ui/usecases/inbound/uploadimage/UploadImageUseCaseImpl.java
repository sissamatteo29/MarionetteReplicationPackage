package com.outfit.ui.usecases.inbound.uploadimage;

import org.springframework.stereotype.Component;

import com.outfit.ui.usecases.inbound.UploadImageUseCase;
import com.outfit.ui.usecases.outbound.uploadimage.UploadImageGateway;

@Component
public class UploadImageUseCaseImpl implements UploadImageUseCase {

    private final UploadImageGateway uploadImageGateway;

    public UploadImageUseCaseImpl(UploadImageGateway uploadImageGateway) {
        this.uploadImageGateway = uploadImageGateway;
    }

    @Override
    public void execute(byte[] imageData, String imageName) {
        uploadImageGateway.uploadImage(imageData, imageName);
    }
    
}

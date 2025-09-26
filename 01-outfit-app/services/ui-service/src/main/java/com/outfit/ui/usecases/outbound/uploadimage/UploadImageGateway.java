package com.outfit.ui.usecases.outbound.uploadimage;

public interface UploadImageGateway {

    public void uploadImage(byte[] imageData, String imageName);
    
}

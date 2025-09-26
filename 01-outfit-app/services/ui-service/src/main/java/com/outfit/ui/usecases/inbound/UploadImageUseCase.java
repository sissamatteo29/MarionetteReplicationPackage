package com.outfit.ui.usecases.inbound;

public interface UploadImageUseCase {

    public void execute(byte[] imageData, String imageName);
    
}

package com.outfit.imagestore.usecases.inbound;

public interface StoreImageUseCase {

    public void execute(byte[] imageData, String originalExtension);
    
}

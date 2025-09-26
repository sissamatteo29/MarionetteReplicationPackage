package com.outfit.imagestore.adapters.outbound.imagerepo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageRepositoryConfig {

    @Value("${images.storage.path:./images}")
    private String imageStoragePath;

    public String getImageStoragePath() {
        return imageStoragePath;
    }
    
}

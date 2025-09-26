package com.outfit.ui.adapters.outbound.uploadimage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageStoreServiceUploadImageAdapterConfig {

    @Value("${imagestore.service.url:http://localhost:8082}")
    private String uploadImageBaseUrl;

    public String getUploadImageBaseUrl() {
        return uploadImageBaseUrl;
    }
    
}

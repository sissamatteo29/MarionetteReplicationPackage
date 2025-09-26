package com.outfit.processor.adapters.outbound.fetchimages;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageStoreServiceFetchImageAdapterConfig {

    @Value("${imagestore.service.url:http://localhost:8082}")
    private String url;

    public String getUrl() {
        return url;
    }
    
}

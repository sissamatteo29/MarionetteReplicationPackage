package com.outfit.ui.adapters.outbound.processimage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageProcessorServiceProcessImageAdapterConfig {

    private final String imageProcessorServiceUrl;

    public ImageProcessorServiceProcessImageAdapterConfig(
            @Value("${image.processor.service.url:http://localhost:8081}") String imageProcessorServiceUrl) {
        this.imageProcessorServiceUrl = imageProcessorServiceUrl;
    }

    public String getImageProcessorServiceUrl() {
        return imageProcessorServiceUrl;
    }

    // Optional: Additional configuration methods
    public String getProcessEndpoint() {
        return imageProcessorServiceUrl + "/process";
    }

    public String getHealthEndpoint() {
        return imageProcessorServiceUrl + "/actuator/health";
    }
}
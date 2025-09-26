package com.outfit.processor.adapters.inbound;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.outfit.processor.usecases.inbound.ProcessImageUseCase;

@RestController
public class ImageProcessingController {

    private final ProcessImageUseCase processImageUseCase;

    public ImageProcessingController(ProcessImageUseCase processImageUseCase) {
        this.processImageUseCase = processImageUseCase;
    }

    @GetMapping("/process/{imageName}")
    public ResponseEntity<byte[]> processImage(@PathVariable String imageName) {
        try {
            byte[] processedImageData = processImageUseCase.execute(imageName);
            
            if (processedImageData == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)  // Tell browser this is an image
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"processed_" + imageName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")  // Disable caching for debugging
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(processedImageData);
                    
        } catch (Exception e) {
            System.err.println("Error processing image " + imageName + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

package com.outfit.imagestore.adapters.outbound.imagerepo.processing;

/**
 * Simple service for processing images before storage.
 */
public interface ImageProcessor {
    
    /**
     * Processes an image and returns the processed data.
     * Always returns JPEG format.
     */
    byte[] processImage(byte[] imageData) throws ImageProcessingException;
    
    /**
     * Checks if the data is a valid image.
     */
    boolean isValidImage(byte[] imageData);
}
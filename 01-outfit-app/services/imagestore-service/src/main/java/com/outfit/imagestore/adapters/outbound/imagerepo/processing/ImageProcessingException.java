package com.outfit.imagestore.adapters.outbound.imagerepo.processing;
/**
 * Exception for image processing errors.
 */
public class ImageProcessingException extends Exception {
    
    public ImageProcessingException(String message) {
        super(message);
    }
    
    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
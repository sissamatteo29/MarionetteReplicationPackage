package com.outfit.processor.usecases.inbound.processimage;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

@Component
public class ImageEnhanceProcessor {

    /**
     * Main method to enhance an image for UI display
     * Applies multiple enhancement techniques to make the image more attractive
     */
    public byte[] enhanceImage(byte[] originalImageData) {
        try {
            // Load the original image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImageData));
            if (image == null) {
                throw new IllegalArgumentException("Invalid image format");
            }

            System.out.println("Starting image enhancement for " + image.getWidth() + "x" + image.getHeight() + " image");

            // Apply enhancement pipeline
            BufferedImage enhanced = image;
            
            // 1. Brightness and contrast adjustment
            enhanced = adjustBrightnessContrast(enhanced, 1.05, 1.1);
            
            // 2. Color saturation boost
            enhanced = enhanceSaturation(enhanced, 1.15);
            
            // 3. Sharpen the image
            enhanced = sharpenImage(enhanced);
            
            // 4. Subtle warm tone (makes images more appealing)
            enhanced = addWarmTone(enhanced, 0.05);

            System.out.println("Image enhancement completed successfully");

            // Convert back to byte array
            return imageToByteArray(enhanced);

        } catch (IOException e) {
            throw new RuntimeException("Failed to process image", e);
        }
    }

    /**
     * Adjusts brightness and contrast to make image more vibrant
     * brightness: 1.0 = normal, >1.0 = brighter, <1.0 = darker
     * contrast: 1.0 = normal, >1.0 = more contrast, <1.0 = less contrast
     */
    private BufferedImage adjustBrightnessContrast(BufferedImage image, double brightness, double contrast) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                
                // Extract RGB components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Apply brightness
                r = (int) (r * brightness);
                g = (int) (g * brightness);
                b = (int) (b * brightness);
                
                // Apply contrast (around midpoint 128)
                r = (int) ((r - 128) * contrast + 128);
                g = (int) ((g - 128) * contrast + 128);
                b = (int) ((b - 128) * contrast + 128);
                
                // Clamp values to 0-255 range
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return result;
    }

    /**
     * Enhances color saturation to make colors more vivid
     * factor: 1.0 = normal, >1.0 = more saturated, <1.0 = less saturated
     */
    private BufferedImage enhanceSaturation(BufferedImage image, double factor) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Calculate luminance (grayscale value)
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                
                // Enhance saturation by moving colors away from luminance
                r = (int) (luminance + (r - luminance) * factor);
                g = (int) (luminance + (g - luminance) * factor);
                b = (int) (luminance + (b - luminance) * factor);
                
                // Clamp values
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return result;
    }

    /**
     * Applies unsharp mask to sharpen the image and make details pop
     */
    private BufferedImage sharpenImage(BufferedImage image) {
        // Unsharp mask kernel - enhances edges
        float[] sharpenKernel = {
            0.0f, -0.25f, 0.0f,
            -0.25f, 2.0f, -0.25f,
            0.0f, -0.25f, 0.0f
        };
        
        Kernel kernel = new Kernel(3, 3, sharpenKernel);
        ConvolveOp sharpenOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        sharpenOp.filter(image, result);
        
        return result;
    }

    /**
     * Adds a subtle warm tone to make the image more appealing
     * intensity: 0.0 = no effect, 0.1 = subtle warmth, 0.2+ = noticeable warmth
     */
    private BufferedImage addWarmTone(BufferedImage image, double intensity) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Add warm tone by slightly boosting red and reducing blue
                r = (int) (r + (255 - r) * intensity * 0.3);
                g = (int) (g + (255 - g) * intensity * 0.1);
                b = (int) (b - b * intensity * 0.1);
                
                // Clamp values
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return result;
    }

    /**
     * Converts BufferedImage back to byte array
     */
    private byte[] imageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", baos);
        return baos.toByteArray();
    }
}
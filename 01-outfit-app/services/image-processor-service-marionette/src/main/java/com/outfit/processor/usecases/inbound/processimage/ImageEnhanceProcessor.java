package com.outfit.processor.usecases.inbound.processimage;

import java.nio.file.Files;
import ij.process.ColorProcessor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import org.springframework.stereotype.Component;
import java.io.File;
import java.awt.image.ConvolveOp;
import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.awt.image.Kernel;
import javax.imageio.ImageIO;

@Component
public class ImageEnhanceProcessor {

    /**
     * Main method to enhance an image for UI display
     * Applies multiple enhancement techniques to make the image more attractive
     */
    public byte[] enhanceImage(byte[] originalImageData) {
        switch(com.outfit.processor.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/processor/usecases/inbound/processimage/ImageEnhanceProcessor.java", "enhanceImage")) {
            case "imageJLib":
                return enhanceImage_imageJLib(originalImageData);
            default:
            case "manual":
                return enhanceImage_manual(originalImageData);
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
        float[] sharpenKernel = { 0.0f, -0.25f, 0.0f, -0.25f, 2.0f, -0.25f, 0.0f, -0.25f, 0.0f };
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

    public byte[] enhanceImage_imageJLib(byte[] originalImageData) {
        try {
            // Set ImageJ to headless mode to work without GUI
            System.setProperty("java.awt.headless", "true");
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(originalImageData));
            if (bufferedImage == null) {
                throw new IllegalArgumentException("Invalid image format");
            }
            ImagePlus imagePlus = new ImagePlus("processing", bufferedImage);
            ImageProcessor processor = imagePlus.getProcessor();
            System.out.println("Using ImageJ built-in methods where available...");
            // Apply contrast enhancement using built-in ImageJ methods
            // Increase brightness slightly
            processor.multiply(1.1);
            if (processor instanceof ColorProcessor) {
                ColorProcessor colorProc = (ColorProcessor) processor;
                // Apply gamma correction for contrast
                colorProc.gamma(0.9);
            }
            // Get the updated processor after contrast enhancement
            processor = imagePlus.getProcessor();
            // Saturation
            double saturationFactor = 1.15;
            if (!(processor instanceof ColorProcessor)) {
                processor = processor.convertToColorProcessor();
            }
            ColorProcessor cp = (ColorProcessor) processor;
            // MANUAL IMPLEMENTATION REQUIRED - no built-in saturation method
            int width = cp.getWidth();
            int height = cp.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = cp.getPixel(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Your saturation algorithm (no ImageJ equivalent)
                    double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                    r = (int) (luminance + (r - luminance) * saturationFactor);
                    g = (int) (luminance + (g - luminance) * saturationFactor);
                    b = (int) (luminance + (b - luminance) * saturationFactor);
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));
                    cp.putPixel(x, y, (r << 16) | (g << 8) | b);
                }
            }
            // Make sure the ImagePlus has the updated processor
            imagePlus.setProcessor(cp);
            // Apply unsharp mask directly without show/hide
            processor = imagePlus.getProcessor();
            if (processor instanceof ColorProcessor) {
                ColorProcessor colorProc = (ColorProcessor) processor;
                colorProc.sharpen();
            } else {
                processor.sharpen();
            }
            // Get the processed result
            processor = imagePlus.getProcessor();
            cp = (ColorProcessor) processor;
            double warmthIntensity = 0.05;
            // MANUAL IMPLEMENTATION REQUIRED - no built-in warm tone method
            for (int y = 0; y < cp.getHeight(); y++) {
                for (int x = 0; x < cp.getWidth(); x++) {
                    int rgb = cp.getPixel(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Your warm tone algorithm (no ImageJ equivalent)
                    r = (int) (r + (255 - r) * warmthIntensity * 0.3);
                    g = (int) (g + (255 - g) * warmthIntensity * 0.1);
                    b = (int) (b - b * warmthIntensity * 0.1);
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));
                    cp.putPixel(x, y, (r << 16) | (g << 8) | b);
                }
            }
            System.out.println("ImageJ enhancement completed");
            // Get the final processed result
            processor = imagePlus.getProcessor();
            if (!(processor instanceof ColorProcessor)) {
                processor = processor.convertToColorProcessor();
            }
            cp = (ColorProcessor) processor;
            // FIX 2: Use cp.getBufferedImage() instead of processor.getBufferedImage()
            BufferedImage resultImage = cp.getBufferedImage();
            if (resultImage == null) {
                throw new RuntimeException("Failed to get BufferedImage from processed ColorProcessor");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = ImageIO.write(resultImage, "JPEG", baos);
            if (!success) {
                throw new RuntimeException("Failed to write processed image to JPEG format");
            }
            byte[] result = baos.toByteArray();
            System.out.println("ImageJ enhancement completed successfully, output size: " + result.length + " bytes");
            return result;
        } catch (IOException e) {
            System.err.println("IOException in ImageJ enhancement: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process image with ImageJ", e);
        } catch (Exception e) {
            System.err.println("Unexpected error in ImageJ enhancement: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process image with ImageJ", e);
        }
    }

    /**
     * Main method to enhance an image for UI display
     * Applies multiple enhancement techniques to make the image more attractive
     */
    public byte[] enhanceImage_manual(byte[] originalImageData) {
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
}

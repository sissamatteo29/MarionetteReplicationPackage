package com.outfit.processor.usecases.inbound.processimage.marionette;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class ImageEnhanceProcessor_ImageJLibrary {

    public byte[] enhanceImage(byte[] originalImageData) {
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
            processor.multiply(1.1); // Increase brightness slightly
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

    public static void main(String[] args) {
        try {
            System.out.println("=== ImageJ Image Enhancement Test ===");

            // Initialize the processor
            ImageEnhanceProcessor_ImageJLibrary processor = new ImageEnhanceProcessor_ImageJLibrary();

            // Define file paths (change these to match your system)
            String inputImagePath = "user-simulator/test-images/file-1.jpg"; // Put your image here
            String outputImagePath = "test_output_enhanced.jpg";

            // Check if input file exists
            File inputFile = new File(inputImagePath);
            if (!inputFile.exists()) {
                System.out.println("❌ Input image not found: " + inputImagePath);
                System.out.println("Please place a JPEG image named 'test_input.jpg' in the project root directory");
                System.out.println("Or update the inputImagePath variable to point to your image");
                return;
            }

            System.out.println("📂 Reading image from: " + inputImagePath);

            // Read the input image as byte array
            byte[] originalImageData = Files.readAllBytes(Paths.get(inputImagePath));

            System.out.println("📊 Original image size: " + (originalImageData.length / 1024) + " KB");

            // Measure processing time
            long startTime = System.currentTimeMillis();

            // Process the image
            byte[] enhancedImageData = processor.enhanceImage(originalImageData);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // Save the enhanced image
            Files.write(Paths.get(outputImagePath), enhancedImageData);

            System.out.println("✅ Enhancement completed successfully!");
            System.out.println("⏱️  Processing time: " + processingTime + " ms");
            System.out.println("📊 Enhanced image size: " + (enhancedImageData.length / 1024) + " KB");
            System.out.println("💾 Enhanced image saved as: " + outputImagePath);
            System.out.println();
            System.out.println("🔍 Compare the original and enhanced images to see the improvements:");
            System.out.println("   - Improved contrast and brightness");
            System.out.println("   - Enhanced color saturation");
            System.out.println("   - Professional sharpening applied");
            System.out.println("   - Subtle warm tone added");

        } catch (Exception e) {
            System.err.println("❌ Error during image processing: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

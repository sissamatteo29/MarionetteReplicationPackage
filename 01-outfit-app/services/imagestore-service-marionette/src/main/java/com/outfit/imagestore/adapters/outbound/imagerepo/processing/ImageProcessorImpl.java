package com.outfit.imagestore.adapters.outbound.imagerepo.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;

/**
 * Simple image processor that resizes and compresses images to JPEG.
 */
@Component
public class ImageProcessorImpl implements ImageProcessor {

    private static final int MAX_SIZE = 800;
    private static final float JPEG_QUALITY = 0.8f;

    @Override
    public byte[] processImage(byte[] imageData) throws ImageProcessingException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                throw new ImageProcessingException("Invalid image format");
            }

            // Resize if needed
            if (image.getWidth() > MAX_SIZE || image.getHeight() > MAX_SIZE) {
                image = resizeImage(image);
            }

            // Convert to RGB (for JPEG compatibility)
            image = ensureRGB(image);

            // Compress to JPEG
            return compressToJpeg(image);

        } catch (IOException e) {
            throw new ImageProcessingException("Failed to process image", e);
        }
    }

    @Override
    public boolean isValidImage(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            return image != null;
        } catch (Exception e) {
            return false;
        }
    }

    private BufferedImage resizeImage(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculate new size maintaining aspect ratio
        double scale = Math.min((double) MAX_SIZE / width, (double) MAX_SIZE / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private BufferedImage ensureRGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgbImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return rgbImage;
    }

    private byte[] compressToJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
        }
        writer.dispose();

        return baos.toByteArray();
    }
}
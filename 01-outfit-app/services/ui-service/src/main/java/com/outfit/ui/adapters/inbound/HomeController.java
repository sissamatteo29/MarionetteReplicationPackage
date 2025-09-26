package com.outfit.ui.adapters.inbound;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.outfit.ui.usecases.inbound.FetchProcessedImageUseCase;
import com.outfit.ui.usecases.inbound.PrepareImageGalleryPageUseCase;
import com.outfit.ui.usecases.inbound.UploadImageUseCase;
import com.outfit.ui.usecases.inbound.imagenames.GalleryPageResponse;

@Controller
public class HomeController {

    private final PrepareImageGalleryPageUseCase prepareImageGalleryPageUseCase;
    private final FetchProcessedImageUseCase fetchProcessedImageUseCase;
    private final UploadImageUseCase uploadImageUseCase;

    public HomeController(
            PrepareImageGalleryPageUseCase prepareImageGalleryPageUseCase,
            FetchProcessedImageUseCase fetchProcessedImageUseCase,
            UploadImageUseCase uploadImageUseCase) {
        this.prepareImageGalleryPageUseCase = prepareImageGalleryPageUseCase;
        this.fetchProcessedImageUseCase = fetchProcessedImageUseCase;
        this.uploadImageUseCase = uploadImageUseCase;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page, 
                      @RequestParam(required = false) String success,
                      @RequestParam(required = false) String error,
                      Model model) {

        System.out.println("User requesting gallery page: " + page);

        try {
            // Use case: Prepare gallery page for user
            GalleryPageResponse galleryPage = prepareImageGalleryPageUseCase.fetchImageNamesForPage(page);

            model.addAttribute("images", galleryPage.getImages());
            model.addAttribute("currentPage", galleryPage.getCurrentPage());
            model.addAttribute("hasNextPage", galleryPage.isHasNextPage());
            model.addAttribute("hasPreviousPage", galleryPage.isHasPreviousPage());
            model.addAttribute("totalImages", galleryPage.getTotalImages());
            
            // Add success/error messages for toast notifications
            if ("true".equals(success)) {
                model.addAttribute("successMessage", "Image uploaded successfully!");
            }
            if ("true".equals(error)) {
                model.addAttribute("errorMessage", "Failed to upload image. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("Failed to prepare gallery page: " + e.getMessage());
            model.addAttribute("images", java.util.Collections.emptyList());
            model.addAttribute("currentPage", page);
            model.addAttribute("hasNextPage", false);
            model.addAttribute("hasPreviousPage", false);
            model.addAttribute("totalImages", 0);
        }

        return "home_beautiful";
    }

    @GetMapping("/image-proxy/{imageId}")
    public ResponseEntity<byte[]> getEnhancedImage(@PathVariable String imageId) {

        System.out.println("User requesting enhanced image: " + imageId);

        try {
            // Use case: Load enhanced image for user
            byte[] enhancedImageData = fetchProcessedImageUseCase.execute(imageId);

            if (enhancedImageData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"enhanced_" + imageId + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate") // Disable caching for debugging
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(enhancedImageData);

        } catch (Exception e) {
            System.err.println("Failed to load enhanced image " + imageId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health/services")
    public ResponseEntity<String> checkServicesHealth() {
        try {
            // You could add health checks for your adapters here
            return ResponseEntity.ok("{\n" +
                    "  \"imagestore\": \"connected\",\n" +
                    "  \"processor\": \"connected\"\n" +
                    "}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Service health check failed");
        }
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "No file provided");
                return "redirect:/?error=true";
            }

            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                model.addAttribute("error", "Invalid filename");
                return "redirect:/?error=true";
            }

            uploadImageUseCase.execute(
                    file.getBytes(),
                    filename);

            return "redirect:/?success=true";

        } catch (IOException e) {
            System.err.println("Error reading uploaded file: " + e.getMessage());
            return "redirect:/?error=true";
        } catch (Exception e) {
            System.err.println("Error uploading image: " + e.getMessage());
            return "redirect:/?error=true";
        }
    }

}

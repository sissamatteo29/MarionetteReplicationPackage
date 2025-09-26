package com.outfit.imagestore.adapters.inbound;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.outfit.imagestore.usecases.inbound.DownloadImageUseCase;
import com.outfit.imagestore.usecases.inbound.FetchImageNamesUseCase;
import com.outfit.imagestore.usecases.inbound.StoreImageUseCase;

@RestController
public class ImageStoreController {

    private final DownloadImageUseCase downloadImageUseCase;
    private final FetchImageNamesUseCase fetchImageNamesUseCase;
    private final StoreImageUseCase storeImageUseCase;

    public ImageStoreController(DownloadImageUseCase downloadImageUseCase,
            FetchImageNamesUseCase fetchImageNamesUseCase, StoreImageUseCase storeImageUseCase) {
        this.downloadImageUseCase = downloadImageUseCase;
        this.fetchImageNamesUseCase = fetchImageNamesUseCase;
        this.storeImageUseCase = storeImageUseCase;
    }


    @GetMapping("/images")
    public ResponseEntity<List<String>> getImageNames() {
        try {
            List<String> imageNames = fetchImageNamesUseCase.execute();
            return ResponseEntity.ok(imageNames);
        } catch (Exception e) {
            System.err.println("Error fetching image names: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<byte[]> getImageBytes(@PathVariable String imageName) {
        try {
            byte[] imageData = downloadImageUseCase.execute(imageName);
            
            if (imageData == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate") // Disable caching for debugging
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .contentType(MediaType.IMAGE_JPEG) 
                    .body(imageData);
                    
        } catch (Exception e) {
            System.err.println("Error downloading image " + imageName + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> postImageFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file provided");
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            
            storeImageUseCase.execute(file.getBytes(), extension);
            return ResponseEntity.ok("Image uploaded successfully");
            
        } catch (Exception e) {
            System.err.println("Error storing image file: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to upload image");
        }
    }

    // Helper method to extract file extension
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "jpg";  // default
        }
        
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        
        return "jpg";  // default
    }
    
}

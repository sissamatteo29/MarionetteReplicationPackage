package com.outfit.imagestore.adapters.inbound;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

@RestController
@RequestMapping("/admin")
public class AdminController {

    public AdminController(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    private final ImageRepository imageRepository;

    @PostMapping("/clear-repository")
    public ResponseEntity<String> clearCache() {
        imageRepository.clearRepository();
        return ResponseEntity.ok("success");
    }

    @GetMapping("/repository-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(imageRepository.getRepositoryStats());
    }

}

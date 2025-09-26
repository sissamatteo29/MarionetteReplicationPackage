package com.outfit.ui.adapters.inbound.marionette;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.outfit.ui.usecases.inbound.PrepareImageGalleryPageUseCase;
import com.outfit.ui.usecases.inbound.imagenames.GalleryPageResponse;

public class HomeController_SimplerUI {

    private final PrepareImageGalleryPageUseCase prepareImageGalleryPageUseCase;

    public HomeController_SimplerUI(PrepareImageGalleryPageUseCase prepareImageGalleryPageUseCase) {
        this.prepareImageGalleryPageUseCase = prepareImageGalleryPageUseCase;
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

        return "home";
    }

}

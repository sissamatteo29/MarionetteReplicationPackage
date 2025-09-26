package com.outfit.ui.usecases.inbound;

import com.outfit.ui.usecases.inbound.imagenames.GalleryPageResponse;

public interface PrepareImageGalleryPageUseCase {
    /**
     * Prepares a page of the image gallery for display to the user
     * @param pageNumber The page number (0-based)
     * @return GalleryPageResponse containing images and pagination info
     */
    GalleryPageResponse fetchImageNamesForPage(int pageNumber);
}

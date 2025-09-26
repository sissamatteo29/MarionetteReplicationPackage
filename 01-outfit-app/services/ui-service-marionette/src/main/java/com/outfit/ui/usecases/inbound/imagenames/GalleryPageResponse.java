package com.outfit.ui.usecases.inbound.imagenames;

import java.util.List;

// Response object that encapsulates all gallery page data
public class GalleryPageResponse {
    private final List<GalleryImageItem> images;
    private final int currentPage;
    private final boolean hasNextPage;
    private final boolean hasPreviousPage;
    private final int totalImages;

    public GalleryPageResponse(List<GalleryImageItem> images, int currentPage, 
                              boolean hasNextPage, boolean hasPreviousPage, int totalImages) {
        this.images = images;
        this.currentPage = currentPage;
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
        this.totalImages = totalImages;
    }

    // Convenience constructor for backward compatibility
    public GalleryPageResponse(List<GalleryImageItem> images, int currentPage, 
                              boolean hasNextPage, int totalImages) {
        this(images, currentPage, hasNextPage, currentPage > 0, totalImages);
    }

    public List<GalleryImageItem> getImages() {
        return images;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalImages / images.size());
    }
}

package com.outfit.ui.usecases.inbound.imagenames;

public class GalleryImageItem {
    private final String imageName;
    private final String imageUrl;

    public GalleryImageItem(String imageName, String imageUrl) {
        this.imageName = imageName;
        this.imageUrl = imageUrl;
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}

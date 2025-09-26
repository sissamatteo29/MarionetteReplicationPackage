package com.outfit.ui.usecases.inbound.imagenames;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.outfit.ui.usecases.inbound.PrepareImageGalleryPageUseCase;
import com.outfit.ui.usecases.outbound.imagelist.FetchImageListGateway;

@Component
public class PrepareImageGalleryPageUseCaseImpl implements PrepareImageGalleryPageUseCase {

    private final FetchImageListGateway fetchImageListGateway;
    private static final int DEFAULT_PAGE_SIZE = 4;

    public PrepareImageGalleryPageUseCaseImpl(FetchImageListGateway fetchImageListGateway) {
        this.fetchImageListGateway = fetchImageListGateway;
    }

    @Override
    public GalleryPageResponse fetchImageNamesForPage(int pageNumber) {
        return fetchImageNamesForPage(pageNumber, DEFAULT_PAGE_SIZE);
    }

    // Overloaded method for custom page size
    public GalleryPageResponse fetchImageNamesForPage(int pageNumber, int pageSize) {
        try {
            List<String> completeImageList = fetchImageListGateway.fetchImageNamesList();

            if (completeImageList == null || completeImageList.isEmpty()) {
                return new GalleryPageResponse(
                        Collections.emptyList(),
                        pageNumber,
                        false,
                        0);
            }

            int totalImages = completeImageList.size();
            int startIndex = pageNumber * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalImages);

            // Handle invalid page numbers
            if (startIndex >= totalImages) {
                return new GalleryPageResponse(
                        Collections.emptyList(),
                        pageNumber,
                        false,
                        totalImages);
            }

            List<String> pageImageNames = completeImageList.subList(startIndex, endIndex);

            // Convert to gallery items with proxy URLs
            List<GalleryImageItem> galleryItems = pageImageNames.stream()
                    .map(imageName -> new GalleryImageItem(
                            imageName,
                            "/image-proxy/" + imageName // Enhanced image URL
                    ))
                    .collect(Collectors.toList());

            boolean hasNextPage = endIndex < totalImages;
            boolean hasPreviousPage = pageNumber > 0;

            return new GalleryPageResponse(
                    galleryItems,
                    pageNumber,
                    hasNextPage,
                    hasPreviousPage,
                    totalImages);

        } catch (Exception e) {
            System.err.println("Error preparing gallery page " + pageNumber + ": " + e.getMessage());
            return new GalleryPageResponse(
                    Collections.emptyList(),
                    pageNumber,
                    false,
                    false,
                    0);
        }
    }
}

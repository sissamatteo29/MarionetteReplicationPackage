package com.outfit.imagestore.usecases.outbound.imagerepo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ImageRepository {

    public byte[] getImage(String imageName);

    public int putImage(byte[] imageData, String originalExtension);  // Returns the imageId

    public List<String> getAllImages() throws IOException;

    public void clearRepository();

    public Map<String, Object> getRepositoryStats();
    
}

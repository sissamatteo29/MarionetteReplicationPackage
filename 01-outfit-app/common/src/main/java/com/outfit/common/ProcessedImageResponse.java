package com.outfit.common;

public class ProcessedImageResponse {
    private final String fileName;
    private String url;

    public ProcessedImageResponse(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String newUrl) {
        this.url = newUrl;
    } 
}

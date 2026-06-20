package com.indiacybercafe.printhub.models;

import java.io.Serializable;

public class FileModel implements Serializable {
    private String id;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String downloadUrl;
    private String storagePath;
    private long uploadedAt;
    private String category; // PDF, DOC, IMAGE etc.
    private int pageCount = 1;
    private String uploadStatus; // Pending, Uploading, Success, Failed

    public FileModel() {}

    public FileModel(String fileName, String fileType, long fileSize, String category) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.category = category;
        this.uploadStatus = "Pending";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public long getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(long uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
}

package com.devhub.service;

import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Service for managing file uploads to Google Cloud Storage.
 */
@Slf4j
@Service
public class StorageService {

    @Value("${gcp.storage.bucket:devhub-storage}")
    private String bucketName;

    private final Storage storage;

    public StorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Upload a file to GCS and return the public URL.
     *
     * @param file The file to upload
     * @param folder The folder path in the bucket (e.g., "snippets", "assets")
     * @return The GCS URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        log.info("Uploading file to GCS: {} to folder: {}", file.getOriginalFilename(), folder);

        // Generate unique filename to avoid collisions
        String filename = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(bucketName, filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        String url = String.format("gs://%s/%s", bucketName, filename);
        log.info("File uploaded successfully: {}", url);
        return url;
    }

    /**
     * Upload a file with custom metadata.
     */
    public String uploadFileWithMetadata(MultipartFile file, String folder, String contentType) throws IOException {
        log.info("Uploading file with metadata: {}", file.getOriginalFilename());

        String filename = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(bucketName, filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType != null ? contentType : file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        return String.format("gs://%s/%s", bucketName, filename);
    }

    /**
     * Delete a file from GCS.
     */
    public boolean deleteFile(String gcsUrl) {
        log.info("Deleting file from GCS: {}", gcsUrl);

        // Parse GCS URL (gs://bucket/path)
        if (!gcsUrl.startsWith("gs://")) {
            log.error("Invalid GCS URL format: {}", gcsUrl);
            return false;
        }

        String pathWithoutPrefix = gcsUrl.substring(5); // Remove "gs://"
        String[] parts = pathWithoutPrefix.split("/", 2);

        if (parts.length != 2) {
            log.error("Invalid GCS URL structure: {}", gcsUrl);
            return false;
        }

        String bucket = parts[0];
        String objectName = parts[1];

        BlobId blobId = BlobId.of(bucket, objectName);
        boolean deleted = storage.delete(blobId);

        if (deleted) {
            log.info("File deleted successfully: {}", gcsUrl);
        } else {
            log.warn("File not found or already deleted: {}", gcsUrl);
        }

        return deleted;
    }

    /**
     * Get a signed URL for temporary access to a private file.
     */
    public String getSignedUrl(String gcsUrl, long durationMinutes) {
        log.info("Generating signed URL for: {}", gcsUrl);

        // Parse GCS URL
        String pathWithoutPrefix = gcsUrl.substring(5);
        String[] parts = pathWithoutPrefix.split("/", 2);
        String bucket = parts[0];
        String objectName = parts[1];

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, objectName)).build();

        // Generate signed URL valid for specified duration
        String url = storage.signUrl(
                blobInfo,
                durationMinutes,
                java.util.concurrent.TimeUnit.MINUTES
        ).toString();

        log.info("Signed URL generated successfully");
        return url;
    }

    /**
     * Check if a file exists in GCS.
     */
    public boolean fileExists(String gcsUrl) {
        if (!gcsUrl.startsWith("gs://")) {
            return false;
        }

        String pathWithoutPrefix = gcsUrl.substring(5);
        String[] parts = pathWithoutPrefix.split("/", 2);

        if (parts.length != 2) {
            return false;
        }

        String bucket = parts[0];
        String objectName = parts[1];

        Blob blob = storage.get(BlobId.of(bucket, objectName));
        return blob != null && blob.exists();
    }
}

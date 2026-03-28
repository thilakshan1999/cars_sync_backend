package com.hinetics.caresync.service;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.hinetics.caresync.dto.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {
    @Value("${google.cloud.credentials.path}")
    private String credentialsPath;
    private Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucketName = "care-sync-bucket"; // your bucket name

    public FileUploadResult uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename()+"_"+System.currentTimeMillis() ;

        String contentType = file.getContentType();
        if (contentType == null || contentType.equals("application/octet-stream")) {
            Path path = Paths.get(file.getOriginalFilename());
            contentType = Files.probeContentType(path);
            if (contentType == null) {
                // Default fallback for known extensions
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else {
                    contentType = "application/octet-stream";
                }
            }
        }

        System.out.println("contentType : ");
        System.out.println(contentType);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName)
                .setContentType(contentType)
                .build();

        System.out.println("fileName : ");
        System.out.println(fileName);

        storage.create(blobInfo, file.getBytes());
        String fileUrl = "https://storage.googleapis.com/" + bucketName + "/" + fileName;

        return new FileUploadResult(fileName, fileUrl,contentType);
    }

    public boolean deleteFile(String fileName) {
        System.out.println("fileName");
        System.out.println(fileName);
        // The fileName should be the object name in GCS, e.g., "1693345678900_myfile.pdf"
        BlobId blobId = BlobId.of(bucketName, fileName);
        return storage.delete(blobId); // returns true if deletion succeeded
    }

    public String generateSignedUrl(String fileName) {
        System.out.println("fileName: " + fileName);

        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName).build();

        try {
            String keyPath=credentialsPath;

            // Create a Storage object using the mounted service account JSON
            Storage signingStorage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(
                            new FileInputStream(keyPath) // mounted secret path
                    ))
                    .build()
                    .getService();

            // Generate signed URL (15 minutes)
            URL signedUrl = signingStorage.signUrl(
                    blobInfo,
                    15,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature()
            );

            return signedUrl.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null; // or throw a custom exception
        }
    }
}

package com.hinetics.caresync.service.ai;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentAIService {
//    private static final String PROJECT_ID = "care-sync-467705";
//    private static final String LOCATION = "us";
//    private static final String PROCESSOR_ID = "6adda2a5acbf6f8d";
    @Value("${google.documentai.project-id}")
    private String projectId;

    @Value("${google.documentai.location}")
    private String location;

    @Value("${google.documentai.processor-id}")
    private String processorId;

    public String extractText(Path filePath, String mimeType) throws IOException {
        // Load credentials
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ClassPathResource("credentials/service-account.json").getInputStream()
        );

        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {
            String name = String.format("projects/%s/locations/%s/processors/%s",
                    projectId, location, processorId);

            // Load file and set MIME type
            byte[] fileData = Files.readAllBytes(filePath);
            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(ByteString.copyFrom(fileData))
                    .setMimeType(mimeType)
                    .build();

            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(name)
                    .setRawDocument(rawDocument)
                    .build();

            ProcessResponse result = client.processDocument(request);
            Document document = result.getDocument();
            return document.getText();
        }
    }
}

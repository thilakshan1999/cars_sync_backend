package com.hinetics.caresync.service.ai;


import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class CloudVisionService {
    public String extractTextFromImage(String imagePath) throws IOException {
        ByteString imgBytes = ByteString.readFrom(Files.newInputStream(Path.of(imagePath)));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            List<AnnotateImageResponse> responses = client.batchAnnotateImages(List.of(request)).getResponsesList();
            if (!responses.isEmpty() && !responses.get(0).hasError()) {
                return responses.get(0).getFullTextAnnotation().getText();
            } else {
                return "No text found or error occurred.";
            }
        }
    }
}

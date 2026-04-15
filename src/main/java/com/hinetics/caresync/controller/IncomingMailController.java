package com.hinetics.caresync.controller;

import com.hinetics.caresync.service.IncomingMailService;
import com.hinetics.caresync.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class IncomingMailController {

    private final IncomingMailService mailService;

//    @PostMapping("/incoming")
//    public ResponseEntity<String> receiveMail(
//            @RequestParam(value = "from", required = false) String from,
//            @RequestParam(value = "To", required = false) String to,   // 👈 ADD THIS
//            @RequestParam(value = "subject", required = false) String subject,
//            @RequestParam(value = "body-plain", required = false) String bodyPlain,
//            @RequestParam(value = "body-html", required = false) String bodyHtml,
//            HttpServletRequest request
//    ) {
//
//        System.out.println("===== NEW EMAIL RECEIVED =====");
//
//        // ✅ Basic fields
//        System.out.println("From: " + from);
//        System.out.println("To: " + to);   // 👈 PRINT TO
//        System.out.println("Subject: " + subject);
//        System.out.println("Body (plain): " + bodyPlain);
//        System.out.println("Body (html): " + bodyHtml);
//
//        // 🔍 Print ALL params
//        System.out.println("----- ALL PARAMS -----");
//        request.getParameterMap().forEach((key, value) -> {
//            System.out.println(key + " = " + Arrays.toString(value));
//        });
//
//        // 📎 Handle attachments PROPERLY
//        if (request instanceof MultipartHttpServletRequest multiRequest) {
//
//            Map<String, MultipartFile> fileMap = multiRequest.getFileMap();
//
//            if (!fileMap.isEmpty()) {
//                System.out.println("----- ATTACHMENTS -----");
//
//                fileMap.forEach((key, file) -> {
//                    try {
//                        System.out.println("Key: " + key); // attachment-1
//                        System.out.println("File Name: " + file.getOriginalFilename());
//                        System.out.println("Content Type: " + file.getContentType());
//                        System.out.println("Size: " + file.getSize() + " bytes");
//
//                        // ⚠️ only for text files
//                        if (file.getContentType() != null && file.getContentType().startsWith("text")) {
//                            String content = new String(file.getBytes());
//                            System.out.println("Content: " + content);
//                        }
//
//                    } catch (Exception e) {
//                        System.out.println("Error reading file: " + e.getMessage());
//                    }
//                });
//
//            } else {
//                System.out.println("No attachments received.");
//            }
//        }
//
//        System.out.println("==============================");
//
//        return ResponseEntity.ok("Received");
//    }

    @PostMapping("/incoming")
    public ResponseEntity<String> receiveMail(
            @RequestParam(value = "recipient", required = false) String receiverEmail,
            HttpServletRequest request
    ) {

        try {
            List<MultipartFile> files = new ArrayList<>();

            if (request instanceof MultipartHttpServletRequest multiRequest) {
                multiRequest.getFileMap().forEach((key, file) -> {
                    if (file != null && !file.isEmpty()) {
                        files.add(file);
                    }
                });
            }

            System.out.println("Total files received: " + files.size());
            System.out.println("Receiver Email: " + receiverEmail);

            // ✅ Call service only if files exist
            if (!files.isEmpty()) {
                mailService.saveMultipleDocumentsViaMail(files, receiverEmail);
            } else {
                System.out.println("No valid attachments to process.");
            }

            return ResponseEntity.ok("Processed");

        } catch (Exception e) {

            System.out.println("❌ ERROR processing incoming mail:"+e);
            return ResponseEntity.ok("ERROR processing incoming mail");
        }
    }
}

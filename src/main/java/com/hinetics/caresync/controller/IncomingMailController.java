package com.hinetics.caresync.controller;

import com.hinetics.caresync.service.IncomingMailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class IncomingMailController {

    private final IncomingMailService mailService;

    @PostMapping("/incoming")
    public ResponseEntity<String> receiveMail(
            @RequestParam(value = "recipient", required = false) String receiverEmail,
            HttpServletRequest request
    ) {

        try {
            System.out.println("Raw Receiver: " + receiverEmail);

            List<File> tempFiles = new ArrayList<>();

            if (request instanceof MultipartHttpServletRequest multiRequest) {
                multiRequest.getFileMap().forEach((key, file) -> {
                    try {
                        if (key.startsWith("attachment") && file != null && !file.isEmpty()) {

                            // ✅ Convert to temp file (VERY IMPORTANT)
                            File temp = File.createTempFile("mail_", "_" + file.getOriginalFilename());
                            file.transferTo(temp);
                            tempFiles.add(temp);
                        }
                    } catch (Exception e) {
                        System.out.println("Error converting file: " + file.getOriginalFilename());
                    }
                });
            }

            if (!tempFiles.isEmpty()) {
                mailService.processIncomingMailAsync(tempFiles, receiverEmail);
            }
            return ResponseEntity.ok("Processed");

        } catch (Exception e) {

            System.out.println("❌ ERROR processing incoming mail:"+e);
            return ResponseEntity.ok("ERROR processing incoming mail");
        }
    }
}

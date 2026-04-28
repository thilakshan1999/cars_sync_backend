package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.FileUploadResult;
import com.hinetics.caresync.entity.UploadTask;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableAsync
public class IncomingMailService {
    private final FileStorageService fileStorageService;
    private final UploadTaskService uploadTaskService;
    private final AsyncShareProcessor asyncShareProcessor;
    private final UserService userService;

    @Async
    public void processIncomingMailAsync(List<File> files, String email) {

        try {
            User user = userService.getUserBySystemEmail(email);

            for (File file : files) {
                try {
                    saveDocument(file, user);
                } catch (Exception e) {
                    System.out.println("❌ Error processing file: " + file.getName());
                } finally {
                    // ✅ cleanup temp file
                    file.delete();
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Error in async mail processing: " + e.getMessage());
        }
    }

    public void saveDocument(File  file, User user) throws Exception {

        FileUploadResult uploadResult = fileStorageService.uploadFile(file);

        UploadTask task = uploadTaskService.createTask(
                uploadResult.getFileName(),
                uploadResult.getFileUrl(),
                uploadResult.getFileType(),
                user.getId(),
                user.getEmail()
        );

        asyncShareProcessor.processDocumentAsync(
                task.getId(),
                uploadResult,
                user.getId(),
                user.getEmail()
        );
    }
}

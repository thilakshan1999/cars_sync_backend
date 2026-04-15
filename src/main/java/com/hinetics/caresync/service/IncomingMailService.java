package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.FileUploadResult;
import com.hinetics.caresync.entity.UploadTask;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomingMailService {
    private final FileStorageService fileStorageService;
    private final UploadTaskService uploadTaskService;
    private final AsyncShareProcessor asyncShareProcessor;
    private final UserService userService;

    public void saveMultipleDocumentsViaMail(List<MultipartFile> files, String email) throws Exception {
        User user = userService.getUserBySystemEmail(email);

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                saveDocument(file, user);
            }
        }
    }

    public void saveDocument(MultipartFile file, User user) throws Exception {

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

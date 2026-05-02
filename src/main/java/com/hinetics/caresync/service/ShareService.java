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
public class ShareService {
    private final FileStorageService fileStorageService;
    private final UploadTaskService uploadTaskService;
    private final AsyncShareProcessor asyncShareProcessor;
    private final UserService userService;
    private final DocumentService documentService;

    public void saveDocumentViaShare(MultipartFile file, Long patientId, String email) throws Exception {

        User user = userService.getUserByEmail(email);
        User targetUser = documentService.resolveTargetUser(user,patientId,true);

        FileUploadResult uploadResult = fileStorageService.uploadFile(file);

        String hash = uploadResult.getFileHash();

        boolean isDuplicate = documentService.checkExistsByFileHash(hash,user.getId());

        UploadTask task = uploadTaskService.createTask(
                uploadResult.getFileName(),
                uploadResult.getFileUrl(),
                uploadResult.getFileType(),
                hash,
                targetUser.getId(),
                user.getEmail(),
                isDuplicate // 👈 pass flag
        );

        if(!isDuplicate){
            asyncShareProcessor.processDocumentAsync(
                    task.getId(),
                    uploadResult,
                    targetUser.getId(),
                    targetUser.getEmail()
            );
        }


    }

}

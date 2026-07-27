// util/FileStorageUtil.java
package com.example.demo.util;

import com.example.demo.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class FileStorageUtil {

    @Value("${app.resume.upload-dir:uploads/resumes}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String userId) {
        validateFile(file);
        try {
            Path userDir = Paths.get(uploadDir, userId);
            Files.createDirectories(userDir);

            String extension = getExtension(file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetPath = userDir.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file: " + filePath, e);
        }
    }

    public byte[] loadFile(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file: " + filePath, e);
        }
    }

    public String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new FileStorageException("Invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload an empty file");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ext.equals("pdf") && !ext.equals("docx")) {
            throw new FileStorageException("Only PDF and DOCX files are allowed");
        }
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new FileStorageException("File size exceeds 5MB limit");
        }
    }
}
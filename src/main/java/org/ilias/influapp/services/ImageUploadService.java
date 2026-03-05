package org.ilias.influapp.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
public class ImageUploadService {

    private static final String UPLOAD_DIR = "uploads";

    public String uploadImage(MultipartFile file, String prefix, Long entityId) throws IOException {
        validateFile(file);

        String filename = generateSafeFilename(file, prefix, entityId);

        Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(filename).normalize();

        // Security check: prevent directory traversal
        if (!target.startsWith(uploadDir)) {
            throw new SecurityException("Invalid file path");
        }

        file.transferTo(target.toFile());

        // Return URL path
        return "/" + UPLOAD_DIR + "/" + filename;
    }


    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }


    private String generateSafeFilename(MultipartFile file, String prefix, Long entityId) {
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safe = original.replaceAll("[^a-zA-Z0-9.\\-_/]", "_");
        return prefix + "-" + entityId + "-" + System.currentTimeMillis() + "-" + safe;
    }
}

package org.ilias.influapp.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service για τη διαχείριση upload εικόνων
 */
@Service
public class ImageUploadService {

    private static final String UPLOAD_DIR = "uploads";

    /**
     * Upload εικόνας με validation και security checks
     *
     * @param file Το αρχείο προς upload
     * @param prefix Το prefix για το filename (π.χ. "influencer", "business")
     * @param entityId Το ID του entity (για unique filename)
     * @return Το URL path της εικόνας (π.χ. "/uploads/influencer-1-12345.jpg")
     * @throws IOException Αν υπάρχει πρόβλημα με το file system
     * @throws IllegalArgumentException Αν το αρχείο δεν είναι έγκυρο
     * @throws SecurityException Αν υπάρχει πρόβλημα ασφαλείας
     */
    public String uploadImage(MultipartFile file, String prefix, Long entityId) throws IOException {
        // Validation
        validateFile(file);

        // Generate safe filename
        String filename = generateSafeFilename(file, prefix, entityId);

        // Save file
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

    /**
     * Validation του uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    /**
     * Δημιουργία ασφαλούς filename
     */
    private String generateSafeFilename(MultipartFile file, String prefix, Long entityId) {
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safe = original.replaceAll("[^a-zA-Z0-9.\\-_/]", "_");
        return prefix + "-" + entityId + "-" + System.currentTimeMillis() + "-" + safe;
    }
}

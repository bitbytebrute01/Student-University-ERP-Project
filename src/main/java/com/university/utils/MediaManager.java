package com.university.utils;

import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MediaManager {
    private static final String DEFAULT_UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg");
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList("pdf", "docx", "zip");
    private static final List<String> GENERAL_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "pdf", "docx", "zip");

    static {
        File dir = uploadRoot().toFile();
        if (!dir.exists()) dir.mkdirs();
    }

    public static String saveFile(File file, String subDir) throws IOException {
        validateFile(file, GENERAL_EXTENSIONS, "file");
        return copyFile(file, subDir);
    }

    public static String saveImage(File file, String subDir) throws IOException {
        validateFile(file, IMAGE_EXTENSIONS, "image");
        validateReadableImage(file);
        return copyFile(file, subDir);
    }

    public static String saveSubmissionFile(File file, String subDir) throws IOException {
        validateFile(file, DOCUMENT_EXTENSIONS, "assignment submission");
        return copyFile(file, subDir);
    }

    public static String savePdf(File file, String subDir) throws IOException {
        validateFile(file, Arrays.asList("pdf"), "PDF");
        return copyFile(file, subDir);
    }

    private static String copyFile(File file, String subDir) throws IOException {
        Path targetDirectory = resolveSafeUploadDirectory(subDir);
        Files.createDirectories(targetDirectory);
        
        String extension = getFileExtension(file.getName());
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = targetDirectory.resolve(fileName);
        
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        return targetPath.toString();
    }

    public static void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }
    }

    private static void validateFile(File file, List<String> allowedExtensions, String label) throws IOException {
        if (file == null) throw new FileNotFoundException("No " + label + " selected.");
        if (!file.exists()) throw new FileNotFoundException("File does not exist.");
        if (!file.isFile()) throw new IOException("Selected path is not a file.");
        if (file.length() > MAX_FILE_SIZE) throw new IOException("File size exceeds 5MB limit.");
        
        String extension = getFileExtension(file.getName()).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IOException("Unsupported file format: " + extension);
        }
    }

    private static void validateReadableImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IOException("Selected file is not a readable image.");
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "";
        return fileName.substring(lastDot + 1);
    }

    private static Path uploadRoot() {
        return Paths.get(System.getProperty("university.upload.dir", DEFAULT_UPLOAD_DIR));
    }

    private static Path resolveSafeUploadDirectory(String subDir) throws IOException {
        Path root = uploadRoot().toAbsolutePath().normalize();
        String requestedSubDir = subDir == null ? "" : subDir;
        Path targetDirectory = root.resolve(requestedSubDir).normalize();
        if (!targetDirectory.startsWith(root)) {
            throw new IOException("Invalid upload directory.");
        }
        return targetDirectory;
    }
}

package com.university.utils;

import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class MediaManager {
    private static final String DEFAULT_UPLOAD_DIR = "uploads";
    private static final String TEMP_DIR_NAME = "temp";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg");
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList("pdf", "docx", "zip");
    private static final List<String> GENERAL_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "pdf", "docx", "zip");

    // retention in minutes for temp files
    private static final long TEMP_RETENTION_MINUTES = Long.parseLong(System.getProperty("university.upload.temp.retention.minutes", "30"));

    static {
        try {
            Files.createDirectories(uploadRoot());
            Files.createDirectories(uploadRoot().resolve(TEMP_DIR_NAME));
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize upload directories: " + e.getMessage(), e);
        }
    }

    // Save directly to final directory (legacy callers)
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

    public static List<String> saveSubmissionFiles(List<File> files, String subDir) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new FileNotFoundException("No assignment submission files selected.");
        }
        List<String> savedPaths = new ArrayList<>();
        for (File file : files) {
            savedPaths.add(saveSubmissionFile(file, subDir));
        }
        return savedPaths;
    }

    public static String savePdf(File file, String subDir) throws IOException {
        validateFile(file, Arrays.asList("pdf"), "PDF");
        return copyFile(file, subDir);
    }

    // New API: save to temp directory for later finalize/rollback
    public static String saveTemp(File file) throws IOException {
        validateFile(file, GENERAL_EXTENSIONS, "file");
        return copyToTemp(file);
    }

    public static String saveTempImage(File file) throws IOException {
        validateFile(file, IMAGE_EXTENSIONS, "image");
        validateReadableImage(file);
        return copyToTemp(file);
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

    private static String copyToTemp(File file) throws IOException {
        Path tempDir = uploadRoot().resolve(TEMP_DIR_NAME);
        Files.createDirectories(tempDir);
        String extension = getFileExtension(file.getName());
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = tempDir.resolve(fileName);
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toString();
    }

    // Finalize a temp file into a destination subdirectory (returns final path)
    public static String finalizeUpload(String tempPathStr, String destSubDir) throws IOException {
        if (tempPathStr == null) throw new IllegalArgumentException("tempPath is required");
        Path tempPath = Path.of(tempPathStr).toAbsolutePath().normalize();
        Path tempDir = uploadRoot().resolve(TEMP_DIR_NAME).toAbsolutePath().normalize();
        if (!tempPath.startsWith(tempDir)) {
            // already final or external path
            return tempPath.toString();
        }

        Path destDir = resolveSafeUploadDirectory(destSubDir);
        Files.createDirectories(destDir);
        String fileName = tempPath.getFileName().toString();
        Path finalPath = destDir.resolve(fileName).toAbsolutePath().normalize();
        Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return finalPath.toString();
    }

    public static boolean isTempPath(String path) {
        if (path == null) return false;
        try {
            Path p = Path.of(path).toAbsolutePath().normalize();
            Path tempDir = uploadRoot().resolve(TEMP_DIR_NAME).toAbsolutePath().normalize();
            return p.startsWith(tempDir);
        } catch (Exception e) {
            return false;
        }
    }

    // Cleanup old temp files older than retention
    public static int cleanupOldTempFiles() {
        long thresholdMillis = System.currentTimeMillis() - (TEMP_RETENTION_MINUTES * 60L * 1000L);
        int deleted = 0;
        Path tempDir = uploadRoot().resolve(TEMP_DIR_NAME);
        try (Stream<Path> stream = Files.list(tempDir)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                try {
                    if (Files.isRegularFile(p) && Files.getLastModifiedTime(p).toMillis() < thresholdMillis) {
                        Files.deleteIfExists(p);
                        deleted++;
                    }
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
        return deleted;
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

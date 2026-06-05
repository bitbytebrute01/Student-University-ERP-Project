package com.university.integration;

import com.university.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class IntegrationTestBase {
    protected Path tempDir;
    protected String dbUrl;

    @BeforeEach
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("erp-integration-");
        System.setProperty("university.upload.dir", tempDir.toString());
        Path dbFile = tempDir.resolve("test.db");
        dbUrl = "jdbc:sqlite:" + dbFile.toString();
        System.setProperty("university.db.url", dbUrl);

        DatabaseManager.initializeSchema();
    }

    @AfterEach
    public void teardown() throws IOException {
        // cleanup files
        try (var stream = Files.walk(tempDir)) {
            stream.sorted((a,b)->b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }
}

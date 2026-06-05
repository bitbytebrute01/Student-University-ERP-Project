package com.university.integration;

import com.university.students.StudentManager;
import com.university.models.Student;
import com.university.utils.MediaManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;

public class ProfilePhotoPersistenceTest extends IntegrationTestBase {

    @Test
    public void testProfilePhotoPersistedAfterFinalizeAndUpdate() throws Exception {
        StudentManager sm = new StudentManager();
        Student s = new Student("S-100", "Test Student", "test@student.edu", "9999999999", "CSE", 3);
        sm.addStudent(s);

        // create a small valid PNG image file in local FS
        Path tmpFile = tempDir.resolve("upload-source.png");
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0,0,2,2);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", tmpFile.toFile());

        // save to temp via MediaManager
        String tempPath = MediaManager.saveTempImage(tmpFile.toFile());
        assertNotNull(tempPath);
        assertTrue(MediaManager.isTempPath(tempPath));

        // finalize into profiles folder and update student
        String finalPath = null;
        try {
            finalPath = MediaManager.finalizeUpload(tempPath, "profiles/" + s.getId());
            assertNotNull(finalPath);
            assertFalse(MediaManager.isTempPath(finalPath));

            s.setProfilePicturePath(finalPath);
            sm.updateStudent(s);

            // reload from DB
            Student loaded = sm.searchById(s.getId());
            assertNotNull(loaded);
            assertEquals(finalPath, loaded.getProfilePicturePath());
            assertTrue(Files.exists(Path.of(finalPath)));
        } finally {
            // cleanup
            if (finalPath != null) MediaManager.deleteFile(finalPath);
        }
    }
}

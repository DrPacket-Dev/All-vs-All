package org.drpacket.allvsall;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseManagerTest {
    @Test
    void savesAndLoadsKitSelection() throws Exception {
        Path tempDb = Files.createTempFile("allvsall-test", ".db");
        Files.deleteIfExists(tempDb);

        DatabaseManager manager = new DatabaseManager(tempDb.toFile());
        manager.initialize();
        manager.saveKit("uhc", "layout-data");
        String loaded = manager.loadKit("uhc");

        assertEquals("layout-data", loaded);
        Files.deleteIfExists(tempDb);
    }
}

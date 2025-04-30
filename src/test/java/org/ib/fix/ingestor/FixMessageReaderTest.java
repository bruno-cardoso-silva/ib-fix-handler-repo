package org.ib.fix.ingestor;

import org.ib.fix.model.RawFixMessage;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FixFileReaderTest {

    @Test
    void testReadBatches_WithExactBatchSize() throws Exception {
        Path tempFile = Files.createTempFile("fix_test", ".txt");

        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            for (int i = 0; i < 1000; i++) {
                writer.write("8=FIX.4.4^35=8^Test" + i + "\n");
            }
        }

        FixFileReader reader = new FixFileReader(tempFile.toString(), 1000);
        List<List<RawFixMessage>> batches = reader.readBatches();

        assertEquals(1, batches.size());
        assertEquals(1000, batches.get(0).size());
        assertTrue(batches.get(0).get(0).toString().contains("Test0"));
    }

    @Test
    void testReadBatches_WithMultipleBatches() throws Exception {
        Path tempFile = Files.createTempFile("fix_test", ".txt");

        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            for (int i = 0; i < 2500; i++) {
                writer.write("8=FIX.4.4^35=8^ID=" + i + "\n");
            }
        }

        FixFileReader reader = new FixFileReader(tempFile.toString(), 1000);
        List<List<RawFixMessage>> batches = reader.readBatches();

        assertEquals(3, batches.size());
        assertEquals(1000, batches.get(0).size());
        assertEquals(1000, batches.get(1).size());
        assertEquals(500, batches.get(2).size());
    }

    @Test
    void testReadBatches_EmptyFile() throws Exception {
        Path tempFile = Files.createTempFile("fix_test_empty", ".txt");

        FixFileReader reader = new FixFileReader(tempFile.toString(), 1000);
        List<List<RawFixMessage>> batches = reader.readBatches();

        assertTrue(batches.isEmpty());
    }
}

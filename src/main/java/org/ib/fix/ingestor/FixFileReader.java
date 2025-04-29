package org.ib.fix.ingestor;

import org.ib.fix.model.RawFixMessage;

import java.io.*;
import java.util.*;

public class FixFileReader {

    private final String filePath;

    public FixFileReader(String filePath) {
        this.filePath = filePath;
    }

    public List<List<RawFixMessage>> readBatches() {
        List<List<RawFixMessage>> batches = new ArrayList<>();
        List<RawFixMessage> batch = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                RawFixMessage message = parseFixMessage(line); // Implement message parsing
                batch.add(message);

                // When batch reaches a certain size, dispatch it
                if (batch.size() >= 1000) { // Adjust batch size as needed
                    batches.add(batch);
                    batch = new ArrayList<>();
                }
            }
            // Add the last batch if not empty
            if (!batch.isEmpty()) {
                batches.add(batch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return batches;
    }

    private RawFixMessage parseFixMessage(String line) {
        // Parse a single line into a RawFixMessage object
        return new RawFixMessage(line); // Replace with actual parsing logic
    }
}

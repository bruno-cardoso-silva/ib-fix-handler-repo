package org.ib.fix.ingestor;

import org.ib.fix.model.RawFixMessage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FixFileReader {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final String filePath;
    private final int batchSize;

    public FixFileReader(String filePath) {
        this(filePath, DEFAULT_BATCH_SIZE);
    }

    public FixFileReader(String filePath, int batchSize) {
        this.filePath = filePath;
        this.batchSize = batchSize;
    }

    public List<List<RawFixMessage>> readBatches() throws IOException {
        List<List<RawFixMessage>> batches = new ArrayList<>();
        List<RawFixMessage> currentBatch = new ArrayList<>();

        try (BufferedReader reader = createReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                RawFixMessage message = toRawFixMessage(line);
                currentBatch.add(message);

                if (currentBatch.size() >= batchSize) {
                    batches.add(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            }

            if (!currentBatch.isEmpty()) {
                batches.add(currentBatch);
            }
        }

        return batches;
    }

    private BufferedReader createReader() throws IOException {
        return new BufferedReader(new FileReader(filePath));
    }

    private RawFixMessage toRawFixMessage(String line) {
        return new RawFixMessage(line); // replace with actual parsing logic if needed
    }
}

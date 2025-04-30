package org.ib.fix.execution;

import quickfix.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class FixMessageReportManager {
    private final ConcurrentLinkedQueue<String> csvDataQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> fulfillDataQueue = new ConcurrentLinkedQueue<>();
    private final BlockingQueue<List<Message>> inputQueue;
    private final ExecutorService executor;

    private static final int NUM_WORKERS = 4;

    public FixMessageReportManager(BlockingQueue<List<Message>> inputQueue) {
        this.inputQueue = inputQueue;
        this.executor = Executors.newFixedThreadPool(NUM_WORKERS);
    }

    public void start() {
        for (int i = 0; i < NUM_WORKERS; i++) {
            executor.submit(new FixMessageProcessorWorker(inputQueue, csvDataQueue, fulfillDataQueue));
        }
    }

    public void shutdown() {
        for (int i = 0; i < NUM_WORKERS; i++) {
            try {
                inputQueue.put(Collections.emptyList());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Set the interrupt flag
            }
        }
        // Shutdown the executor and await termination
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                // Force shutdown if workers are taking too long
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Ensure we restore the interrupt flag
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        writeBatchToFile("AllMsgs.csv", csvDataQueue);
        writeBatchToFile("FullFill.txt", fulfillDataQueue);

    }

    private static final int BATCH_SIZE = 100;
    private final Object writeLock = new Object();

    private void writeBatchToFile(String fileName, ConcurrentLinkedQueue<String> lines) {
        List<String> linesToWrite = new ArrayList<>(BATCH_SIZE);
        long startTime = System.nanoTime();

        try {
            while (!lines.isEmpty()) {
                while (!lines.isEmpty() && linesToWrite.size() < BATCH_SIZE) {
                    String line = lines.poll(); // Poll until batch is filled
                    if (line != null) {
                        linesToWrite.add(line);
                    }
                }

                if (!linesToWrite.isEmpty()) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                            for (String line : linesToWrite) {
                                writer.write(line);
                                writer.newLine();
                            }
                        }
                        // After writing, clear the list for the next batch
                        linesToWrite.clear();
                    }
            }

            long elapsedTime = System.nanoTime() - startTime;
            System.out.println("Time taken to write batch: " + elapsedTime / 1_000_000 + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

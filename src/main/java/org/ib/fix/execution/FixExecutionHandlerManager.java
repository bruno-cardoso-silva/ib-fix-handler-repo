package org.ib.fix.execution;

import org.ib.fix.Constants;
import quickfix.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class FixExecutionHandlerManager {

    private static final int NUM_PROCESSING_THREADS = 4;
    private static final int BATCH_WRITE_SIZE = 100;
    private static final long WORKER_TERMINATION_TIMEOUT_SECONDS = 60;

    private final BlockingQueue<List<Message>> messageQueue;
    private final ConcurrentLinkedQueue<String> allMessagesQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> fullFillQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService processingExecutor;
    private final Object fileWriteLock = new Object(); // Consider using for more controlled file writing

    public FixExecutionHandlerManager(BlockingQueue<List<Message>> messageQueue) {
        this.messageQueue = messageQueue;
        this.processingExecutor = Executors.newFixedThreadPool(NUM_PROCESSING_THREADS);
    }

    public void start() {
        for (int i = 0; i < NUM_PROCESSING_THREADS; i++) {
            processingExecutor.submit(new FixMessageProcessorWorker(messageQueue, allMessagesQueue, fullFillQueue));
        }
        System.out.println("Started " + NUM_PROCESSING_THREADS + " message processing threads.");
    }

    public void shutdown() {
        signalWorkersToStop();
        shutdownAndAwaitTermination();
        writeQueuedDataToFile();
    }

    private void signalWorkersToStop() {
        System.out.println("Signaling workers to stop...");
        for (int i = 0; i < NUM_PROCESSING_THREADS; i++) {
            try {
                messageQueue.put(Collections.emptyList()); // Use an empty list as a termination signal
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while signaling worker to stop: " + e.getMessage());
            }
        }
    }

    private void shutdownAndAwaitTermination() {
        processingExecutor.shutdown();
        try {
            if (!processingExecutor.awaitTermination(WORKER_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.err.println("Processing workers did not terminate within the timeout. Forcing shutdown.");
                processingExecutor.shutdownNow();
            } else {
                System.out.println("Processing workers shut down gracefully.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processingExecutor.shutdownNow();
            System.err.println("Interrupted while awaiting worker termination: " + e.getMessage());
        }
    }

    private void writeQueuedDataToFile() {
        System.out.println("Writing processed data to files...");
        writeBatchToFile(Constants.ALL_MESSAGES_FILE_NAME.getName(), allMessagesQueue);
        writeBatchToFile(Constants.FULL_FILL_REPORT_FILE_NAME.getName(), fullFillQueue);
        System.out.println("Writing to files completed.");
    }

    private void writeBatchToFile(String fileName, ConcurrentLinkedQueue<String> lines) {
        List<String> buffer = new ArrayList<>(BATCH_WRITE_SIZE);
        long startTime = System.nanoTime();
        int totalLinesWritten = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            while (!lines.isEmpty()) {
                String line = lines.poll();
                if (line != null) {
                    buffer.add(line);
                    if (buffer.size() >= BATCH_WRITE_SIZE) {
                        writeBatch(writer, buffer);
                        totalLinesWritten += buffer.size();
                        buffer.clear();
                    }
                }
            }
            // Write any remaining lines in the buffer
            if (!buffer.isEmpty()) {
                writeBatch(writer, buffer);
                totalLinesWritten += buffer.size();
                buffer.clear();
            }
            long elapsedTime = System.nanoTime() - startTime;
            System.out.println("Wrote " + totalLinesWritten + " lines to " + fileName + " in " + elapsedTime / 1_000_000 + " ms.");

        } catch (IOException e) {
            System.err.println("Error writing to " + fileName + ": " + e.getMessage());
        }
    }

    private void writeBatch(BufferedWriter writer, List<String> batch) throws IOException {
        synchronized (fileWriteLock) { // Added synchronization for thread-safe writing
            for (String line : batch) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
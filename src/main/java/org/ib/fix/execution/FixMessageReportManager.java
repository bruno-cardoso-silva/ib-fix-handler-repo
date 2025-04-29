package org.ib.fix.execution;

import quickfix.Message;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class FixMessageReportManager {

    private final BlockingQueue<List<Message>> inputQueue;
    private final ExecutorService executor;

    private static final int NUM_WORKERS = 4;

    public FixMessageReportManager(BlockingQueue<List<Message>> inputQueue) {
        this.inputQueue = inputQueue;
        this.executor = Executors.newFixedThreadPool(NUM_WORKERS);
    }

    public void start() {
        for (int i = 0; i < NUM_WORKERS; i++) {
            executor.submit(new FixMessageProcessorWorker(inputQueue));
        }
    }

    public void shutdown() {
        // Send poison pills to stop workers
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
    }
}

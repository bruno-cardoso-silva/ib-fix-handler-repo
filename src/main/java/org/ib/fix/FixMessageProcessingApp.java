package org.ib.fix;

import org.ib.fix.execution.FixExecutionReport;
import org.ib.fix.generator.FixMessageGenerator;
import org.ib.fix.ingestor.FixFileReader;
import org.ib.fix.ingestor.FileBatchDispatcher;
import org.ib.fix.model.RawFixMessage;
import org.ib.fix.processor.FixMessageEnrichWorker;
import org.ib.fix.execution.FixExecutionHandlerManager;
import quickfix.Message;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class FixMessageProcessingApp {

    private static final BlockingQueue<List<RawFixMessage>> rawMessageQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<List<Message>> enrichedMessageQueue = new LinkedBlockingQueue<>();
    private static final int ENRICHMENT_TIMEOUT_SECONDS = 2;

    public static void main(String[] args) throws InterruptedException, IOException {
        long overallStartTime = System.currentTimeMillis();

        if (args.length > 0 && args[0].equals("--generate-mock")) {
            System.out.println("Generating mock FIX messages...");
            FixMessageGenerator.main(new String[]{Constants.FIX_MESSAGES_FILE_PATH.getName(), "5000"}); // Reuse the generator's main method
            logOverallDuration(overallStartTime);
            System.out.println("Mock FIX messages generated. Exiting.");
            return; // Exit after generating
        }

        // 1. Read and Dispatch Raw FIX Messages
        long batchingStartTime = System.currentTimeMillis();
        List<List<RawFixMessage>> messageBatches = new FixFileReader(Constants.FIX_MESSAGES_FILE_PATH.getName()).readBatches();
        new FileBatchDispatcher(rawMessageQueue).dispatch(messageBatches);
        logStepDuration("Batching", batchingStartTime);

        // 2. Enrich FIX Messages
        long enrichmentStartTime = System.currentTimeMillis();
        ExecutorService enrichmentService = Executors.newSingleThreadExecutor();
        FixMessageEnrichWorker enrichWorker = new FixMessageEnrichWorker(rawMessageQueue, enrichedMessageQueue);
        enrichmentService.submit(enrichWorker);
        logStepDuration("Enrichment started", enrichmentStartTime);

        // 3. Handle Enriched FIX Messages
        long handlingStartTime = System.currentTimeMillis();
        FixExecutionHandlerManager reportManager = new FixExecutionHandlerManager(enrichedMessageQueue);
        reportManager.start();

        // 4. Wait for Enrichment to Complete
        shutdownAndAwaitTermination(enrichmentService, ENRICHMENT_TIMEOUT_SECONDS, "Enrichment worker");

        // 5. Shutdown Report Manager
        reportManager.shutdown();
        logStepDuration("Handling and Writing", handlingStartTime);

        // 6. Generate Final Execution Report
        long reportStartTime = System.currentTimeMillis();
        new FixExecutionReport().generateFinalComparisonReport();
        logStepDuration("Final Execution Report", reportStartTime);

        // Log Overall Execution Time
        logOverallDuration(overallStartTime);
    }

    private static void shutdownAndAwaitTermination(ExecutorService executor, long timeoutSeconds, String serviceName) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
            System.out.println(serviceName + " did not finish in time. Forcing shutdown.");
            executor.shutdownNow();
        }
    }

    private static void logStepDuration(String stepName, long startTime) {
        long endTime = System.currentTimeMillis();
        System.out.println(stepName + " completed in ms: " + (endTime - startTime));
    }

    private static void logOverallDuration(long startTime) {
        long endTime = System.currentTimeMillis();
        System.out.println("Overall execution completed in ms: " + (endTime - startTime));
    }
}

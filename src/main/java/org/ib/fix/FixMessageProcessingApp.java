package org.ib.fix;

import org.ib.fix.ingestor.FixFileReader;
import org.ib.fix.ingestor.FileBatchDispatcher;
import org.ib.fix.model.RawFixMessage;
import org.ib.fix.processor.FixMessageEnrichWorker;
import org.ib.fix.execution.FixMessageReportManager;
import quickfix.Message;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class FixMessageProcessingApp {

    private static final String FIX_FILE_PATH = "fix_messages.txt";
    private static final BlockingQueue<List<RawFixMessage>> inputQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<List<Message>> outputQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws InterruptedException, IOException {

        // Start of overall execution
        long overallStart = System.currentTimeMillis();
        // Step 1: Read and dispatch
        long start = System.currentTimeMillis();
        FixFileReader fileReader = new FixFileReader(FIX_FILE_PATH);
        List<List<RawFixMessage>> batches = fileReader.readBatches();

        FileBatchDispatcher dispatcher = new FileBatchDispatcher(inputQueue);
        dispatcher.dispatch(batches);

        long end = System.currentTimeMillis();
        System.out.println("Batching step completed in ms: " + (end - start)); // Logging batching time

        // Step 2: Start enrichment with proper termination control
        start = System.currentTimeMillis();
        FixMessageEnrichWorker enrichWorker = new FixMessageEnrichWorker(inputQueue, outputQueue);
        ExecutorService enrichmentExecutor = Executors.newSingleThreadExecutor();
        enrichmentExecutor.submit(enrichWorker);

        end = System.currentTimeMillis();
        System.out.println("Enriching step started in ms: " + (end - start)); // Logging enrichment start time

        // Step 3: Start report manager
        start = System.currentTimeMillis();
        FixMessageReportManager reportManager = new FixMessageReportManager(outputQueue);
        reportManager.start();

        // Step 4: Wait for enrichment to finish
        enrichmentExecutor.shutdown();

        // Step 5: Shutdown report manager after enrichment is done
        reportManager.shutdown();
        end = System.currentTimeMillis();
        System.out.println("Writing step completed in ms: " + (end - start)); // Logging writing time

        // Log the total execution time
        long overallEnd = System.currentTimeMillis();
        System.out.println("Overall execution completed in ms: " + (overallEnd - overallStart)); // Logging total execution time
    }
}

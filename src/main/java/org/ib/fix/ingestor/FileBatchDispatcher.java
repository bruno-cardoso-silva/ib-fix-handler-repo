package org.ib.fix.ingestor;

import org.ib.fix.model.RawFixMessage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class FileBatchDispatcher {

    private final BlockingQueue<List<RawFixMessage>> queue;

    public FileBatchDispatcher(BlockingQueue<List<RawFixMessage>> queue) {
        this.queue = queue;
    }

    public void dispatch(List<List<RawFixMessage>> batches) throws InterruptedException {
        for (List<RawFixMessage> batch : batches) {
            putBatchIntoQueue(batch);
        }
        sendPoisonPill(); // signal end of processing
    }

    private void putBatchIntoQueue(List<RawFixMessage> batch) throws InterruptedException {
        queue.put(batch);
    }

    private void sendPoisonPill() throws InterruptedException {
        queue.put(Collections.emptyList());
    }
}

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

    /**
     * Dispatches the list of batches to the queue.
     * This method will block if the queue is full until space becomes available.
     *
     * @param batches the list of batches to be dispatched
     * @throws InterruptedException if interrupted while waiting to put the batch into the queue
     */
    public void dispatch(List<List<RawFixMessage>> batches) throws InterruptedException {
        for (List<RawFixMessage> batch : batches) {
            putBatchIntoQueue(batch);
        }
        sendPoisonPill(); // signal end of processing
    }

    /**
     * Puts a batch into the queue. This will block if the queue is full.
     *
     * @param batch the batch to be put into the queue
     * @throws InterruptedException if interrupted while waiting to put the batch into the queue
     */
    private void putBatchIntoQueue(List<RawFixMessage> batch) throws InterruptedException {
        queue.put(batch);
    }

    /**
     * Sends a poison pill to signal the end of the batch processing.
     * This is an empty list of RawFixMessages.
     *
     * @throws InterruptedException if interrupted while waiting to put the poison pill into the queue
     */
    private void sendPoisonPill() throws InterruptedException {
        queue.put(Collections.emptyList());
    }
}

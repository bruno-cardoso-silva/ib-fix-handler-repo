package org.ib.fix.ingestor;

import org.ib.fix.model.RawFixMessage;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.List;

public class FileBatchDispatcher {

    private final BlockingQueue<List<RawFixMessage>> queue;

    public FileBatchDispatcher(BlockingQueue<List<RawFixMessage>> queue) {
        this.queue = queue;
    }

    public void dispatch(List<List<RawFixMessage>> batches) throws InterruptedException {
        for (List<RawFixMessage> batch : batches) {
            queue.put(batch); // blocks if full
        }
        queue.put(Collections.emptyList()); // poison pill to signal end
    }
}

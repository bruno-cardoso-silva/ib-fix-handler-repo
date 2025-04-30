package org.ib.fix.generator;

import org.ib.fix.ingestor.FileBatchDispatcher;
import org.ib.fix.model.RawFixMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.mockito.Mockito.*;

class FileBatchDispatcherTest {

    private BlockingQueue<List<RawFixMessage>> queue;
    private FileBatchDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        queue = Mockito.mock(BlockingQueue.class);
        dispatcher = new FileBatchDispatcher(queue);
    }

    @Test
    void testDispatch_ShouldPutBatchesIntoQueue() throws InterruptedException {
        List<RawFixMessage> batch1 = new ArrayList<>();
        batch1.add(new RawFixMessage("Test message 1"));
        List<RawFixMessage> batch2 = new ArrayList<>();
        batch2.add(new RawFixMessage("Test message 2"));

        List<List<RawFixMessage>> batches = new ArrayList<>();
        batches.add(batch1);
        batches.add(batch2);

        dispatcher.dispatch(batches);

        // Verify the batches are added to the queue
        verify(queue, times(1)).put(batch1);
        verify(queue, times(1)).put(batch2);
    }

    @Test
    void testDispatch_ShouldSendPoisonPill() throws InterruptedException {
        List<RawFixMessage> batch1 = new ArrayList<>();
        batch1.add(new RawFixMessage("Test message 1"));
        List<List<RawFixMessage>> batches = Collections.emptyList();

        dispatcher.dispatch(batches);

        // Verify the poison pill (empty list) is put into the queue
        verify(queue, times(1)).put(Collections.emptyList());
    }
}

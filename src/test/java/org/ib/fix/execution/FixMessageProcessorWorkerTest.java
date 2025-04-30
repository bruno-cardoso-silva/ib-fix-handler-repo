package org.ib.fix.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.field.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixMessageProcessorWorkerTest {

    private BlockingQueue<List<Message>> messageQueue;
    private ConcurrentLinkedQueue<String> allProcessedLines;
    private ConcurrentLinkedQueue<String> fulfilledOrderLines;
    private FixMessageProcessorWorker worker;

    @BeforeEach
    void setUp() {
        messageQueue = new LinkedBlockingQueue<>();
        allProcessedLines = new ConcurrentLinkedQueue<>();
        fulfilledOrderLines = new ConcurrentLinkedQueue<>();
        worker = new FixMessageProcessorWorker(messageQueue, allProcessedLines, fulfilledOrderLines);
    }

    private Message createFixMessage(String executionType) {
        Message message = new Message();
        message.setField(new SendingTime());
        message.setField(new Account("Acc1"));
        message.setField(new Symbol("Inst1"));
        message.setField(new Side(Side.BUY));
        message.setField(new OrderQty(100));
        message.setField(new LastQty(50));
        message.setField(new CumQty(50));
        message.setField(new AvgPx(10.50));
        message.setField(new ExecType(executionType.charAt(0)));
        return message;
    }

    @Test
    void testRun_partialFill() throws InterruptedException {
        Message message = createFixMessage(String.valueOf(ExecType.PARTIAL_FILL));
        messageQueue.put(Collections.singletonList(message));
        messageQueue.put(Collections.emptyList());

        worker.run();

        assertEquals(1, allProcessedLines.size());
        assertEquals(0, fulfilledOrderLines.size());
    }
}

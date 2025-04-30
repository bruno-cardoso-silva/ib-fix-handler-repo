package org.ib.fix.processor;

import org.ib.fix.model.RawFixMessage;
import quickfix.*;
import quickfix.field.ExecType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class FixMessageEnrichWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FixMessageEnrichWorker.class.getName());
    private static final int BATCH_SIZE = 1000;

    private final BlockingQueue<List<RawFixMessage>> inputQueue;
    private final BlockingQueue<List<Message>> outputQueue;
    private final MessageFactory messageFactory;

    public FixMessageEnrichWorker(
            BlockingQueue<List<RawFixMessage>> inputQueue,
            BlockingQueue<List<Message>> outputQueue) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.messageFactory = new DefaultMessageFactory(); // Injectable for testability if needed
    }

    @Override
    public void run() {
        try {
            while (true) {
                List<RawFixMessage> rawMessages = inputQueue.take();

                if (rawMessages.isEmpty()) {
                    LOGGER.info("Received poison pill. Shutting down worker.");
                    break;
                }

                processBatch(rawMessages);
            }

            outputQueue.put(Collections.emptyList()); // poison pill for report manager

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Worker thread interrupted.");
        } catch (Exception e) {
            LOGGER.severe("Unhandled exception in worker: " + e.getMessage());
        }
    }

    private void processBatch(List<RawFixMessage> rawMessages) throws InterruptedException {
        List<Message> currentBatch = new ArrayList<>(BATCH_SIZE);

        for (RawFixMessage raw : rawMessages) {
            try {
                Message message = parseFixMessage(raw.getFixMessage());

                if (isTradeMessage(message)) {
                    LOGGER.fine("message processed: " + message);
                }

                currentBatch.add(message);

                if (currentBatch.size() == BATCH_SIZE) {
                    outputQueue.put(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to process message: " + raw + ". Error: " + e.getMessage());
                // Depending on use-case: continue, skip or throw
            }
        }

        if (!currentBatch.isEmpty()) { // no messages left behind
            outputQueue.put(currentBatch);
        }
    }

    private Message parseFixMessage(String fixString) throws InvalidMessage {
        return MessageUtils.parse(messageFactory, null, fixString);
    }

    private boolean isTradeMessage(Message msg) throws FieldNotFound {
        return msg.isSetField(ExecType.FIELD) && msg.getChar(ExecType.FIELD) == ExecType.TRADE;
    }
}

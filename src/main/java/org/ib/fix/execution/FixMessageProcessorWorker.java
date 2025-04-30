package org.ib.fix.execution;

import quickfix.Message;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FixMessageProcessorWorker implements Runnable {
    private final BlockingQueue<List<Message>> messageQueue;
    private final ConcurrentLinkedQueue<String> allProcessedLines;
    private final ConcurrentLinkedQueue<String> fulfilledOrderLines;

    public FixMessageProcessorWorker(BlockingQueue<List<Message>> messageQueue, ConcurrentLinkedQueue<String> allProcessedLines, ConcurrentLinkedQueue<String> fulfilledOrderLines) {
        this.messageQueue = messageQueue;
        this.allProcessedLines = allProcessedLines;
        this.fulfilledOrderLines = fulfilledOrderLines;
    }

    @Override
    public void run() {
        try {
            while (true) {
                List<Message> messages = messageQueue.take();

                if (messages.isEmpty()) {
                    break; // Termination signal (poison pill)
                }

                for (Message message : messages) {
                    String rawFixLine = message.toString().replace('\u0001', '|');

                    try {
                        String sendingTime = message.isSetField(52) ? message.getString(52) : "";
                        String account = message.isSetField(1) ? message.getString(1) : "";
                        String symbol = message.isSetField(55) ? message.getString(55) : "";
                        String side = message.isSetField(54) ? message.getString(54) : "";
                        int orderQuantity = message.isSetField(38) ? message.getInt(38) : 0;
                        int lastExecutedQuantity = message.isSetField(32) ? message.getInt(32) : 0;
                        int cumulativeQuantity = message.isSetField(14) ? message.getInt(14) : 0;
                        double averageExecutionPrice = message.isSetField(6) ? message.getDouble(6) : 0.0;
                        String enteringTrader = message.isSetField(528) ? message.getString(528) : ""; // Or another relevant tag

                        double notionalOrderValue = orderQuantity * averageExecutionPrice;
                        double notionalLastExecutionValue = lastExecutedQuantity * averageExecutionPrice;
                        double notionalCumulativeValue = cumulativeQuantity * averageExecutionPrice;

                        String enrichedLine = String.join(";",
                                sendingTime, account, symbol, side,
                                String.valueOf(orderQuantity),
                                String.valueOf(lastExecutedQuantity),
                                String.valueOf(cumulativeQuantity),
                                String.format("%.2f", averageExecutionPrice),
                                String.format("%.2f", notionalOrderValue),
                                String.format("%.2f", notionalLastExecutionValue),
                                String.format("%.2f", notionalCumulativeValue),
                                enteringTrader
                        );
                        allProcessedLines.offer(enrichedLine);

                        if (isOrderFulfilled(message)) {
                            message.setDouble(1010, notionalOrderValue); // Notional order value
                            message.setString(1011, message.getString(5149)); // Entering trade (assuming tag 5149 exists and is relevant)
                            fulfilledOrderLines.offer(rawFixLine);
                        }

                    } catch (Exception e) {
                        System.err.println("Failed to parse or enrich message: " + e.getMessage());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isOrderFulfilled(Message message) {
        try {
            if (message.isSetField(150)) {
                char executionType = message.getChar(150);
                if (executionType == 'F') {
                    return true;
                } else {
                    System.out.println("Execution Type (150) is not 'F', it is: " + executionType);
                    return false;
                }
            } else {
                System.out.println("Execution Type (150) field is not set.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error checking fulfillment status: " + e.getMessage());
            return false;
        }
    }
}
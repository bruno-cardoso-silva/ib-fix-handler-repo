package org.ib.fix.execution;

import quickfix.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FixMessageProcessorWorker implements Runnable {
    private final BlockingQueue<List<Message>> inputQueue;
    private final ConcurrentLinkedQueue<String> allLines;
    private final ConcurrentLinkedQueue<String> fulfilledLines;


    public FixMessageProcessorWorker(BlockingQueue<List<Message>> inputQueue, ConcurrentLinkedQueue<String> allLines, ConcurrentLinkedQueue<String> fulfilledLines) {
        this.inputQueue = inputQueue;
        this.allLines = allLines;
        this.fulfilledLines = fulfilledLines;
    }

    @Override
    public void run() {
        try {
            while (true) {
                List<Message> messages = inputQueue.take();

                if (messages.isEmpty()) {
                    break; // Poison pill
                }


                for (Message msg : messages) {

                    String line = msg.toString().replace('\u0001', '|');

                    try {
                        String horario = msg.isSetField(52) ? msg.getString(52) : ""; // SendingTime
                        String conta = msg.isSetField(1) ? msg.getString(1) : "";     // Account
                        String instrumento = msg.isSetField(55) ? msg.getString(55) : ""; // Symbol
                        String lado = msg.isSetField(54) ? msg.getString(54) : "";    // Side
                        int qtdOrdem = msg.isSetField(38) ? msg.getInt(38) : 0;       // OrderQty
                        int qtdExecAtual = msg.isSetField(32) ? msg.getInt(32) : 0;   // LastQty
                        int qtdExecAcum = msg.isSetField(14) ? msg.getInt(14) : 0;    // CumQty
                        double precoExecutado = msg.isSetField(6) ? msg.getDouble(6) : 0.0; // AvgPx
                        String trader = msg.isSetField(528) ? msg.getString(528) : ""; // Entering Trader (or use another tag if appropriate)

                        double notionalOrdem = qtdOrdem * precoExecutado;
                        double notionalExecAtual = qtdExecAtual * precoExecutado;
                        double notionalExecAcum = qtdExecAcum * precoExecutado;

                        String enrichedLine = String.join(";",
                                horario, conta, instrumento, lado,
                                String.valueOf(qtdOrdem),
                                String.valueOf(qtdExecAtual),
                                String.valueOf(qtdExecAcum),
                                String.format("%.2f", precoExecutado),
                                String.format("%.2f", notionalOrdem),
                                String.format("%.2f", notionalExecAtual),
                                String.format("%.2f", notionalExecAcum),
                                trader
                        );
                        allLines.offer(enrichedLine);
                        if (isFulfilled(msg)) {
                            msg.setDouble(1010,  notionalOrdem); // notional order
                            msg.setString(1011, msg.getString(5149)); //entering trade
                            fulfilledLines.offer(line);
                        }

                    } catch (Exception e) {
                        System.err.println("Failed to parse/enrich message: " + e.getMessage());
                    }

                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isFulfilled(Message msg) {
        try {
            if (msg.isSetField(150)) {
                char execType = msg.getChar(150);
                if (execType == 'F') {
                    return true; // Order is fulfilled
                } else {
                    System.out.println("ExecType is not 'F', it is: " + execType);
                    return false;
                }
            } else {
                System.out.println("ExecType (150) field is not set.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error checking fulfillment status: " + e.getMessage());
            return false;
        }
    }

}

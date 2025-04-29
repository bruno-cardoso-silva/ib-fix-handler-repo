package org.ib.fix.generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

public class FixMessageGenerator {

    private static final int TOTAL_MESSAGES = 5000;
    private static final Random random = new Random();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");
    private static final DecimalFormat decimalFormat = new DecimalFormat("#0.00");

    public static void main(String[] args) throws IOException {
        String outputFile = "fix_messages.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (int i = 0; i < TOTAL_MESSAGES; i++) {
                boolean isFilled = i < 2500;
                String msg = generateFixMessage(i, isFilled);
                writer.write(msg);
                writer.newLine();
            }
        }

        System.out.println("Generated " + TOTAL_MESSAGES + " FIX messages to " + outputFile);
    }

    private static String generateFixMessage(int index, boolean isFilled) {
        StringBuilder sb = new StringBuilder();

        String now = LocalDateTime.now().format(formatter);
        String price = decimalFormat.format(10 + random.nextDouble() * 10); // range: 10–20
        int orderQty = random.nextInt(9000) + 1000; // 1000 - 9999
        int lastQty = isFilled ? orderQty : random.nextInt(orderQty - 1) + 1;
        String account = accounts();
        String symbol = randomSymbol();
        String enteringTrader = enteringTraders(); // tag 5149
        int side = random.nextBoolean() ? 1 : 2; // 1 = Buy, 2 = Sell
        String clOrdId = UUID.randomUUID().toString().substring(0, 10); // tag 11 - unique
        String orderId = UUID.randomUUID().toString().substring(0, 10); // tag 37 - unique

        sb.append("8=FIX.4.4^9=100^35=8"); // FIX header
        sb.append("^34=").append(index);
        sb.append("^49=SYSTEM^52=").append(now).append("^56=DROPCOPY");
        sb.append("^1=").append(account);             // Account
        sb.append("^55=").append(symbol);             // Symbol
        sb.append("^54=").append(side);               // Side
        sb.append("^31=").append(price);              // LastPx
        sb.append("^32=").append(lastQty);            // LastQty
        sb.append("^150=F");                          // ExecType = Fill
        sb.append("^39=").append(isFilled ? 2: 1);    // OrdStatus = Filled
        sb.append("^60=").append(now);                // TransactTime
        sb.append("^5149=").append(enteringTrader);   // entering traders
        sb.append("^11=").append(clOrdId);            //client order id
        sb.append("^37=").append(orderId);            //order id
        sb.append("^38=").append(orderQty);            //orderQty
        sb.append("^14=").append(isFilled? lastQty: lastQty -1);
        sb.append("^6=").append(price);                               //orderQty
        sb.append("^10=000^");                                     //Checksum dummy

        return sb.toString();
    }

    private static String randomSymbol() {
        String[] symbols = {"PETR4", "VALE3", "ITUB4", "BBDC4", "ABEV3", "BBAS3", "MGLU3", "WEGE3", "B3SA3", "LREN3"};
        return symbols[random.nextInt(symbols.length)];
    }

    private static String enteringTraders() {
        String[] traders = {"TRADER1", "TRADER2", "TRADER3", "TRADER4", "TRADER5"};
        return traders[random.nextInt(traders.length)];
    }

    private static String accounts() {
        final String[]  accounts = {"ACC01", "ACC02", "ACC03", "ACC04", "ACC05", "ACC06", "ACC07", "ACC08", "ACC09", "ACC10"};
        return accounts[random.nextInt(accounts.length)];
    }
}

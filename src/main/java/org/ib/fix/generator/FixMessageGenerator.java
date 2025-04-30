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

    private final int totalMessages;
    private final String outputFile;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");
    private static final DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    private static final Random random = new Random();

    public FixMessageGenerator(int totalMessages, String outputFile) {
        this.totalMessages = totalMessages;
        this.outputFile = outputFile;
    }

    public void generateMessages() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (int i = 0; i < totalMessages; i++) {
                boolean isFilled = i < (totalMessages / 2);
                String msg = generateFixMessage(i, isFilled);
                writer.write(msg);
                writer.newLine();
            }
        }
        System.out.println("Generated " + totalMessages + " FIX messages to " + outputFile);
    }

    public String generateFixMessage(int index, boolean isFilled) {
        String now = LocalDateTime.now().format(formatter);
        String price = decimalFormat.format(10 + random.nextDouble() * 10);
        int orderQty = random.nextInt(9000) + 1000;
        int lastQty = isFilled ? orderQty : random.nextInt(orderQty - 1) + 1;
        String account = randomAccount();
        String symbol = randomSymbol();
        String enteringTrader = randomTrader();
        int side = random.nextBoolean() ? 1 : 2;
        String clOrdId = UUID.randomUUID().toString().substring(0, 10);
        String orderId = UUID.randomUUID().toString().substring(0, 10);

        return new StringBuilder()
                .append("8=FIX.4.4^9=100^35=8")
                .append("^34=").append(index)
                .append("^49=SYSTEM^52=").append(now).append("^56=DROPCOPY")
                .append("^1=").append(account)
                .append("^55=").append(symbol)
                .append("^54=").append(side)
                .append("^31=").append(price)
                .append("^32=").append(lastQty)
                .append("^150=F")
                .append("^39=").append(isFilled ? 2 : 1)
                .append("^60=").append(now)
                .append("^5149=").append(enteringTrader)
                .append("^11=").append(clOrdId)
                .append("^37=").append(orderId)
                .append("^38=").append(orderQty)
                .append("^14=").append(isFilled ? lastQty : lastQty - 1)
                .append("^6=").append(price)
                .append("^10=000^")
                .toString();
    }

    private String randomSymbol() {
        String[] symbols = {"PETR4", "VALE3", "ITUB4", "BBDC4", "ABEV3", "BBAS3", "MGLU3", "WEGE3", "B3SA3", "LREN3"};
        return symbols[random.nextInt(symbols.length)];
    }

    private String randomTrader() {
        String[] traders = {"TRADER1", "TRADER2", "TRADER3", "TRADER4", "TRADER5"};
        return traders[random.nextInt(traders.length)];
    }

    private String randomAccount() {
        String[] accounts = {"ACC01", "ACC02", "ACC03", "ACC04", "ACC05", "ACC06", "ACC07", "ACC08", "ACC09", "ACC10"};
        return accounts[random.nextInt(accounts.length)];
    }

    public static void main(String[] args) {
        String file = "fix_messages.txt";
        int total = 5000;

        FixMessageGenerator generator = new FixMessageGenerator(total, file);
        try {
            generator.generateMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

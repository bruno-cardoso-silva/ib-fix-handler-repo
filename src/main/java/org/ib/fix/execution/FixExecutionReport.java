package org.ib.fix.execution;

import org.ib.fix.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FixExecutionReport {

    private static final Logger logger = LoggerFactory.getLogger(FixExecutionReport.class);
    private static final String CSV_SEPARATOR = ";";
    private static final String FIX_FIELD_SEPARATOR = "\\|";
    private static final String FIX_KEY_VALUE_SEPARATOR = "=";
    private static final String FULL_FILL_STATUS = "F";
    private static final String HEADER_DELIMITER = ";;";
    private static final int EXPECTED_CSV_PARTS = 8;
    private static final String HORARIO_HEADER = "Horário";

    private final NumberFormat priceFormatter;

    public FixExecutionReport() {
        this.priceFormatter = NumberFormat.getInstance(Locale.US);
        this.priceFormatter.setMinimumFractionDigits(2);
        this.priceFormatter.setMaximumFractionDigits(2);
    }

    public void generateFinalComparisonReport() throws IOException {
        Path allMessagesPath = Paths.get(Constants.ALL_MESSAGES_FILE_NAME.getName());
        Path fullFillPath = Paths.get(Constants.FULL_FILL_REPORT_FILE_NAME.getName());
        Path executionReportPath = Paths.get(Constants.EXECUTION_REPORT_FILE_NAME.getName());

        Map<AggregationKey, AggregatedStats> allMessagesData = readAndAggregate(allMessagesPath, true);
        Map<AggregationKey, AggregatedStats> fullFillData = readAndAggregate(fullFillPath, false);

        List<String> comparisonReport = compareAggregations(allMessagesData, fullFillData);
        writeToFile(comparisonReport, executionReportPath);
    }

    Map<AggregationKey, AggregatedStats> readAndAggregate(Path filePath, boolean isCsvFormat) throws IOException {
        Map<AggregationKey, AggregatedStats> aggregatedData = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isHeaderOrEmptyLine(line)) {
                    continue;
                }
                parseAndAggregateLine(line, isCsvFormat, aggregatedData);
            }
        }
        return aggregatedData;
    }

    private boolean isHeaderOrEmptyLine(String line) {
        return line.trim().isEmpty() || line.startsWith(HORARIO_HEADER);
    }

    private void parseAndAggregateLine(String line, boolean isCsvFormat, Map<AggregationKey, AggregatedStats> aggregatedData) {
        try {
            if (isCsvFormat) {
                parseCsvLine(line, aggregatedData);
            } else {
                parseFixLine(line, aggregatedData);
            }
        } catch (Exception e) {
            logger.error("Skipping malformed line: {}", line, e);
        }
    }

    void parseCsvLine(String line, Map<AggregationKey, AggregatedStats> aggregatedData) {
        String[] parts = line.split(CSV_SEPARATOR);
        if (parts.length < EXPECTED_CSV_PARTS) {
            logger.warn("Skipping incomplete CSV line: {}", line);
            return;
        }
        try {
            String account = parts[1].trim();
            String instrument = parts[2].trim();
            String side = parts[3].trim();
            double quantity = Double.parseDouble(parts[5].trim());
            double price = Double.parseDouble(parts[7].trim());
            aggregateData(aggregatedData, account, instrument, side, price, quantity);
        } catch (NumberFormatException e) {
            logger.error("Error parsing numeric value in CSV line: {}", line, e);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Error accessing CSV field in line: {}", line, e);
        }
    }

    void parseFixLine(String line, Map<AggregationKey, AggregatedStats> aggregatedData) {
        String[] fields = line.split(FIX_FIELD_SEPARATOR);
        Map<String, String> fieldMap = new HashMap<>();
        for (String field : fields) {
            String[] keyValue = field.split(FIX_KEY_VALUE_SEPARATOR, 2);
            if (keyValue.length == 2) {
                fieldMap.put(keyValue[0], keyValue[1]);
            }
        }

        if (!FULL_FILL_STATUS.equals(fieldMap.get("150"))) {
            return;
        }

        try {
            String account = fieldMap.get("1");
            String instrument = fieldMap.get("55");
            String side = fieldMap.get("54");
            double quantity = Double.parseDouble(fieldMap.get("32"));
            double price = Double.parseDouble(fieldMap.get("31"));
            aggregateData(aggregatedData, account, instrument, side, price, quantity);
        } catch (NumberFormatException e) {
            logger.error("Error parsing numeric value in FIX line: {}", line, e);
        } catch (NullPointerException e) {
            logger.error("Missing FIX field in line: {}", line, e);
        }
    }

    private void aggregateData(Map<AggregationKey, AggregatedStats> aggregatedData, String account, String instrument, String side, double price, double quantity) {
        AggregationKey key = new AggregationKey(account, instrument, side);
        aggregatedData.computeIfAbsent(key, k -> new AggregatedStats()).add(price, quantity);
    }

    List<String> compareAggregations(Map<AggregationKey, AggregatedStats> allMessages,
                                     Map<AggregationKey, AggregatedStats> fullFill) {
        List<String> reportLines = new ArrayList<>();
        reportLines.add("AccountTxt;InstrumentTxt;SideTxt;AveragePriceTxt" + HEADER_DELIMITER + "AccountCsv;InstrumentCsv;SideCsv;AveragePriceCsv");

        Set<AggregationKey> allKeys = new HashSet<>();
        allKeys.addAll(allMessages.keySet());
        allKeys.addAll(fullFill.keySet());

        for (AggregationKey key : allKeys) {
            AggregatedStats allStats = allMessages.getOrDefault(key, new AggregatedStats());
            AggregatedStats fullStats = fullFill.getOrDefault(key, new AggregatedStats());

            reportLines.add(String.join(CSV_SEPARATOR,
                    key.account, key.instrument, key.side, formatPrice(fullStats.getAverage()),
                    "", // Delimiter
                    key.account, key.instrument, key.side, formatPrice(allStats.getAverage())
            ));
        }
        return reportLines;
    }

    private String formatPrice(double price) {
        return priceFormatter.format(price);
    }

    private void writeToFile(List<String> lines, Path outputPath) throws IOException {
        Files.write(outputPath, lines);
    }

    public static class AggregationKey {
        private final String account;
        private final String instrument;
        private final String side;

        public AggregationKey(String account, String instrument, String side) {
            this.account = account;
            this.instrument = instrument;
            this.side = side;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AggregationKey that = (AggregationKey) o;
            return Objects.equals(account, that.account) &&
                    Objects.equals(instrument, that.instrument) &&
                    Objects.equals(side, that.side);
        }

        @Override
        public int hashCode() {
            return Objects.hash(account, instrument, side);
        }
    }

    public static class AggregatedStats {
        private double sumPriceQuantity = 0.0;
        private double totalQuantity = 0.0;

        void add(double price, double quantity) {
            sumPriceQuantity += price * quantity;
            totalQuantity += quantity;
        }

        double getAverage() {
            return totalQuantity == 0.0 ? 0.0 : sumPriceQuantity / totalQuantity;
        }
    }
}
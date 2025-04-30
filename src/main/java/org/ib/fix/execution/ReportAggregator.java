package org.ib.fix.execution;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReportAggregator {

    public void generateFinalComparisonReport() throws IOException {
        Map<AggregationKey, AggregatedStats> allMsgsData = readAndAggregate("AllMsgs.csv", true);
        Map<AggregationKey, AggregatedStats> fullFillData = readAndAggregate("FullFill.txt", false);

        List<String> comparison = compareAggregations(allMsgsData, fullFillData);
        writeToFile(comparison, "FinalReport.csv");
    }

    private Map<AggregationKey, AggregatedStats> readAndAggregate(String filePath, boolean isCsvFormat) throws IOException {
        Map<AggregationKey, AggregatedStats> result = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("Horário")) continue;

                if (isCsvFormat) {
                    String[] parts = line.split(";");
                    if (parts.length < 8) continue;
                    try {
                        String account = parts[1].trim();
                        String instrument = parts[2].trim();
                        String side = parts[3].trim();
                        double qty = Double.parseDouble(parts[5].trim());
                        double price = Double.parseDouble(parts[7].trim());

                        AggregationKey key = new AggregationKey(account, instrument, side);
                        result.computeIfAbsent(key, k -> new AggregatedStats()).add(price, qty);
                    } catch (Exception e) {
                        System.err.println("Skipping malformed CSV line: " + line);
                    }
                } else {
                    String[] fields = line.split("\\|");
                    Map<String, String> fieldMap = new HashMap<>();
                    for (String field : fields) {
                        String[] keyValue = field.split("=", 2);
                        if (keyValue.length == 2) {
                            fieldMap.put(keyValue[0], keyValue[1]);
                        }
                    }

                    if (!"F".equals(fieldMap.get("150"))) continue;

                    try {
                        String account = fieldMap.get("1");
                        String instrument = fieldMap.get("55");
                        String side = fieldMap.get("54");
                        double qty = Double.parseDouble(fieldMap.get("32"));
                        double price = Double.parseDouble(fieldMap.get("31"));
                        AggregationKey key = new AggregationKey(account, instrument, side);
                        result.computeIfAbsent(key, k -> new AggregatedStats()).add(price, qty);
                    } catch (Exception e) {
                        System.err.println("Skipping malformed FIX line: " + line);
                    }
                }
            }
        }

        return result;
    }

    private List<String> compareAggregations(Map<AggregationKey, AggregatedStats> allMsgs,
                                             Map<AggregationKey, AggregatedStats> fullFill) {
        List<String> output = new ArrayList<>();
        output.add("TxtConta;TxtPapel;TxtLado;TxtPrecoMedio;;CsvConta;CsvPapel;CsvLado;CsvPrecoMedio");

        Set<AggregationKey> allKeys = new HashSet<>();
        allKeys.addAll(allMsgs.keySet());
        allKeys.addAll(fullFill.keySet());

        for (AggregationKey key : allKeys) {
            AggregatedStats allStats = allMsgs.getOrDefault(key, new AggregatedStats());
            AggregatedStats fullStats = fullFill.getOrDefault(key, new AggregatedStats());

            output.add(String.join(";",
                    key.conta, key.instrumento, key.lado,
                    String.format(Locale.US, "%.2f", fullStats.getAverage()),
                    "",
                    key.conta, key.instrumento, key.lado,
                    String.format(Locale.US, "%.2f", allStats.getAverage())
            ));
        }

        return output;
    }

    private void writeToFile(List<String> lines, String outputPath) throws IOException {
        Files.write(Paths.get(outputPath), lines);
    }

    public static class AggregationKey {
        String conta;
        String instrumento;
        String lado;

        public AggregationKey(String conta, String instrumento, String lado) {
            this.conta = conta;
            this.instrumento = instrumento;
            this.lado = lado;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AggregationKey that = (AggregationKey) o;
            return Objects.equals(conta, that.conta) &&
                    Objects.equals(instrumento, that.instrumento) &&
                    Objects.equals(lado, that.lado);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conta, instrumento, lado);
        }
    }

    public static class AggregatedStats {
        double sumPriceQty = 0.0;
        double sumQty = 0.0;

        void add(double price, double qty) {
            sumPriceQty += price * qty;
            sumQty += qty;
        }

        double getAverage() {
            return sumQty == 0.0 ? 0.0 : sumPriceQty / sumQty;
        }
    }

    public static void main(String[] args) throws IOException {
        ReportAggregator aggregator = new ReportAggregator();
        aggregator.generateFinalComparisonReport();
        System.out.println("Final report generated: FinalReport.csv");
    }
}

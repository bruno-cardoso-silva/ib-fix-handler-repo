package org.ib.fix.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixExecutionReportTest {

    private FixExecutionReport fixExecutionReport;

    @TempDir
    Path tempDir;

    private Path allMessagesFile;

    @BeforeEach
    void setUp() throws IOException {
        fixExecutionReport = new FixExecutionReport();
        allMessagesFile = tempDir.resolve("all_messages.csv");
        Files.createFile(allMessagesFile);
    }

    private void writeToFile(Path path, List<String> lines) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    @Test
    void testReadAndAggregateCsv() throws IOException {
        List<String> csvData = new ArrayList<>();
        csvData.add("Horário;Conta;Instrumento;Lado;Tipo;QtdOrdem;Preço;PreçoExecutado");
        csvData.add("09:30:00;Acc1;Inst1;Compra;Market;100;10.00;10.50");
        writeToFile(allMessagesFile, csvData);

        Map<FixExecutionReport.AggregationKey, FixExecutionReport.AggregatedStats> result =
                fixExecutionReport.readAndAggregate(allMessagesFile, true);

        assertEquals(1, result.size());
        FixExecutionReport.AggregationKey key = new FixExecutionReport.AggregationKey("Acc1", "Inst1", "Compra");
        assertTrue(result.containsKey(key));
        assertEquals(10.50, result.get(key).getAverage(), 0.001);
    }

    @Test
    void testParseCsvLine_incompleteInput() {
        Map<FixExecutionReport.AggregationKey, FixExecutionReport.AggregatedStats> aggregatedData = new HashMap<>();
        String csvLine = "09:30:00;Acc1;Inst1;Compra;Market;100;10.00";
        fixExecutionReport.parseCsvLine(csvLine, aggregatedData);
        assertTrue(aggregatedData.isEmpty());
    }
}

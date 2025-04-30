package org.ib.fix.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class FixMessageGeneratorTest {

    private FixMessageGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new FixMessageGenerator(10, "test_fix_messages.txt");
    }

    @Test
    void testGenerateFixMessage_containsExpectedTags() {
        String msg = generator.generateFixMessage(1, true);

        assertThat(msg).contains("8=FIX.4.4");
        assertThat(msg).contains("^35=8");
        assertThat(msg).contains("^150=F"); // ExecType=Fill
        assertThat(msg).contains("^11=");   // ClOrdID
        assertThat(msg).contains("^37=");   // OrderID
        assertThat(msg).contains("^38=");   // OrderQty
    }

    @Test
    void testGenerateMessages_createsCorrectNumberOfLines() throws IOException {
        generator.generateMessages();

        try (BufferedReader reader = new BufferedReader(new FileReader("test_fix_messages.txt"))) {
            List<String> lines = reader.lines().collect(Collectors.toList());

            assertThat(lines).hasSize(10);
            assertThat(lines.get(0)).startsWith("8=FIX.4.4");
        }
    }

    @Test
    void testMessageFormat_shouldEndWithChecksumAndCaret() {
        String message = generator.generateFixMessage(0, true);

        assertThat(message).endsWith("^10=000^");
    }

    @Test
    void testHalfFilledAndHalfPartialLogic() {
        int total = 100;
        FixMessageGenerator localGenerator = new FixMessageGenerator(total, "dummy.txt");

        int filledCount = 0;
        int partialCount = 0;

        for (int i = 0; i < total; i++) {
            boolean isFilled = i < (total / 2);
            String message = localGenerator.generateFixMessage(i, isFilled);

            if (message.contains("^39=2^")) {
                filledCount++;
            } else if (message.contains("^39=1^")) {
                partialCount++;
            }
        }

        assertThat(filledCount).isEqualTo(50);
        assertThat(partialCount).isEqualTo(50);
    }
}

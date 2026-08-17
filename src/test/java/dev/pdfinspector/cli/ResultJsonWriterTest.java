package dev.pdfinspector.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.pdfinspector.PdfInspector;
import dev.pdfinspector.model.PdfProcessResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class ResultJsonWriterTest {
    @Test
    void resultAndItemJsonKeepTheirContractFields() throws Exception {
        Path fixture = Paths.get("src", "test", "resources", "fixtures", "thermo-freon12.pdf")
                .toAbsolutePath().normalize();
        PdfProcessResult result = PdfInspector.process(fixture);
        ResultJsonWriter writer = new ResultJsonWriter();

        String resultJson = writer.write(result);
        String itemJson = writer.writeItems(result);

        assertTrue(resultJson.startsWith("{\"pdf_type\":"));
        assertTrue(resultJson.contains("\"pages_needing_ocr\":"));
        assertTrue(resultJson.contains("\"has_encoding_issues\":"));
        assertTrue(itemJson.startsWith("{\"items\":"));
        assertTrue(itemJson.contains("\"unresolved_encoding\":"));
    }
}

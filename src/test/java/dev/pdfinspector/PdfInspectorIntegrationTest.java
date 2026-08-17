package dev.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.pdfinspector.model.MarkdownOptions;
import dev.pdfinspector.model.PdfOptions;
import dev.pdfinspector.model.PdfProcessResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PdfInspectorIntegrationTest {
    @Test
    void processesExistingTextFixtureThroughOnePublicApi() throws IOException {
        Path fixture = fixture("thermo-freon12.pdf");

        PdfProcessResult result = PdfInspector.process(fixture);

        assertTrue(result.getPageCount() > 0);
        assertNotNull(result.getMarkdown());
        assertFalse(result.getMarkdown().trim().isEmpty());
        assertFalse(result.getPageExtractions().isEmpty());
        assertTrue(result.getConfidence() >= 0.50d);
    }

    @Test
    void respectsOneBasedPageFilterAndAddsPageMarkers() throws IOException {
        PdfOptions options = PdfOptions.builder()
                .selectedPages(Collections.singleton(Integer.valueOf(1)))
                .markdownOptions(MarkdownOptions.builder().includePageNumbers(true).build())
                .build();

        PdfProcessResult result = PdfInspector.process(fixture("thermo-freon12.pdf"), options);

        assertEquals(1, result.getPageExtractions().size());
        assertEquals(1, result.getPageExtractions().get(0).getPage());
        assertTrue(result.getMarkdown().startsWith("<!-- Page 1 -->"));
    }

    @Test
    void acceptsPdfBytesThroughTheSamePipeline() throws IOException {
        byte[] bytes = Files.readAllBytes(fixture("thermo-freon12.pdf"));

        PdfProcessResult result = PdfInspector.detect(bytes);

        assertTrue(result.getPagesSampled() > 0);
        assertTrue(result.getPagesWithText() > 0);
        assertNull(result.getMarkdown());
    }

    private Path fixture(String name) {
        Path fixture = Paths.get("src", "test", "resources", "fixtures", name).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(fixture), "missing test fixture: " + fixture);
        return fixture;
    }
}

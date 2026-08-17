package dev.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.pdfinspector.model.PdfOptions;
import dev.pdfinspector.model.PdfProcessResult;
import dev.pdfinspector.model.TextItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/** Fixtures that pin the first Java port's decode and OCR contract. */
class PdfInspectorFixtureContractTest {
    @Test
    void identityHWithoutUsableMappingUsesReplacementAndRoutesToOcr() throws IOException {
        PdfProcessResult result = PdfInspector.process(fixture("shinagawa_identity_h.pdf"));

        boolean hasReplacement = false;
        for (TextItem item : result.getPageExtractions().get(0).getItems()) {
            hasReplacement = hasReplacement || item.getText().indexOf('\uFFFD') >= 0;
            for (int index = 0; index < item.getText().length(); index++) {
                char character = item.getText().charAt(index);
                assertFalse(character >= 0x80 && character <= 0xFF,
                        "Identity-H fallback must not leak Latin-1 mojibake");
            }
        }

        assertTrue(hasReplacement, "unresolvable CID glyphs must become U+FFFD");
        assertTrue(result.getPagesNeedingOcr().contains(Integer.valueOf(1)));
        assertTrue(result.hasEncodingIssues());
        assertTrue(result.getMarkdown().isEmpty());
    }

    @Test
    void shiftedToUnicodeCipherRoutesToOcrAndSuppressesMarkdown() throws IOException {
        PdfProcessResult result = PdfInspector.process(fixture("shifted_cipher_tounicode.pdf"));

        assertTrue(result.getPagesNeedingOcr().contains(Integer.valueOf(1)));
        assertTrue(result.hasEncodingIssues());
        assertTrue(result.getMarkdown().isEmpty());
    }

    @Test
    void encryptedFixtureRejectsMissingOrWrongPasswordsAndAcceptsTheRightOne() throws IOException {
        Path encrypted = fixture("encrypted-secret123.pdf");

        assertThrows(IOException.class, () -> PdfInspector.process(encrypted));
        assertThrows(IOException.class, () -> PdfInspector.process(encrypted,
                PdfOptions.builder().password("wrong").build()));

        PdfProcessResult decrypted = PdfInspector.process(encrypted,
                PdfOptions.builder().password("secret123").build());
        assertTrue(decrypted.getMarkdown().contains("Procurement"));
    }

    @Test
    void richTextOverWatermarkStaysExtractable() throws IOException {
        PdfProcessResult result = PdfInspector.process(fixture("text_page_with_watermark_image.pdf"));

        assertFalse(result.getPagesNeedingOcr().contains(Integer.valueOf(1)));
        assertTrue(result.getMarkdown().toLowerCase(java.util.Locale.ROOT).contains("watermark"));
    }

    @Test
    void vectorAndScanFixturesSuppressUntrustedNativeText() throws IOException {
        assertOcrOnly("vector_outlined_text_with_caption.pdf");
        assertOcrOnly("scan_with_native_header_text.pdf");
    }

    private void assertOcrOnly(String fixtureName) throws IOException {
        PdfProcessResult result = PdfInspector.process(fixture(fixtureName));
        assertTrue(result.getPagesNeedingOcr().contains(Integer.valueOf(1)), fixtureName);
        assertTrue(result.getMarkdown().isEmpty(), fixtureName);
    }

    private Path fixture(String name) {
        Path fixture = Paths.get("src", "test", "resources", "fixtures", name).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(fixture), "missing test fixture: " + fixture);
        return fixture;
    }
}

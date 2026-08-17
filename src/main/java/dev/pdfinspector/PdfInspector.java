package dev.pdfinspector;

import dev.pdfinspector.detector.DetectionResult;
import dev.pdfinspector.detector.PdfTypeDetector;
import dev.pdfinspector.extractor.PdfTextExtractor;
import dev.pdfinspector.markdown.LayoutAnalyzer;
import dev.pdfinspector.markdown.MarkdownRenderer;
import dev.pdfinspector.markdown.TextLineAssembler;
import dev.pdfinspector.model.LayoutComplexity;
import dev.pdfinspector.model.PageExtraction;
import dev.pdfinspector.model.PdfOptions;
import dev.pdfinspector.model.PdfProcessResult;
import dev.pdfinspector.model.ProcessMode;
import dev.pdfinspector.tables.TableDetector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

/** Public facade for detect → extract → Markdown processing. */
public final class PdfInspector {
    private PdfInspector() {
    }

    public static PdfProcessResult process(Path path) throws IOException {
        return process(path, PdfOptions.defaults());
    }

    public static PdfProcessResult detect(Path path) throws IOException {
        return process(path, PdfOptions.detectOnly());
    }

    public static PdfProcessResult process(byte[] pdfBytes) throws IOException {
        return process(pdfBytes, PdfOptions.defaults());
    }

    public static PdfProcessResult detect(byte[] pdfBytes) throws IOException {
        return process(pdfBytes, PdfOptions.detectOnly());
    }

    public static PdfProcessResult process(Path path, PdfOptions options) throws IOException {
        RuntimeRequirements.requireJdk17OrNewer();
        validatePath(path);
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        PDDocument document = options.getPassword() == null
                ? Loader.loadPDF(path.toFile())
                : Loader.loadPDF(path.toFile(), options.getPassword());
        try {
            return processLoadedDocument(document, options);
        } finally {
            document.close();
        }
    }

    public static PdfProcessResult process(byte[] pdfBytes, PdfOptions options) throws IOException {
        RuntimeRequirements.requireJdk17OrNewer();
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes must not be empty");
        }
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        PDDocument document = options.getPassword() == null
                ? Loader.loadPDF(pdfBytes)
                : Loader.loadPDF(pdfBytes, options.getPassword());
        try {
            return processLoadedDocument(document, options);
        } finally {
            document.close();
        }
    }

    private static PdfProcessResult processLoadedDocument(PDDocument document, PdfOptions options) throws IOException {
        long startedAt = System.nanoTime();
        TextLineAssembler lineAssembler = new TextLineAssembler();
        PdfTextExtractor textExtractor = new PdfTextExtractor(lineAssembler);
        List<PageExtraction> pages = textExtractor.extract(document, options.getSelectedPages());
        PdfTypeDetector detector = new PdfTypeDetector();
        DetectionResult detection = detector.detect(document, options.getDetectionConfig(), options.getSelectedPages(), pages);
        LayoutComplexity layout = LayoutComplexity.simple();
        String markdown = null;
        if (options.getMode() != ProcessMode.DETECT_ONLY) {
            layout = new LayoutAnalyzer(new TableDetector()).analyze(pages);
            if (options.getMode() == ProcessMode.FULL) {
                markdown = new MarkdownRenderer().render(pages, options.getMarkdownOptions(),
                        new java.util.HashSet<Integer>(detection.getPagesNeedingOcr()));
            }
        }

        String title = document.getDocumentInformation() == null
                ? null : document.getDocumentInformation().getTitle();
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        return new PdfProcessResult(
                detection.getPdfType(), markdown, document.getNumberOfPages(), detection.getPagesSampled(),
                detection.getPagesWithText(), elapsedMs,
                detection.getPagesNeedingOcr(), detection.getOcrReasonsByPage(), title,
                detection.getConfidence(), layout, detection.hasEncodingIssues(), pages);
    }

    private static void validatePath(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("PDF file does not exist or is not a regular file: " + path);
        }
    }
}

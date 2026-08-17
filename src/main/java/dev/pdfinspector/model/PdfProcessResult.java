package dev.pdfinspector.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result returned by the public PDF Inspector API. */
public record PdfProcessResult(PdfType pdfType, String markdown, int pageCount, int pagesSampled,
                               int pagesWithText, long processingTimeMs, List<Integer> pagesNeedingOcr,
                               List<PageOcrReasons> ocrReasonsByPage, String title, double confidence,
                               LayoutComplexity layout, boolean hasEncodingIssues,
                               List<PageExtraction> pageExtractions) {
    public PdfProcessResult {
        pagesNeedingOcr = Collections.unmodifiableList(new ArrayList<Integer>(pagesNeedingOcr));
        ocrReasonsByPage = Collections.unmodifiableList(new ArrayList<PageOcrReasons>(ocrReasonsByPage));
        pageExtractions = Collections.unmodifiableList(new ArrayList<PageExtraction>(pageExtractions));
    }

    public PdfType getPdfType() { return pdfType; }
    public String getMarkdown() { return markdown; }
    public int getPageCount() { return pageCount; }
    public int getPagesSampled() { return pagesSampled; }
    public int getPagesWithText() { return pagesWithText; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    public List<Integer> getPagesNeedingOcr() { return pagesNeedingOcr; }
    public List<PageOcrReasons> getOcrReasonsByPage() { return ocrReasonsByPage; }
    public String getTitle() { return title; }
    public double getConfidence() { return confidence; }
    public LayoutComplexity getLayout() { return layout; }
    public List<PageExtraction> getPageExtractions() { return pageExtractions; }
}

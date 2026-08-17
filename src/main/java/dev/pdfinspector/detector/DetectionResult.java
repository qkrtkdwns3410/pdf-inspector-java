package dev.pdfinspector.detector;

import dev.pdfinspector.model.PageOcrReasons;
import dev.pdfinspector.model.PdfType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Detailed output of the lightweight detector. */
public record DetectionResult(PdfType pdfType, int pagesSampled, int pagesWithText, double confidence,
                              List<Integer> pagesNeedingOcr, List<PageOcrReasons> ocrReasonsByPage,
                              boolean hasEncodingIssues) {
    public DetectionResult {
        pagesNeedingOcr = Collections.unmodifiableList(new ArrayList<Integer>(pagesNeedingOcr));
        ocrReasonsByPage = Collections.unmodifiableList(new ArrayList<PageOcrReasons>(ocrReasonsByPage));
    }

    public PdfType getPdfType() { return pdfType; }
    public int getPagesSampled() { return pagesSampled; }
    public int getPagesWithText() { return pagesWithText; }
    public double getConfidence() { return confidence; }
    public List<Integer> getPagesNeedingOcr() { return pagesNeedingOcr; }
    public List<PageOcrReasons> getOcrReasonsByPage() { return ocrReasonsByPage; }
}

package dev.pdfinspector.detector;

import dev.pdfinspector.model.DetectionConfig;
import dev.pdfinspector.model.PageExtraction;
import dev.pdfinspector.model.OcrReason;
import dev.pdfinspector.model.PageOcrReasons;
import dev.pdfinspector.model.PdfType;
import dev.pdfinspector.model.ScanStrategy;
import dev.pdfinspector.model.TextItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Fast OCR-routing classifier. It combines actual text positions with raster
 * image resource evidence and deliberately does not render any page.
 */
public final class PdfTypeDetector {
    private static final int MIN_USABLE_TEXT_CHARACTERS = 12;

    private final PageResourceInspector resourceInspector = new PageResourceInspector();

    public DetectionResult detect(PDDocument document, DetectionConfig config, Set<Integer> selectedPages,
                                  List<PageExtraction> extractedPages) throws IOException {
        java.util.Map<Integer, PageExtraction> pagesByNumber = new java.util.HashMap<Integer, PageExtraction>();
        for (PageExtraction page : extractedPages) {
            pagesByNumber.put(Integer.valueOf(page.getPage()), page);
        }
        List<Integer> candidates = candidatePages(document.getNumberOfPages(), selectedPages);
        List<Integer> sampledPages = samplePages(candidates, config);
        List<PageEvidence> evidence = new ArrayList<PageEvidence>();
        for (Integer pageNumber : sampledPages) {
            PageExtraction extracted = pagesByNumber.get(pageNumber);
            List<TextItem> textItems = extracted == null ? Collections.<TextItem>emptyList() : extracted.getItems();
            PageResourceInspector.ImageStats imageStats = resourceInspector.inspect(
                    document.getPage(pageNumber.intValue() - 1));
            evidence.add(PageEvidence.from(pageNumber.intValue(), textItems, imageStats,
                    config.getScannedImagePixelThreshold()));
        }
        return summarize(evidence, pagesByNumber);
    }

    private List<Integer> candidatePages(int pageCount, Set<Integer> selectedPages) {
        List<Integer> candidates = new ArrayList<Integer>();
        for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            if (selectedPages.isEmpty() || selectedPages.contains(Integer.valueOf(pageNumber))) {
                candidates.add(Integer.valueOf(pageNumber));
            }
        }
        return candidates;
    }

    private List<Integer> samplePages(List<Integer> candidates, DetectionConfig config) {
        if (config.getScanStrategy() == ScanStrategy.ALL || candidates.size() <= config.getSampleSize()) {
            return candidates;
        }
        if (config.getSampleSize() == 1) {
            return Collections.singletonList(candidates.get(0));
        }
        LinkedHashSet<Integer> sampled = new LinkedHashSet<Integer>();
        int last = candidates.size() - 1;
        int requested = config.getSampleSize();
        for (int index = 0; index < requested; index++) {
            int candidateIndex = Math.round((float) index * last / (requested - 1));
            sampled.add(candidates.get(candidateIndex));
        }
        return new ArrayList<Integer>(sampled);
    }

    private DetectionResult summarize(List<PageEvidence> evidence, java.util.Map<Integer, PageExtraction> pagesByNumber) {
        if (evidence.isEmpty()) {
            return new DetectionResult(PdfType.IMAGE_BASED, 0, 0, 0.0d,
                    Collections.<Integer>emptyList(), Collections.<PageOcrReasons>emptyList(), false);
        }
        int usableTextPages = 0;
        int scannedPages = 0;
        int imagePages = 0;
        boolean encodingIssues = false;
        List<Integer> pagesNeedingOcr = new ArrayList<Integer>();
        List<PageOcrReasons> ocrReasonEntries = new ArrayList<PageOcrReasons>();
        java.util.Map<Integer, List<String>> reasonNamesByPage = new java.util.TreeMap<Integer, List<String>>();
        for (PageEvidence page : evidence) {
            if (page.hasUsableText()) {
                usableTextPages++;
            }
            if (page.hasScannedImage()) {
                scannedPages++;
            }
            if (page.imageCount() > 0) {
                imagePages++;
            }
            encodingIssues = encodingIssues || page.garbledText();
            List<String> reasons = page.ocrReasons();
            if (!reasons.isEmpty()) {
                reasonNamesByPage.put(Integer.valueOf(page.page()), reasons);
            }
        }

        for (PageExtraction page : pagesByNumber.values()) {
            Integer pageNumber = Integer.valueOf(page.getPage());
            if (TextQualityAnalyzer.hasEncodingIssue(page.getItems())) {
                encodingIssues = true;
                reasonNamesByPage.put(pageNumber, Collections.singletonList(
                        OcrReason.SUSPECTED_GARBLED_TEXT.getWireName()));
            }
        }

        PdfType type;
        if (usableTextPages == evidence.size() && !encodingIssues) {
            type = PdfType.TEXT_BASED;
        } else if (usableTextPages == 0 && scannedPages == evidence.size()) {
            type = PdfType.SCANNED;
        } else if (usableTextPages == 0) {
            type = PdfType.IMAGE_BASED;
        } else {
            type = PdfType.MIXED;
        }
        for (java.util.Map.Entry<Integer, List<String>> entry : reasonNamesByPage.entrySet()) {
            pagesNeedingOcr.add(entry.getKey());
            ocrReasonEntries.add(new PageOcrReasons(entry.getKey().intValue(), entry.getValue()));
        }
        int dominantPages = Math.max(usableTextPages, Math.max(scannedPages, imagePages));
        double confidence = Math.max(0.50d, (double) dominantPages / evidence.size());
        return new DetectionResult(type, evidence.size(), usableTextPages, confidence,
                pagesNeedingOcr, ocrReasonEntries, encodingIssues);
    }

    private record PageEvidence(int page, boolean hasUsableText, boolean garbledText, int imageCount,
                                boolean hasScannedImage, boolean hasVectorText) {

        static PageEvidence from(int page, List<TextItem> items, PageResourceInspector.ImageStats imageStats,
                                 long scannedThreshold) {
            int visibleCharacters = 0;
            boolean garbled = TextQualityAnalyzer.hasEncodingIssue(items);
            for (TextItem item : items) {
                String text = item.getText();
                for (int offset = 0; offset < text.length(); offset++) {
                    char character = text.charAt(offset);
                    if (!Character.isWhitespace(character)) {
                        visibleCharacters++;
                    }
                }
            }
            boolean imageDominated = visibleCharacters < 60 && (imageStats.getLargestImagePixels() >= scannedThreshold
                    || imageStats.getImageCount() == 1);
            boolean vectorText = imageStats.hasVectorText();
            boolean usable = visibleCharacters >= MIN_USABLE_TEXT_CHARACTERS && !garbled && !imageDominated
                    && !vectorText;
            return new PageEvidence(page, usable, garbled, imageStats.getImageCount(),
                    imageDominated, vectorText);
        }

        List<String> ocrReasons() {
            List<String> reasons = new ArrayList<String>();
            if (garbledText) {
                reasons.add(OcrReason.SUSPECTED_GARBLED_TEXT.getWireName());
            }
            if (!hasUsableText) {
                if (hasVectorText) {
                    reasons.add(OcrReason.VECTOR_TEXT.getWireName());
                } else if (hasScannedImage) {
                    reasons.add(OcrReason.SCANNED.getWireName());
                } else if (imageCount == 0) {
                    reasons.add(OcrReason.VECTOR_TEXT.getWireName());
                } else {
                    reasons.add(OcrReason.NO_TEXT.getWireName());
                }
            }
            return reasons;
        }
    }
}

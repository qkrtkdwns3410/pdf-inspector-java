package dev.pdfinspector.extractor;

import dev.pdfinspector.markdown.TextLineAssembler;
import dev.pdfinspector.model.PageExtraction;
import dev.pdfinspector.model.TextItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;

/** Extracts all requested pages while the caller owns the open PDDocument. */
public final class PdfTextExtractor {
    private final TextLineAssembler lineAssembler;

    public PdfTextExtractor(TextLineAssembler lineAssembler) {
        this.lineAssembler = lineAssembler;
    }

    public List<PageExtraction> extract(PDDocument document, Set<Integer> selectedPages) throws IOException {
        PdfBoxContentStreamExtractor stripper = new PdfBoxContentStreamExtractor();
        List<PageExtraction> pages = new ArrayList<PageExtraction>();
        for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
            if (!selectedPages.isEmpty() && !selectedPages.contains(Integer.valueOf(pageNumber))) {
                continue;
            }
            List<TextItem> items = stripper.extractPage(document, pageNumber);
            pages.add(new PageExtraction(pageNumber, items, lineAssembler.assemble(items)));
        }
        return pages;
    }
}

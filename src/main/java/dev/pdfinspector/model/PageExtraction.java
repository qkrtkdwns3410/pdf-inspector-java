package dev.pdfinspector.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Positioned text and visual lines for one extracted page. */
public record PageExtraction(int page, List<TextItem> items, List<TextLine> lines) {
    public PageExtraction {
        items = Collections.unmodifiableList(new ArrayList<TextItem>(items));
        lines = Collections.unmodifiableList(new ArrayList<TextLine>(lines));
    }

    public int getPage() { return page; }
    public List<TextItem> getItems() { return items; }
    public List<TextLine> getLines() { return lines; }
}

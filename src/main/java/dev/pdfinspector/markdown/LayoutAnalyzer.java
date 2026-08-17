package dev.pdfinspector.markdown;

import dev.pdfinspector.model.LayoutComplexity;
import dev.pdfinspector.model.PageExtraction;
import dev.pdfinspector.model.TextLine;
import dev.pdfinspector.tables.TableDetector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Collects non-destructive layout signals for callers and future renderers. */
public final class LayoutAnalyzer {
    private final TableDetector tableDetector;

    public LayoutAnalyzer(TableDetector tableDetector) {
        this.tableDetector = tableDetector;
    }

    public LayoutComplexity analyze(List<PageExtraction> pages) {
        List<Integer> tablePages = new ArrayList<Integer>();
        List<Integer> columnPages = new ArrayList<Integer>();
        for (PageExtraction page : pages) {
            if (tableDetector.hasLikelyTable(page.getLines())) {
                tablePages.add(Integer.valueOf(page.getPage()));
            }
            if (hasLikelyColumns(page.getLines())) {
                columnPages.add(Integer.valueOf(page.getPage()));
            }
        }
        return new LayoutComplexity(!tablePages.isEmpty() || !columnPages.isEmpty(), tablePages, columnPages);
    }

    private boolean hasLikelyColumns(List<TextLine> lines) {
        if (lines.size() < 10) {
            return false;
        }
        Map<Integer, Integer> starts = new HashMap<Integer, Integer>();
        for (TextLine line : lines) {
            int bucket = Math.round(line.getX() / 24.0f);
            Integer count = starts.get(Integer.valueOf(bucket));
            starts.put(Integer.valueOf(bucket), Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        List<Integer> repeatedStarts = new ArrayList<Integer>();
        for (Map.Entry<Integer, Integer> entry : starts.entrySet()) {
            if (entry.getValue().intValue() >= 4) {
                repeatedStarts.add(entry.getKey());
            }
        }
        Collections.sort(repeatedStarts, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return left.compareTo(right);
            }
        });
        for (int index = 1; index < repeatedStarts.size(); index++) {
            if ((repeatedStarts.get(index).intValue() - repeatedStarts.get(index - 1).intValue()) * 24 >= 120) {
                return true;
            }
        }
        return false;
    }
}

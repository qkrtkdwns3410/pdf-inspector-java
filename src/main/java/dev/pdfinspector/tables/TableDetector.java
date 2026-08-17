package dev.pdfinspector.tables;

import dev.pdfinspector.model.TextLine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conservative text-alignment signal for a future table conversion pipeline.
 * It only reports a likely table; it never changes reading order or Markdown.
 */
public final class TableDetector {
    public boolean hasLikelyTable(List<TextLine> lines) {
        if (lines.size() < 6) {
            return false;
        }
        Map<Integer, Integer> xAlignmentCounts = new HashMap<Integer, Integer>();
        for (TextLine line : lines) {
            int bucket = Math.round(line.getX() / 12.0f);
            Integer count = xAlignmentCounts.get(Integer.valueOf(bucket));
            xAlignmentCounts.put(Integer.valueOf(bucket), Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        int repeatingColumns = 0;
        for (Integer count : xAlignmentCounts.values()) {
            if (count.intValue() >= 3) {
                repeatingColumns++;
            }
        }
        return repeatingColumns >= 3;
    }
}

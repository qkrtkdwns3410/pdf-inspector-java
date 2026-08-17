package dev.pdfinspector.markdown;

import dev.pdfinspector.model.TextItem;
import dev.pdfinspector.model.TextLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Converts positioned glyphs into readable visual lines. */
public final class TextLineAssembler {
    private static final Comparator<TextItem> READING_ORDER = new Comparator<TextItem>() {
        @Override
        public int compare(TextItem left, TextItem right) {
            int byY = Float.compare(left.getY(), right.getY());
            return byY != 0 ? byY : compareX(left, right);
        }
    };

    public List<TextLine> assemble(List<TextItem> sourceItems) {
        if (sourceItems.isEmpty()) {
            return Collections.emptyList();
        }
        List<TextItem> items = new ArrayList<TextItem>(sourceItems);
        Collections.sort(items, READING_ORDER);

        List<List<TextItem>> groups = new ArrayList<List<TextItem>>();
        List<TextItem> group = new ArrayList<TextItem>();
        float baseline = 0.0f;
        for (TextItem item : items) {
            if (group.isEmpty()) {
                group.add(item);
                baseline = item.getY();
                continue;
            }
            float tolerance = Math.max(2.0f, Math.max(item.getFontSize(), group.get(0).getFontSize()) * 0.35f);
            if (Math.abs(item.getY() - baseline) <= tolerance) {
                group.add(item);
                baseline = (baseline * (group.size() - 1) + item.getY()) / group.size();
            } else {
                groups.add(group);
                group = new ArrayList<TextItem>();
                group.add(item);
                baseline = item.getY();
            }
        }
        groups.add(group);

        List<TextLine> lines = new ArrayList<TextLine>();
        for (List<TextItem> glyphs : groups) {
            lines.add(toLine(glyphs));
        }
        return lines;
    }

    private TextLine toLine(List<TextItem> glyphs) {
        Collections.sort(glyphs, new Comparator<TextItem>() {
            @Override
            public int compare(TextItem left, TextItem right) {
                return compareX(left, right);
            }
        });

        StringBuilder text = new StringBuilder();
        TextItem first = glyphs.get(0);
        TextItem previous = null;
        float maxRight = first.getX();
        float maxHeight = 0.0f;
        float totalFontSize = 0.0f;
        int boldCount = 0;
        int italicCount = 0;
        for (TextItem glyph : glyphs) {
            if (needsSpace(previous, glyph, text)) {
                text.append(' ');
            }
            text.append(glyph.getText());
            maxRight = Math.max(maxRight, glyph.getX() + glyph.getWidth());
            maxHeight = Math.max(maxHeight, glyph.getHeight());
            totalFontSize += glyph.getFontSize();
            if (glyph.isBold()) {
                boldCount++;
            }
            if (glyph.isItalic()) {
                italicCount++;
            }
            previous = glyph;
        }
        String lineText = text.toString().replaceAll("\\s+", " ").trim();
        return new TextLine(
                first.getPage(), lineText, first.getX(), first.getY(), maxRight - first.getX(), maxHeight,
                totalFontSize / glyphs.size(), first.getFontName(), boldCount * 2 >= glyphs.size(),
                italicCount * 2 >= glyphs.size());
    }

    private boolean needsSpace(TextItem previous, TextItem current, StringBuilder text) {
        if (previous == null || text.length() == 0) {
            return false;
        }
        char last = text.charAt(text.length() - 1);
        String currentText = current.getText();
        if (Character.isWhitespace(last) || currentText.length() == 0 || Character.isWhitespace(currentText.charAt(0))) {
            return false;
        }
        float visualGap = current.getX() - (previous.getX() + previous.getWidth());
        float inferredSpaceWidth = Math.max(1.5f, previous.getFontSize() * 0.20f);
        return visualGap > inferredSpaceWidth;
    }

    private static int compareX(TextItem left, TextItem right) {
        float overlapTolerance = Math.min(left.getWidth(), right.getWidth()) * 0.75f;
        return Math.abs(left.getX() - right.getX()) <= overlapTolerance ? 0 : Float.compare(left.getX(), right.getX());
    }
}

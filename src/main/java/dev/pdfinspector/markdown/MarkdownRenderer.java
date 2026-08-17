package dev.pdfinspector.markdown;

import dev.pdfinspector.model.MarkdownOptions;
import dev.pdfinspector.model.MarkdownProfile;
import dev.pdfinspector.model.PageExtraction;
import dev.pdfinspector.model.TextLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Renders basic semantic Markdown from positioned visual lines. */
public final class MarkdownRenderer {
    private static final Pattern LIST_PREFIX = Pattern.compile("^(?:[•▪◦‣]|[-*])\\s+.+");
    private static final Pattern ORDERED_LIST_PREFIX = Pattern.compile("^\\d+[.)]\\s+.+");
    private static final Pattern DOT_LEADERS = Pattern.compile("\\.{4,}");

    public String render(List<PageExtraction> pages, MarkdownOptions options) {
        return render(pages, options, Collections.<Integer>emptySet());
    }

    /** OCR-routed pages deliberately contribute no Markdown until OCR supplies trusted text. */
    public String render(List<PageExtraction> pages, MarkdownOptions options, Set<Integer> pagesNeedingOcr) {
        float bodyFontSize = findBodyFontSize(pages);
        StringBuilder markdown = new StringBuilder();
        TextLine previous = null;
        for (PageExtraction page : pages) {
            if (pagesNeedingOcr.contains(Integer.valueOf(page.getPage()))) {
                previous = null;
                continue;
            }
            if (options.isIncludePageNumbers()) {
                appendBlockBreak(markdown);
                markdown.append("<!-- Page ").append(page.getPage()).append(" -->\n\n");
            }
            for (TextLine line : page.getLines()) {
                String text = normalize(line.getText(), options.getProfile());
                if (text.length() == 0) {
                    continue;
                }
                if (previous != null && shouldStartNewBlock(previous, line)) {
                    appendBlockBreak(markdown);
                }
                markdown.append(formatLine(text, line, bodyFontSize)).append('\n');
                previous = line;
            }
            if (!page.getLines().isEmpty()) {
                appendBlockBreak(markdown);
                previous = null;
            }
        }
        return markdown.toString().trim();
    }

    private String normalize(String text, MarkdownProfile profile) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (profile == MarkdownProfile.COMPACT) {
            normalized = DOT_LEADERS.matcher(normalized).replaceAll("...");
        }
        return normalized;
    }

    private String formatLine(String text, TextLine line, float bodyFontSize) {
        int headingLevel = headingLevel(line, bodyFontSize);
        if (headingLevel > 0) {
            return repeat('#', headingLevel) + " " + inlineStyle(text, line);
        }
        if (LIST_PREFIX.matcher(text).matches()) {
            return "- " + text.replaceFirst("^(?:[•▪◦‣]|[-*])\\s+", "");
        }
        if (ORDERED_LIST_PREFIX.matcher(text).matches()) {
            return text;
        }
        return inlineStyle(text, line);
    }

    private int headingLevel(TextLine line, float bodyFontSize) {
        if (bodyFontSize <= 0.0f || line.getText().length() > 150) {
            return 0;
        }
        float ratio = line.getFontSize() / bodyFontSize;
        if (ratio >= 1.85f) {
            return 1;
        }
        if (ratio >= 1.55f) {
            return 2;
        }
        if (ratio >= 1.30f && line.isBold()) {
            return 3;
        }
        return 0;
    }

    private String inlineStyle(String text, TextLine line) {
        if (line.isBold() && line.isItalic()) {
            return "***" + text + "***";
        }
        if (line.isBold()) {
            return "**" + text + "**";
        }
        if (line.isItalic()) {
            return "*" + text + "*";
        }
        return text;
    }

    private boolean shouldStartNewBlock(TextLine previous, TextLine current) {
        if (previous.getPage() != current.getPage()) {
            return true;
        }
        float gap = current.getY() - previous.getY();
        float expectedLineHeight = Math.max(previous.getHeight(), previous.getFontSize()) * 1.65f;
        return gap > expectedLineHeight || current.getX() + 24.0f < previous.getX();
    }

    private float findBodyFontSize(List<PageExtraction> pages) {
        Map<Integer, Integer> frequencyByTenthPoint = new HashMap<Integer, Integer>();
        for (PageExtraction page : pages) {
            for (TextLine line : page.getLines()) {
                if (line.getText().length() < 2) {
                    continue;
                }
                Integer bucket = Integer.valueOf(Math.round(line.getFontSize() * 10.0f));
                Integer count = frequencyByTenthPoint.get(bucket);
                frequencyByTenthPoint.put(bucket, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
        }
        int bestBucket = 0;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> entry : frequencyByTenthPoint.entrySet()) {
            if (entry.getValue().intValue() > bestCount
                    || (entry.getValue().intValue() == bestCount && entry.getKey().intValue() < bestBucket)) {
                bestBucket = entry.getKey().intValue();
                bestCount = entry.getValue().intValue();
            }
        }
        return bestBucket / 10.0f;
    }

    private void appendBlockBreak(StringBuilder output) {
        while (output.length() > 0 && output.charAt(output.length() - 1) == '\n') {
            output.deleteCharAt(output.length() - 1);
        }
        if (output.length() > 0) {
            output.append("\n\n");
        }
    }

    private String repeat(char character, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            result.append(character);
        }
        return result.toString();
    }
}

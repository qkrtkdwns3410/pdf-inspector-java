package dev.pdfinspector.cli;

import java.util.LinkedHashSet;
import java.util.Set;

/** Parses the shared CLI page syntax: 1,3,5-10. */
final class PageSelectionParser {
    Set<Integer> parse(String specification) {
        if (specification == null || specification.trim().length() == 0) {
            throw new IllegalArgumentException("page selection must not be empty");
        }
        Set<Integer> pages = new LinkedHashSet<Integer>();
        String[] parts = specification.split(",");
        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.matches("\\d+")) {
                pages.add(Integer.valueOf(parsePositive(part)));
            } else if (part.matches("\\d+-\\d+")) {
                String[] bounds = part.split("-", -1);
                int from = parsePositive(bounds[0]);
                int to = parsePositive(bounds[1]);
                if (from > to) {
                    throw new IllegalArgumentException("page range start must not be after end: " + part);
                }
                for (int page = from; page <= to; page++) {
                    pages.add(Integer.valueOf(page));
                }
            } else {
                throw new IllegalArgumentException("invalid page selection segment: " + part);
            }
        }
        return pages;
    }

    private int parsePositive(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException("page numbers must be positive: " + value);
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid page number: " + value, error);
        }
    }
}

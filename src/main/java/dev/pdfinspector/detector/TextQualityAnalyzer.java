package dev.pdfinspector.detector;

import dev.pdfinspector.model.TextItem;
import java.util.List;

/** Detects decoded text that is unsafe to present as reliable Markdown. */
final class TextQualityAnalyzer {
    private TextQualityAnalyzer() {
    }

    static boolean hasEncodingIssue(List<TextItem> items) {
        CipherGarbleStats cipher = new CipherGarbleStats();
        int visible = 0;
        int alphaNumeric = 0;
        int dollars = 0;
        int letterDollarLetter = 0;
        StringBuilder joined = new StringBuilder();
        for (TextItem item : items) {
            if (item.isUnresolvedEncoding()) {
                return true;
            }
            String text = item.getText();
            joined.append(text);
            cipher.add(text);
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (!Character.isWhitespace(character)) {
                    visible++;
                    if (Character.isLetterOrDigit(character)) {
                        alphaNumeric++;
                    }
                }
                if (character == '\uFFFD' || (character >= 0x80 && character <= 0x9F)
                        || (character >= 0xE000 && character <= 0xF8FF)) {
                    return true;
                }
                if (character == '$') {
                    dollars++;
                }
            }
        }
        for (int index = 1; index + 1 < joined.length(); index++) {
            if (joined.charAt(index) == '$' && isAsciiLetter(joined.charAt(index - 1))
                    && isAsciiLetter(joined.charAt(index + 1))) {
                letterDollarLetter++;
            }
        }
        return (dollars > 10 && (letterDollarLetter > 20 || letterDollarLetter * 2 > dollars))
                || (visible >= 50 && alphaNumeric * 2 < visible)
                || cipher.looksGarbled();
    }

    private static boolean isAsciiLetter(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    /** Java port of Rust's case-shift and letter-distribution cipher detector. */
    private static final class CipherGarbleStats {
        private static final double[] ENGLISH_FREQUENCIES = {
                8.2d, 1.5d, 2.8d, 4.3d, 12.7d, 2.2d, 2.0d, 6.1d, 7.0d, 0.15d, 0.8d, 4.0d,
                2.4d, 6.7d, 7.5d, 1.9d, 0.1d, 6.0d, 6.3d, 9.1d, 2.8d, 1.0d, 2.4d, 0.15d,
                2.0d, 0.07d
        };
        private final int[] letterCounts = new int[26];
        private int asciiLetters;
        private int asciiVowels;
        private int latinExtendedLetters;
        private int nonLatinLetters;
        private int letterBigrams;
        private int caseShiftBigrams;

        void add(String text) {
            char previous = 0;
            boolean hasPrevious = false;
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (isAsciiLetter(character)) {
                    char lower = Character.toLowerCase(character);
                    letterCounts[lower - 'a']++;
                    asciiLetters++;
                    if (lower == 'a' || lower == 'e' || lower == 'i' || lower == 'o' || lower == 'u') {
                        asciiVowels++;
                    }
                    if (hasPrevious) {
                        letterBigrams++;
                        if (Character.isLowerCase(previous) && Character.isUpperCase(character)) {
                            caseShiftBigrams++;
                        }
                    }
                    previous = character;
                    hasPrevious = true;
                } else {
                    if (Character.isAlphabetic(character)) {
                        if ((character >= 0x00C0 && character <= 0x024F)
                                || (character >= 0x1E00 && character <= 0x1EFF)) {
                            latinExtendedLetters++;
                        } else {
                            nonLatinLetters++;
                        }
                    }
                    hasPrevious = false;
                }
            }
        }

        boolean looksGarbled() {
            if (asciiLetters < 200 || nonLatinLetters > asciiLetters + latinExtendedLetters) {
                return false;
            }
            if ((double) asciiVowels / asciiLetters > 0.30d) {
                return false;
            }
            boolean caseShifts = letterBigrams >= 100
                    && (double) caseShiftBigrams / letterBigrams >= 0.10d;
            return caseShifts || (cosine(false) < 0.60d && cosine(true) >= 0.90d);
        }

        private double cosine(boolean sort) {
            if (asciiLetters == 0) {
                return 1.0d;
            }
            double[] observed = new double[26];
            double[] expected = ENGLISH_FREQUENCIES.clone();
            for (int index = 0; index < observed.length; index++) {
                observed[index] = (double) letterCounts[index] / asciiLetters;
            }
            if (sort) {
                java.util.Arrays.sort(observed);
                java.util.Arrays.sort(expected);
                reverse(observed);
                reverse(expected);
            }
            double dot = 0.0d;
            double observedNorm = 0.0d;
            double expectedNorm = 0.0d;
            for (int index = 0; index < observed.length; index++) {
                dot += observed[index] * expected[index];
                observedNorm += observed[index] * observed[index];
                expectedNorm += expected[index] * expected[index];
            }
            return dot / (Math.sqrt(observedNorm) * Math.sqrt(expectedNorm));
        }

        private void reverse(double[] values) {
            for (int left = 0, right = values.length - 1; left < right; left++, right--) {
                double value = values[left];
                values[left] = values[right];
                values[right] = value;
            }
        }
    }
}

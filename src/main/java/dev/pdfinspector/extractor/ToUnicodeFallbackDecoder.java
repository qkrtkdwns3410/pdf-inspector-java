package dev.pdfinspector.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/** Minimal recovery path for malformed Type0 ToUnicode maps after PDFBox decoding fails. */
final class ToUnicodeFallbackDecoder {
    private static final Pattern BFCHAR_BLOCK = Pattern.compile("beginbfchar(.*?)endbfchar", Pattern.DOTALL);
    private static final Pattern BFRANGE_BLOCK = Pattern.compile("beginbfrange(.*?)endbfrange", Pattern.DOTALL);
    private static final Pattern PAIR = Pattern.compile("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>");
    private static final Pattern RANGE = Pattern.compile(
            "<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>");
    private static final Pattern RANGE_ARRAY = Pattern.compile(
            "<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern HEX = Pattern.compile("<([0-9A-Fa-f]+)>");

    private final Map<PDFont, Map<Integer, String>> mapsByFont = new IdentityHashMap<PDFont, Map<Integer, String>>();

    String decode(PDFont font, int code) {
        Map<Integer, String> map = mapFor(font);
        String mapped = map.get(Integer.valueOf(code));
        return mapped != null ? mapped : decodeFromTrueType(font, code);
    }

    boolean hasExplicitMappings(PDFont font) {
        return !mapFor(font).isEmpty();
    }

    private Map<Integer, String> mapFor(PDFont font) {
        Map<Integer, String> map = mapsByFont.get(font);
        if (map == null) {
            map = readToUnicodeMap(font);
            mapsByFont.put(font, map);
        }
        return map;
    }

    private Map<Integer, String> readToUnicodeMap(PDFont font) {
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();
        COSBase source = font.getCOSObject().getItem(COSName.TO_UNICODE);
        if (!(source instanceof COSStream)) {
            return result;
        }
        try (InputStream input = ((COSStream) source).createInputStream()) {
            String cmap = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            result.putAll(parseCMap(cmap));
        } catch (IOException | IllegalArgumentException ignored) {
            // PDFBox remains the primary decoder; a broken fallback map means OCR is safer.
        }
        return result;
    }

    static Map<Integer, String> parseCMap(String cmap) {
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();
        parseBfchar(cmap, result);
        parseBfrange(cmap, result);
        return result;
    }

    private static void parseBfchar(String cmap, Map<Integer, String> result) {
        Matcher blocks = BFCHAR_BLOCK.matcher(cmap);
        while (blocks.find()) {
            Matcher pairs = PAIR.matcher(blocks.group(1));
            while (pairs.find()) {
                put(result, pairs.group(1), pairs.group(2));
            }
        }
    }

    private static void parseBfrange(String cmap, Map<Integer, String> result) {
        Matcher blocks = BFRANGE_BLOCK.matcher(cmap);
        while (blocks.find()) {
            String block = blocks.group(1);
            Matcher arrays = RANGE_ARRAY.matcher(block);
            while (arrays.find()) {
                int start = hexToInt(arrays.group(1));
                int end = hexToInt(arrays.group(2));
                Matcher values = HEX.matcher(arrays.group(3));
                int code = start;
                while (values.find() && code <= end) {
                    put(result, Integer.toHexString(code), values.group(1));
                    code++;
                }
            }
            Matcher ranges = RANGE.matcher(block);
            while (ranges.find()) {
                int start = hexToInt(ranges.group(1));
                int end = hexToInt(ranges.group(2));
                String initial = utf16be(ranges.group(3));
                if (initial.codePointCount(0, initial.length()) != 1) {
                    continue;
                }
                int firstCodePoint = initial.codePointAt(0);
                for (int code = start; code <= end && code - start <= 4096; code++) {
                    result.put(Integer.valueOf(code), new String(Character.toChars(firstCodePoint + code - start)));
                }
            }
        }
    }

    private static void put(Map<Integer, String> result, String code, String value) {
        result.put(Integer.valueOf(hexToInt(code)), utf16be(value));
    }

    private String decodeFromTrueType(PDFont font, int code) {
        if (!(font instanceof PDType0Font)) {
            return null;
        }
        try {
            PDType0Font type0 = (PDType0Font) font;
            CmapLookup cmap = type0.getCmapLookup();
            if (cmap == null) {
                return null;
            }
            List<Integer> codePoints = cmap.getCharCodes(type0.codeToGID(code));
            if (codePoints == null || codePoints.isEmpty()) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            for (Integer codePoint : codePoints) {
                if (codePoint != null && Character.isValidCodePoint(codePoint.intValue())) {
                    text.appendCodePoint(codePoint.intValue());
                }
            }
            String result = text.toString();
            return result.length() == 0 || "?".equals(result) || "\uFFFD".equals(result) ? null : result;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static int hexToInt(String value) {
        return (int) Long.parseLong(value, 16);
    }

    private static String utf16be(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("odd-length CMap hex string");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(hex.substring(index * 2, index * 2 + 2), 16);
        }
        return new String(bytes, StandardCharsets.UTF_16BE);
    }
}

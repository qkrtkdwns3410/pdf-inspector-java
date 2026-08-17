package dev.pdfinspector.extractor;

import dev.pdfinspector.model.TextItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

/**
 * One PDFBox content-stream pass that collects positioned glyphs in drawing order.
 *
 * <p>{@link PDFTextStripper} is PDFBox's content-stream engine for text. Keeping its
 * protected callbacks here preserves PDFBox's mature coordinate handling while exposing
 * raw Type0 glyph codes only where a font fallback is necessary.</p>
 */
public final class PdfBoxContentStreamExtractor extends PDFTextStripper {
    private final List<TextItem> items = new ArrayList<TextItem>();
    private final ToUnicodeFallbackDecoder fallbackDecoder = new ToUnicodeFallbackDecoder();
    private final Map<PDFont, FontStyle> fontStyles = new IdentityHashMap<PDFont, FontStyle>();
    private int extractingPage;

    public PdfBoxContentStreamExtractor() throws IOException {
        setSortByPosition(false);
    }

    /** Extracts exactly one 1-based page from an already-loaded document. */
    public List<TextItem> extractPage(PDDocument document, int pageNumber) throws IOException {
        if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
            throw new IllegalArgumentException("pageNumber is outside the document: " + pageNumber);
        }
        items.clear();
        fontStyles.clear();
        extractingPage = pageNumber;
        setStartPage(pageNumber);
        setEndPage(pageNumber);
        getText(document);
        return Collections.unmodifiableList(new ArrayList<TextItem>(items));
    }

    @Override
    protected void processTextPosition(TextPosition position) {
        FontStyle style = fontStyle(position.getFont());
        items.add(new TextItem(
                extractingPage,
                position.getUnicode(),
                position.getXDirAdj(),
                position.getYDirAdj(),
                position.getWidthDirAdj(),
                position.getHeightDir(),
                style.name(),
                position.getFontSizeInPt(),
                style.bold(),
                style.italic()));
        // PDFTextStripper's string assembly is unused: this extractor owns positioned output.
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
            throws IOException {
        if (font instanceof PDType0Font && shouldRecover((PDType0Font) font, code)) {
            String recovered = fallbackDecoder.decode(font, code);
            appendFallbackGlyph(textRenderingMatrix, font, code, displacement,
                    recovered == null ? "\uFFFD" : recovered, recovered == null);
            return;
        }
        super.showGlyph(textRenderingMatrix, font, code, displacement);
    }

    private void appendFallbackGlyph(Matrix matrix, PDFont font, int code, Vector displacement,
                                     String text, boolean unresolved) throws IOException {
        PDRectangle cropBox = getCurrentPage().getCropBox();
        float x = matrix.getTranslateX() - cropBox.getLowerLeftX();
        float y = cropBox.getHeight() - (matrix.getTranslateY() - cropBox.getLowerLeftY());
        float width = Math.abs(displacement.getX() * matrix.getScalingFactorX());
        if (width == 0.0f) {
            width = Math.abs(font.getWidth(code) / 1000.0f * matrix.getScalingFactorX());
        }
        float height = Math.max(1.0f, Math.abs(matrix.getScalingFactorY()) * 0.8f);
        float fontSize = getGraphicsState().getTextState().getFontSize();
        FontStyle style = fontStyle(font);
        items.add(new TextItem(extractingPage, text, x, y, width, height, style.name(), fontSize,
                style.bold(), style.italic(), unresolved));
    }

    private static boolean isMissing(String text) {
        return text == null || text.length() == 0;
    }

    private boolean shouldRecover(PDType0Font font, int code) {
        String cmapName = font.getCMap() == null ? "" : font.getCMap().getName();
        if (cmapName.startsWith("Identity") && !fallbackDecoder.hasExplicitMappings(font)) {
            return true;
        }
        return isMissing(font.toUnicode(code));
    }

    private static String fontName(PDFont font) {
        return font == null ? "" : font.getName();
    }

    private FontStyle fontStyle(PDFont font) {
        FontStyle style = fontStyles.get(font);
        if (style != null) {
            return style;
        }
        String name = fontName(font);
        String normalizedName = name.toLowerCase(Locale.ROOT);
        style = new FontStyle(name, normalizedName.contains("bold") || normalizedName.contains("black"),
                normalizedName.contains("italic") || normalizedName.contains("oblique"));
        fontStyles.put(font, style);
        return style;
    }

    private record FontStyle(String name, boolean bold, boolean italic) { }
}

package dev.pdfinspector.extractor;

import dev.pdfinspector.model.TextItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        extractingPage = pageNumber;
        setStartPage(pageNumber);
        setEndPage(pageNumber);
        getText(document);
        return Collections.unmodifiableList(new ArrayList<TextItem>(items));
    }

    @Override
    protected void processTextPosition(TextPosition position) {
        items.add(new TextItem(
                extractingPage,
                position.getUnicode(),
                position.getXDirAdj(),
                position.getYDirAdj(),
                position.getWidthDirAdj(),
                position.getHeightDir(),
                fontName(position.getFont()),
                position.getFontSizeInPt(),
                isBold(position.getFont()),
                isItalic(position.getFont())));
        super.processTextPosition(position);
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
        items.add(new TextItem(extractingPage, text, x, y, width, height, fontName(font), fontSize,
                isBold(font), isItalic(font), unresolved));
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

    private static boolean isBold(PDFont font) {
        String name = fontName(font).toLowerCase(Locale.ROOT);
        return name.contains("bold") || name.contains("black");
    }

    private static boolean isItalic(PDFont font) {
        String name = fontName(font).toLowerCase(Locale.ROOT);
        return name.contains("italic") || name.contains("oblique");
    }
}

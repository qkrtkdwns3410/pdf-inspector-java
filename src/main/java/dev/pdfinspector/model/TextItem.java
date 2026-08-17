package dev.pdfinspector.model;

/** A positioned text glyph or glyph cluster emitted by PDFBox. */
public record TextItem(int page, String text, float x, float y, float width, float height,
                       String fontName, float fontSize, boolean bold, boolean italic,
                       boolean unresolvedEncoding) {
    public TextItem(int page, String text, float x, float y, float width, float height,
                    String fontName, float fontSize, boolean bold, boolean italic) {
        this(page, text, x, y, width, height, fontName, fontSize, bold, italic, false);
    }

    public int getPage() { return page; }
    public String getText() { return text; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getFontName() { return fontName; }
    public float getFontSize() { return fontSize; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    /** True when PDFBox and the configured font fallbacks could not decode this glyph. */
    public boolean isUnresolvedEncoding() { return unresolvedEncoding; }
}

package dev.pdfinspector.model;

/** Text grouped onto one visual baseline. */
public record TextLine(int page, String text, float x, float y, float width, float height,
                       float fontSize, String fontName, boolean bold, boolean italic) {
    public int getPage() { return page; }
    public String getText() { return text; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getFontSize() { return fontSize; }
    public String getFontName() { return fontName; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
}

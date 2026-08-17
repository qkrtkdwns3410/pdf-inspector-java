package dev.pdfinspector.model;

/** High-level routing decision for a PDF. */
public enum PdfType {
    TEXT_BASED("text_based"),
    SCANNED("scanned"),
    IMAGE_BASED("image_based"),
    MIXED("mixed");

    private final String wireName;

    PdfType(String wireName) {
        this.wireName = wireName;
    }

    public String getWireName() {
        return wireName;
    }
}

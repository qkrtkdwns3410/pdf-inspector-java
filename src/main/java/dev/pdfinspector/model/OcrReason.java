package dev.pdfinspector.model;

/** Machine-readable reason why a page should be sent to OCR. */
public enum OcrReason {
    SCANNED("scanned"),
    NO_TEXT("no_text"),
    VECTOR_TEXT("vector_text"),
    SUSPECTED_GARBLED_TEXT("suspected_garbled_text");

    private final String wireName;

    OcrReason(String wireName) {
        this.wireName = wireName;
    }

    public String getWireName() {
        return wireName;
    }
}

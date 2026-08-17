package dev.pdfinspector.model;

/** Immutable detector tuning values. */
public record DetectionConfig(ScanStrategy scanStrategy, int sampleSize, long scannedImagePixelThreshold) {
    public static final int DEFAULT_SAMPLE_SIZE = 5;
    public static final long DEFAULT_SCANNED_IMAGE_PIXEL_THRESHOLD = 2_000_000L;

    public DetectionConfig {
        if (scanStrategy == null) {
            throw new IllegalArgumentException("scanStrategy must not be null");
        }
        if (sampleSize < 1) {
            throw new IllegalArgumentException("sampleSize must be at least 1");
        }
        if (scannedImagePixelThreshold < 1) {
            throw new IllegalArgumentException("scannedImagePixelThreshold must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public ScanStrategy getScanStrategy() { return scanStrategy; }
    public int getSampleSize() { return sampleSize; }
    public long getScannedImagePixelThreshold() { return scannedImagePixelThreshold; }

    public static final class Builder {
        private ScanStrategy scanStrategy = ScanStrategy.SAMPLE;
        private int sampleSize = DEFAULT_SAMPLE_SIZE;
        private long scannedImagePixelThreshold = DEFAULT_SCANNED_IMAGE_PIXEL_THRESHOLD;

        public Builder scanStrategy(ScanStrategy scanStrategy) {
            if (scanStrategy == null) {
                throw new IllegalArgumentException("scanStrategy must not be null");
            }
            this.scanStrategy = scanStrategy;
            return this;
        }

        public Builder sampleSize(int sampleSize) {
            if (sampleSize < 1) {
                throw new IllegalArgumentException("sampleSize must be at least 1");
            }
            this.sampleSize = sampleSize;
            return this;
        }

        public Builder scannedImagePixelThreshold(long threshold) {
            if (threshold < 1) {
                throw new IllegalArgumentException("scannedImagePixelThreshold must be positive");
            }
            this.scannedImagePixelThreshold = threshold;
            return this;
        }

        public DetectionConfig build() {
            return new DetectionConfig(scanStrategy, sampleSize, scannedImagePixelThreshold);
        }
    }
}

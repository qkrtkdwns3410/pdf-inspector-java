package dev.pdfinspector.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Options for the complete PDF processing pipeline. */
public record PdfOptions(ProcessMode mode, DetectionConfig detectionConfig, MarkdownOptions markdownOptions,
                         Set<Integer> selectedPages, String password) {
    public PdfOptions {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (detectionConfig == null) {
            throw new IllegalArgumentException("detectionConfig must not be null");
        }
        if (markdownOptions == null) {
            throw new IllegalArgumentException("markdownOptions must not be null");
        }
        if (selectedPages == null) {
            throw new IllegalArgumentException("selectedPages must not be null");
        }
        for (Integer page : selectedPages) {
            if (page == null || page.intValue() < 1) {
                throw new IllegalArgumentException("selectedPages must contain positive 1-based page numbers");
            }
        }
        selectedPages = Collections.unmodifiableSet(new LinkedHashSet<Integer>(selectedPages));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PdfOptions defaults() {
        return builder().build();
    }

    public static PdfOptions detectOnly() {
        return builder().mode(ProcessMode.DETECT_ONLY).build();
    }

    public ProcessMode getMode() { return mode; }
    public DetectionConfig getDetectionConfig() { return detectionConfig; }
    public MarkdownOptions getMarkdownOptions() { return markdownOptions; }
    /** Empty means every page. Page numbers are 1-based. */
    public Set<Integer> getSelectedPages() { return selectedPages; }
    public boolean hasPageFilter() { return !selectedPages.isEmpty(); }
    public String getPassword() { return password; }

    @Override
    public String toString() {
        return "PdfOptions{mode=" + mode + ", selectedPages=" + selectedPages
                + ", password=" + (password == null ? "null" : "[REDACTED]") + "}";
    }

    public static final class Builder {
        private ProcessMode mode = ProcessMode.FULL;
        private DetectionConfig detectionConfig = DetectionConfig.builder().build();
        private MarkdownOptions markdownOptions = MarkdownOptions.builder().build();
        private Set<Integer> selectedPages = new LinkedHashSet<Integer>();
        private String password;

        public Builder mode(ProcessMode mode) {
            if (mode == null) {
                throw new IllegalArgumentException("mode must not be null");
            }
            this.mode = mode;
            return this;
        }

        public Builder detectionConfig(DetectionConfig detectionConfig) {
            if (detectionConfig == null) {
                throw new IllegalArgumentException("detectionConfig must not be null");
            }
            this.detectionConfig = detectionConfig;
            return this;
        }

        public Builder markdownOptions(MarkdownOptions markdownOptions) {
            if (markdownOptions == null) {
                throw new IllegalArgumentException("markdownOptions must not be null");
            }
            this.markdownOptions = markdownOptions;
            return this;
        }

        public Builder selectedPages(Set<Integer> selectedPages) {
            if (selectedPages == null) {
                throw new IllegalArgumentException("selectedPages must not be null");
            }
            for (Integer page : selectedPages) {
                if (page == null || page.intValue() < 1) {
                    throw new IllegalArgumentException("selectedPages must contain positive 1-based page numbers");
                }
            }
            this.selectedPages = new LinkedHashSet<Integer>(selectedPages);
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public PdfOptions build() {
            return new PdfOptions(mode, detectionConfig, markdownOptions, selectedPages, password);
        }
    }
}

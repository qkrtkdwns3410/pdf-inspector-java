package dev.pdfinspector.model;

/** Immutable Markdown rendering options. */
public record MarkdownOptions(MarkdownProfile profile, boolean includePageNumbers) {
    public MarkdownOptions {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public MarkdownProfile getProfile() { return profile; }
    public boolean isIncludePageNumbers() { return includePageNumbers; }

    public static final class Builder {
        private MarkdownProfile profile = MarkdownProfile.STANDARD;
        private boolean includePageNumbers;

        public Builder profile(MarkdownProfile profile) {
            if (profile == null) {
                throw new IllegalArgumentException("profile must not be null");
            }
            this.profile = profile;
            return this;
        }

        public Builder includePageNumbers(boolean includePageNumbers) {
            this.includePageNumbers = includePageNumbers;
            return this;
        }

        public MarkdownOptions build() {
            return new MarkdownOptions(profile, includePageNumbers);
        }
    }
}

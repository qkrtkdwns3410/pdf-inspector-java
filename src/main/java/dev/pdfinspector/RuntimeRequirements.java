package dev.pdfinspector;

/** Enforces the repository's JDK 17 runtime baseline for API and CLI callers. */
final class RuntimeRequirements {
    private static final int MINIMUM_JDK_FEATURE = 17;

    private RuntimeRequirements() {
    }

    static void requireJdk17OrNewer() {
        String version = System.getProperty("java.specification.version", "");
        if (!isJdk17OrNewer(version)) {
            throw new IllegalStateException("PDF Inspector Java requires JDK 17 or newer; detected " + version);
        }
    }

    static boolean isJdk17OrNewer(String version) {
        if (version == null || version.length() == 0) {
            return false;
        }
        String normalized = version.startsWith("1.") ? version.substring(2) : version;
        int separator = normalized.indexOf('.');
        String feature = separator < 0 ? normalized : normalized.substring(0, separator);
        try {
            return Integer.parseInt(feature) >= MINIMUM_JDK_FEATURE;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}

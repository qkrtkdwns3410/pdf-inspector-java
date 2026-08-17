package dev.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuntimeRequirementsTest {
    @Test
    void acceptsModernFeatureVersionsAndRejectsJava8() {
        assertFalse(RuntimeRequirements.isJdk17OrNewer("1.8"));
        assertFalse(RuntimeRequirements.isJdk17OrNewer("11"));
        assertTrue(RuntimeRequirements.isJdk17OrNewer("17"));
        assertTrue(RuntimeRequirements.isJdk17OrNewer("21.0.7"));
    }
}

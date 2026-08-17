package dev.pdfinspector.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class PageSelectionParserTest {
    @Test
    void expandsSinglesAndRangesInStableOrder() {
        assertEquals(new LinkedHashSet<Integer>(Arrays.asList(1, 3, 5, 6, 7)),
                new PageSelectionParser().parse("1,3,5-7"));
    }

    @Test
    void rejectsReversedRanges() {
        assertThrows(IllegalArgumentException.class, () -> new PageSelectionParser().parse("3-1"));
    }
}

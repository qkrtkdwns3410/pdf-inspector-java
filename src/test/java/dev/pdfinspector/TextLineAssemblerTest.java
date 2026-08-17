package dev.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.pdfinspector.markdown.TextLineAssembler;
import dev.pdfinspector.model.TextItem;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextLineAssemblerTest {
    @Test
    void infersWordSpaceFromPositionGap() {
        List<TextItem> items = Arrays.asList(
                item("Hello", 72.0f),
                item("world", 110.0f));

        assertEquals("Hello world", new TextLineAssembler().assemble(items).get(0).getText());
    }

    private TextItem item(String text, float x) {
        return new TextItem(1, text, x, 100.0f, 24.0f, 10.0f,
                "Helvetica", 10.0f, false, false);
    }
}

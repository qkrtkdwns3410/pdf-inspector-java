package dev.pdfinspector.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ToUnicodeFallbackDecoderTest {
    @Test
    void parsesBfcharAndBfrangeAsUtf16be() {
        String cmap = "2 beginbfchar <0001> <0041> <0002> <D55C> endbfchar "
                + "1 beginbfrange <0003> <0005> <0042> endbfrange";

        Map<Integer, String> mapping = ToUnicodeFallbackDecoder.parseCMap(cmap);

        assertEquals("A", mapping.get(Integer.valueOf(1)));
        assertEquals("한", mapping.get(Integer.valueOf(2)));
        assertEquals("B", mapping.get(Integer.valueOf(3)));
        assertEquals("D", mapping.get(Integer.valueOf(5)));
    }
}

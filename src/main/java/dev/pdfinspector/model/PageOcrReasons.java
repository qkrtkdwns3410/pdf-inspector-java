package dev.pdfinspector.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** OCR routing reasons for a single 1-based page. */
public record PageOcrReasons(int page, List<String> reasons) {
    public PageOcrReasons {
        reasons = Collections.unmodifiableList(new ArrayList<String>(reasons));
    }

    public int getPage() { return page; }
    public List<String> getReasons() { return reasons; }
}

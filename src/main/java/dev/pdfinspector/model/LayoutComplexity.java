package dev.pdfinspector.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Layout facts discovered while extracting text. */
public record LayoutComplexity(boolean complex, List<Integer> pagesWithTables, List<Integer> pagesWithColumns) {
    public LayoutComplexity {
        pagesWithTables = immutableCopy(pagesWithTables);
        pagesWithColumns = immutableCopy(pagesWithColumns);
    }

    public static LayoutComplexity simple() {
        return new LayoutComplexity(false, Collections.<Integer>emptyList(), Collections.<Integer>emptyList());
    }

    public boolean isComplex() { return complex; }
    public List<Integer> getPagesWithTables() { return pagesWithTables; }
    public List<Integer> getPagesWithColumns() { return pagesWithColumns; }

    private static List<Integer> immutableCopy(List<Integer> pages) {
        return Collections.unmodifiableList(new ArrayList<Integer>(pages));
    }
}

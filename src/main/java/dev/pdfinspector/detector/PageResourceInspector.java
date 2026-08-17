package dev.pdfinspector.detector;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Counts raster image resources without rendering the page. */
final class PageResourceInspector {
    ImageStats inspect(PDPage page) throws IOException {
        ImageStats stats = new ImageStats();
        Map<COSBase, Boolean> visited = new IdentityHashMap<COSBase, Boolean>();
        inspectResources(page.getResources(), stats, visited);
        inspectOperators(page, stats);
        return stats;
    }

    private void inspectOperators(PDPage page, ImageStats stats) {
        PDFStreamParser parser = null;
        try {
            parser = new PDFStreamParser(page);
            for (Object token : parser.parse()) {
                if (!(token instanceof Operator)) {
                    continue;
                }
                String name = ((Operator) token).getName();
                if ("Tj".equals(name) || "TJ".equals(name) || "'".equals(name) || "\"".equals(name)) {
                    stats.textOperatorCount++;
                } else if (isPathOperator(name)) {
                    stats.pathOperatorCount++;
                }
            }
        } catch (IOException ignored) {
            // Corrupt page streams are handled by PDFBox extraction and OCR routing.
        } finally {
            if (parser != null) {
                try {
                    parser.close();
                } catch (IOException ignored) {
                    // Parsing was already best-effort and owns no caller-visible resource.
                }
            }
        }
    }

    private boolean isPathOperator(String name) {
        return "m".equals(name) || "l".equals(name) || "c".equals(name) || "v".equals(name)
                || "y".equals(name) || "h".equals(name) || "re".equals(name) || "S".equals(name)
                || "s".equals(name) || "f".equals(name) || "F".equals(name) || "f*".equals(name)
                || "B".equals(name) || "B*".equals(name) || "b".equals(name) || "b*".equals(name);
    }

    private void inspectResources(PDResources resources, ImageStats stats, Map<COSBase, Boolean> visited)
            throws IOException {
        if (resources == null || visited.put(resources.getCOSObject(), Boolean.TRUE) != null) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xObject;
                stats.imageCount++;
                long pixels = (long) image.getWidth() * (long) image.getHeight();
                stats.largestImagePixels = Math.max(stats.largestImagePixels, pixels);
            } else if (xObject instanceof PDFormXObject) {
                inspectResources(((PDFormXObject) xObject).getResources(), stats, visited);
            }
        }
    }

    static final class ImageStats {
        private int imageCount;
        private long largestImagePixels;
        private int textOperatorCount;
        private int pathOperatorCount;

        int getImageCount() {
            return imageCount;
        }

        long getLargestImagePixels() {
            return largestImagePixels;
        }

        boolean hasVectorText() {
            return pathOperatorCount >= 1000 && pathOperatorCount > textOperatorCount * 200;
        }
    }
}

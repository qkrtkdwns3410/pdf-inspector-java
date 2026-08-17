package dev.pdfinspector.cli;

import dev.pdfinspector.PdfInspector;
import dev.pdfinspector.model.PdfOptions;
import dev.pdfinspector.model.PdfProcessResult;
import dev.pdfinspector.model.ProcessMode;

/** `detect-pdf` equivalent command-line entry point. */
public final class DetectPdf {
    private DetectPdf() {
    }

    public static void main(String[] arguments) {
        try {
            CommandOptions command = CommandOptions.parse(arguments);
            ProcessMode mode = command.isAnalyze() ? ProcessMode.ANALYZE : ProcessMode.DETECT_ONLY;
            PdfOptions options = PdfOptions.builder()
                    .mode(mode)
                    .selectedPages(command.getSelectedPages())
                    .password(command.getPassword())
                    .build();
            PdfProcessResult result = PdfInspector.process(command.getInput(), options);
            if (command.isJson()) {
                System.out.println(new ResultJsonWriter().write(result));
            } else {
                System.out.print(summary(result, command.isAnalyze()));
            }
        } catch (Exception error) {
            if (contains(arguments, "--json")) {
                System.out.println(new ResultJsonWriter().error(error.getMessage()));
            } else {
                System.err.println("Error: " + error.getMessage());
                System.err.println("Usage: detect-pdf <pdf-file> [--json] [--analyze] [--select-pages 1,3,5-10]");
            }
            System.exit(1);
        }
    }

    private static String summary(PdfProcessResult result, boolean includeLayout) {
        StringBuilder output = new StringBuilder();
        output.append("Type: ").append(result.getPdfType().getWireName()).append('\n');
        output.append("Confidence: ").append(Math.round(result.getConfidence() * 100.0d)).append("%\n");
        output.append("Pages: ").append(result.getPageCount()).append('\n');
        output.append("Pages sampled: ").append(result.getPagesSampled()).append('\n');
        output.append("Pages with text: ").append(result.getPagesWithText()).append('\n');
        if (!result.getPagesNeedingOcr().isEmpty()) {
            output.append("Pages needing OCR: ").append(result.getPagesNeedingOcr()).append('\n');
        }
        if (includeLayout) {
            output.append("Table pages: ").append(result.getLayout().getPagesWithTables()).append('\n');
            output.append("Column pages: ").append(result.getLayout().getPagesWithColumns()).append('\n');
        }
        return output.toString();
    }

    private static boolean contains(String[] arguments, String expected) {
        for (String argument : arguments) {
            if (expected.equals(argument)) {
                return true;
            }
        }
        return false;
    }
}

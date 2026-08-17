package dev.pdfinspector.cli;

import dev.pdfinspector.PdfInspector;
import dev.pdfinspector.model.MarkdownOptions;
import dev.pdfinspector.model.MarkdownProfile;
import dev.pdfinspector.model.PdfOptions;
import dev.pdfinspector.model.PdfProcessResult;
import dev.pdfinspector.model.ProcessMode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** `pdf2md` equivalent command-line entry point. */
public final class Pdf2Md {
    private Pdf2Md() {
    }

    public static void main(String[] arguments) {
        CommandOptions command;
        try {
            command = CommandOptions.parse(arguments);
            PdfProcessResult result = PdfInspector.process(command.getInput(), toPdfOptions(command));
            String output = format(result, command);
            write(command, output);
        } catch (Exception error) {
            boolean json = contains(arguments, "--json") || contains(arguments, "--items-json");
            if (json) {
                System.out.println(new ResultJsonWriter().error(error.getMessage()));
            } else {
                System.err.println("Error: " + error.getMessage());
                printUsage();
            }
            System.exit(1);
        }
    }

    private static PdfOptions toPdfOptions(CommandOptions command) {
        if (command.isItemsJson() && command.isDetectOnly()) {
            throw new IllegalArgumentException("--items-json cannot be combined with --detect-only");
        }
        ProcessMode mode = command.isItemsJson() ? ProcessMode.ANALYZE
                : command.isDetectOnly() ? ProcessMode.DETECT_ONLY
                : command.isAnalyze() ? ProcessMode.ANALYZE : ProcessMode.FULL;
        MarkdownOptions markdown = MarkdownOptions.builder()
                .profile(command.isCompact() ? MarkdownProfile.COMPACT : MarkdownProfile.STANDARD)
                .includePageNumbers(command.hasPageMarkers())
                .build();
        return PdfOptions.builder()
                .mode(mode)
                .markdownOptions(markdown)
                .selectedPages(command.getSelectedPages())
                .password(command.getPassword())
                .build();
    }

    private static String format(PdfProcessResult result, CommandOptions command) {
        if (command.isItemsJson()) {
            return new ResultJsonWriter().writeItems(result);
        }
        if (command.isJson()) {
            return new ResultJsonWriter().write(result);
        }
        if (command.isDetectOnly() || command.isAnalyze()) {
            return humanSummary(result);
        }
        if (command.isRaw()) {
            return result.getMarkdown() == null ? "" : result.getMarkdown();
        }
        String markdown = result.getMarkdown() == null ? "" : result.getMarkdown();
        return "PDF type: " + result.getPdfType().getWireName() + "\n"
                + "Pages: " + result.getPageCount() + "\n\n" + markdown;
    }

    private static String humanSummary(PdfProcessResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("Type: ").append(result.getPdfType().getWireName()).append('\n');
        summary.append("Pages: ").append(result.getPageCount()).append('\n');
        summary.append("Confidence: ").append(Math.round(result.getConfidence() * 100.0d)).append("%\n");
        summary.append("Processing time: ").append(result.getProcessingTimeMs()).append("ms\n");
        if (!result.getPagesNeedingOcr().isEmpty()) {
            summary.append("Pages needing OCR: ").append(result.getPagesNeedingOcr()).append('\n');
        }
        if (result.getLayout().isComplex()) {
            summary.append("Table pages: ").append(result.getLayout().getPagesWithTables()).append('\n');
            summary.append("Column pages: ").append(result.getLayout().getPagesWithColumns()).append('\n');
        }
        return summary.toString();
    }

    private static void write(CommandOptions command, String output) throws IOException {
        if (command.getOutput() == null) {
            System.out.println(output);
        } else {
            Files.write(command.getOutput(), output.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean contains(String[] arguments, String expected) {
        for (String argument : arguments) {
            if (expected.equals(argument)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.err.println("Usage: pdf2md <pdf-file> [output.md] [--json|--items-json] [--raw] [--compact] [--pages]");
        System.err.println("              [--select-pages 1,3,5-10] [--password PASSWORD] [--detect-only|--analyze]");
    }
}

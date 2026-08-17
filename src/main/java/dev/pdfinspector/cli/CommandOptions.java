package dev.pdfinspector.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Shared intentionally small command-line parser for the two executable classes. */
record CommandOptions(Path input, Path output, boolean json, boolean itemsJson, boolean raw, boolean compact,
                      boolean pageMarkers, boolean detectOnly, boolean analyze, String password,
                      Set<Integer> selectedPages) {
    CommandOptions {
        selectedPages = Collections.unmodifiableSet(new LinkedHashSet<Integer>(selectedPages));
    }

    static CommandOptions parse(String[] arguments) {
        if (arguments.length == 0 || hasHelp(arguments)) {
            throw new IllegalArgumentException("missing input PDF");
        }
        Path input = null;
        Path output = null;
        boolean json = false;
        boolean itemsJson = false;
        boolean raw = false;
        boolean compact = false;
        boolean pageMarkers = false;
        boolean detectOnly = false;
        boolean analyze = false;
        String password = null;
        Set<Integer> selectedPages = Collections.emptySet();

        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            if ("--json".equals(argument)) {
                json = true;
            } else if ("--items-json".equals(argument)) {
                itemsJson = true;
            } else if ("--raw".equals(argument)) {
                raw = true;
            } else if ("--compact".equals(argument)) {
                compact = true;
            } else if ("--pages".equals(argument)) {
                pageMarkers = true;
            } else if ("--detect-only".equals(argument)) {
                detectOnly = true;
            } else if ("--analyze".equals(argument)) {
                analyze = true;
            } else if ("--password".equals(argument)) {
                password = nextValue(arguments, ++index, "--password");
            } else if ("--select-pages".equals(argument)) {
                selectedPages = new PageSelectionParser().parse(nextValue(arguments, ++index, "--select-pages"));
            } else if (argument.startsWith("--")) {
                throw new IllegalArgumentException("unknown option: " + argument);
            } else if (input == null) {
                input = Paths.get(argument);
            } else if (output == null) {
                output = Paths.get(argument);
            } else {
                throw new IllegalArgumentException("only one input and optional output path are allowed");
            }
        }
        if (input == null) {
            throw new IllegalArgumentException("missing input PDF");
        }
        return new CommandOptions(input, output, json, itemsJson, raw, compact, pageMarkers, detectOnly, analyze,
                password, selectedPages);
    }

    private static boolean hasHelp(String[] arguments) {
        for (String argument : arguments) {
            if ("--help".equals(argument) || "-h".equals(argument)) {
                return true;
            }
        }
        return false;
    }

    private static String nextValue(String[] arguments, int index, String option) {
        if (index >= arguments.length || arguments[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return arguments[index];
    }

    Path getInput() { return input; }
    Path getOutput() { return output; }
    boolean isJson() { return json; }
    boolean isItemsJson() { return itemsJson; }
    boolean isRaw() { return raw; }
    boolean isCompact() { return compact; }
    boolean hasPageMarkers() { return pageMarkers; }
    boolean isDetectOnly() { return detectOnly; }
    boolean isAnalyze() { return analyze; }
    String getPassword() { return password; }
    Set<Integer> getSelectedPages() { return selectedPages; }

    @Override
    public String toString() {
        return "CommandOptions{input=" + input + ", output=" + output
                + ", password=" + (password == null ? "null" : "[REDACTED]") + "}";
    }
}

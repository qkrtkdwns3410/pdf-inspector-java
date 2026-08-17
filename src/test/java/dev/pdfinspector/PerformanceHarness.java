package dev.pdfinspector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Gradle-invoked warmed benchmark; it is intentionally outside the ordinary test suite. */
public final class PerformanceHarness {
    private static final int WARMUPS = 10;
    private static final int ITERATIONS = 20;
    private static final String[] FIXTURES = {
            "thermo-freon12.pdf", "wireless_two_col_no_rects.pdf", "text_page_with_watermark_image.pdf"
    };

    private PerformanceHarness() {
    }

    public static void main(String[] arguments) throws Exception {
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.fontbox").setLevel(Level.SEVERE);
        Path project = Paths.get("").toAbsolutePath().normalize();
        Path fixtureDirectory = project.resolve("src").resolve("test").resolve("resources").resolve("fixtures");
        Map<String, Measurement> measurements = new LinkedHashMap<String, Measurement>();
        for (String fixture : FIXTURES) {
            measurements.put(fixture, measure(fixtureDirectory.resolve(fixture)));
        }

        String environment = environment();
        String report = report(environment, measurements);
        Path latest = project.resolve("build").resolve("reports").resolve("performance").resolve("latest.json");
        Files.createDirectories(latest.getParent());
        Files.writeString(latest, report, StandardCharsets.UTF_8);

        Path baseline = project.resolve("benchmarks").resolve("performance-baseline.json");
        if (Boolean.getBoolean("pdfinspector.performance.updateBaseline")) {
            Files.createDirectories(baseline.getParent());
            Files.writeString(baseline, report, StandardCharsets.UTF_8);
            System.out.println("Performance baseline updated: " + baseline);
            return;
        }
        if (Boolean.getBoolean("pdfinspector.performance.skipBaseline")) {
            System.out.println("Performance report: " + latest);
            return;
        }
        verifyBaseline(baseline, environment, measurements);
        System.out.println("Performance report: " + latest);
    }

    private static Measurement measure(Path fixture) throws IOException {
        if (!Files.isRegularFile(fixture)) {
            throw new IOException("missing fixture: " + fixture);
        }
        for (int iteration = 0; iteration < WARMUPS; iteration++) {
            PdfInspector.process(fixture);
        }
        long[] samples = new long[ITERATIONS];
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            long started = System.nanoTime();
            PdfInspector.process(fixture);
            samples[iteration] = (System.nanoTime() - started) / 1_000_000L;
        }
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        return new Measurement(samples, median(sorted), percentile95(sorted));
    }

    private static long median(long[] sorted) {
        return (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2L;
    }

    private static long percentile95(long[] sorted) {
        return sorted[(int) Math.ceil(sorted.length * 0.95d) - 1];
    }

    private static void verifyBaseline(Path baseline, String environment, Map<String, Measurement> measurements)
            throws IOException {
        if (!Files.isRegularFile(baseline)) {
            System.out.println("No baseline yet; run -PupdatePerformanceBaseline after reviewing latest.json.");
            return;
        }
        String baselineJson = Files.readString(baseline, StandardCharsets.UTF_8);
        String baselineEnvironment = value(baselineJson, "environment");
        if (!environment.equals(baselineEnvironment)) {
            System.out.println("Baseline environment differs; report only. Current: " + environment);
            return;
        }
        List<String> regressions = new ArrayList<String>();
        for (Map.Entry<String, Measurement> entry : measurements.entrySet()) {
            Long baselineMedian = medianFor(baselineJson, entry.getKey());
            if (baselineMedian != null && entry.getValue().medianMs() > baselineMedian.longValue() * 1.10d) {
                regressions.add(entry.getKey() + " median " + entry.getValue().medianMs() + "ms > baseline "
                        + baselineMedian + "ms by more than 10%");
            }
        }
        if (!regressions.isEmpty()) {
            throw new AssertionError("Performance regression requires root-cause review: " + regressions);
        }
    }

    private static String report(String environment, Map<String, Measurement> measurements) {
        StringBuilder json = new StringBuilder("{\n  \"environment\": \"");
        json.append(escape(environment)).append("\",\n  \"fixtures\": {");
        int index = 0;
        for (Map.Entry<String, Measurement> entry : measurements.entrySet()) {
            if (index++ > 0) {
                json.append(',');
            }
            Measurement measurement = entry.getValue();
            json.append("\n    \"").append(escape(entry.getKey())).append("\": {\"median_ms\": ")
                    .append(measurement.medianMs()).append(", \"p95_ms\": ").append(measurement.p95Ms())
                    .append(", \"samples_ms\": ").append(Arrays.toString(measurement.samplesMs())).append('}');
        }
        return json.append("\n  }\n}\n").toString();
    }

    private static String environment() {
        return "java=" + System.getProperty("java.version") + ";vendor=" + System.getProperty("java.vendor")
                + ";os=" + System.getProperty("os.name") + ";arch=" + System.getProperty("os.arch");
    }

    private static String value(String json, String name) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(name) + "\\\"\\s*:\\s*\\\"([^\\\"]*)").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Long medianFor(String json, String fixture) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(fixture)
                + "\\\"\\s*:\\s*\\{\\\"median_ms\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Measurement(long[] samplesMs, long medianMs, long p95Ms) {
        private Measurement {
            samplesMs = samplesMs.clone();
        }

        @Override
        public long[] samplesMs() { return samplesMs.clone(); }
    }
}

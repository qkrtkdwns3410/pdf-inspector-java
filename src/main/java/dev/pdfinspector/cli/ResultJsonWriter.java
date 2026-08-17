package dev.pdfinspector.cli;

import dev.pdfinspector.model.PageOcrReasons;
import dev.pdfinspector.model.PdfProcessResult;
import dev.pdfinspector.model.PageExtraction;
import dev.pdfinspector.model.TextItem;
import java.util.List;

/** Dependency-free JSON writer for stable command-line output. */
final class ResultJsonWriter {
    String write(PdfProcessResult result) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        field(json, "pdf_type", result.getPdfType().getWireName()).append(',');
        numberField(json, "page_count", result.getPageCount()).append(',');
        numberField(json, "pages_sampled", result.getPagesSampled()).append(',');
        numberField(json, "pages_with_text", result.getPagesWithText()).append(',');
        numberField(json, "processing_time_ms", result.getProcessingTimeMs()).append(',');
        numberField(json, "confidence", result.getConfidence()).append(',');
        nullableField(json, "title", result.getTitle()).append(',');
        nullableField(json, "markdown", result.getMarkdown()).append(',');
        json.append("\"pages_needing_ocr\":");
        numberArray(json, result.getPagesNeedingOcr());
        json.append(',').append("\"ocr_reasons_by_page\":");
        reasonsArray(json, result.getOcrReasonsByPage());
        json.append(',').append("\"is_complex\":").append(result.getLayout().isComplex());
        json.append(',').append("\"pages_with_tables\":");
        numberArray(json, result.getLayout().getPagesWithTables());
        json.append(',').append("\"pages_with_columns\":");
        numberArray(json, result.getLayout().getPagesWithColumns());
        json.append(',').append("\"has_encoding_issues\":").append(result.hasEncodingIssues());
        json.append('}');
        return json.toString();
    }

    String error(String message) {
        return "{\"error\":\"" + escape(message == null ? "Unknown error" : message) + "\"}";
    }

    String writeItems(PdfProcessResult result) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        boolean first = true;
        for (PageExtraction page : result.getPageExtractions()) {
            for (TextItem item : page.getItems()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append('{');
                numberField(json, "page", item.getPage()).append(',');
                field(json, "text", item.getText()).append(',');
                numberField(json, "x", item.getX()).append(',');
                numberField(json, "y", item.getY()).append(',');
                numberField(json, "width", item.getWidth()).append(',');
                numberField(json, "height", item.getHeight()).append(',');
                field(json, "font_name", item.getFontName()).append(',');
                numberField(json, "font_size", item.getFontSize()).append(',');
                json.append("\"bold\":").append(item.isBold()).append(',');
                json.append("\"italic\":").append(item.isItalic()).append(',');
                json.append("\"unresolved_encoding\":").append(item.isUnresolvedEncoding());
                json.append('}');
            }
        }
        return json.append("]}").toString();
    }

    private StringBuilder field(StringBuilder output, String name, String value) {
        return output.append('"').append(name).append("\":\"").append(escape(value)).append('"');
    }

    private StringBuilder nullableField(StringBuilder output, String name, String value) {
        output.append('"').append(name).append("\":");
        return value == null ? output.append("null") : output.append('"').append(escape(value)).append('"');
    }

    private StringBuilder numberField(StringBuilder output, String name, Number value) {
        return output.append('"').append(name).append("\":").append(value);
    }

    private void numberArray(StringBuilder output, List<Integer> values) {
        output.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            output.append(values.get(index));
        }
        output.append(']');
    }

    private void reasonsArray(StringBuilder output, List<PageOcrReasons> entries) {
        output.append('[');
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            PageOcrReasons entry = entries.get(index);
            output.append("{\"page\":").append(entry.getPage()).append(",\"reasons\":[");
            for (int reasonIndex = 0; reasonIndex < entry.getReasons().size(); reasonIndex++) {
                if (reasonIndex > 0) {
                    output.append(',');
                }
                output.append('"').append(escape(entry.getReasons().get(reasonIndex))).append('"');
            }
            output.append("]}");
        }
        output.append(']');
    }

    private String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", Integer.valueOf(character)));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }
}

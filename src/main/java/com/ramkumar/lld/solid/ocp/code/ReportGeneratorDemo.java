package com.ramkumar.lld.solid.ocp.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario A: Report Generation System
 *
 * Demonstrates the Open/Closed Principle (OCP).
 *
 * STEP 1 — shows the VIOLATION: ReportGeneratorGod uses an if/else chain on a
 *           type string. Every new format forces you to open and edit the class.
 *
 * STEP 2 — shows the FIX: a ReportExporter interface is the stable extension
 *           point. New formats are new classes — zero modification to existing code.
 *           Proven by adding JsonExporter without touching any existing class.
 *
 * OCP concepts illustrated:
 *   - "closed for modification" = the if/else never grows again
 *   - "open for extension"      = new format → new class, plug it in
 *   - Interface as extension point
 *   - Strategy / Plugin registration pattern (Map-based dispatch)
 *   - Template Method as an alternative extension point (BaseExporter)
 *   - instanceof as an OCP violation even when hidden inside a helper method
 */
public class ReportGeneratorDemo {

    // =========================================================================
    // DOMAIN MODEL — shared
    // =========================================================================

    static class Report {
        private final String reportId;
        private final String title;
        private final String content;
        private final String author;

        public Report(String reportId, String title, String content, String author) {
            if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId blank");
            if (title    == null || title.isBlank())    throw new IllegalArgumentException("title blank");
            if (content  == null || content.isBlank())  throw new IllegalArgumentException("content blank");
            if (author   == null || author.isBlank())   throw new IllegalArgumentException("author blank");
            this.reportId = reportId;
            this.title    = title;
            this.content  = content;
            this.author   = author;
        }

        public String getReportId() { return reportId; }
        public String getTitle()    { return title; }
        public String getContent()  { return content; }
        public String getAuthor()   { return author; }
    }

    // =========================================================================
    // ❌  VIOLATION — if/else chain that must be edited for every new format
    // =========================================================================

    /**
     * OCP VIOLATION — this class has a growing if/else chain.
     *
     * Adding JSON requires:
     *   1. Open this file
     *   2. Add `else if (type.equals("JSON")) { ... }`
     *   3. Retest PDF, CSV, HTML rendering even though you didn't touch them
     *
     * Adding XML, YAML, Markdown each repeat the same violation.
     */
    @SuppressWarnings("unused")
    static class ReportGeneratorGod {

        public String generateReport(String type, Report report) {
            if (type.equals("PDF")) {
                return "=== PDF ===\n"
                     + "Title:   " + report.getTitle()   + "\n"
                     + "Author:  " + report.getAuthor()  + "\n"
                     + "Content: " + report.getContent() + "\n"
                     + "=========";

            } else if (type.equals("CSV")) {
                return "reportId,title,author,content\n"
                     + report.getReportId() + ","
                     + report.getTitle()    + ","
                     + report.getAuthor()   + ","
                     + report.getContent();

            } else if (type.equals("HTML")) {
                return "<html>\n"
                     + "  <h1>" + report.getTitle()   + "</h1>\n"
                     + "  <em>" + report.getAuthor()  + "</em>\n"
                     + "  <p>"  + report.getContent() + "</p>\n"
                     + "</html>";

            } else {
                // ← Every new format requires editing this method
                throw new IllegalArgumentException("Unknown format: " + type);
            }
        }
    }

    // =========================================================================
    // ✅  FIX — Interface as the extension point (CLOSED for modification)
    // =========================================================================

    // ── Extension point abstraction ───────────────────────────────────────────
    // This interface is the STABLE contract — it never changes.
    // New formats implement this interface without touching anything else.
    interface ReportExporter {
        String export(Report report);   // what to produce
        String getFormat();             // e.g. "PDF", "CSV", "HTML"
    }

    // ── Concrete implementations (existing, never modified after they ship) ──

    static class PdfExporter implements ReportExporter {
        @Override public String getFormat() { return "PDF"; }

        @Override
        public String export(Report report) {
            return "=== PDF ===\n"
                 + "Title:   " + report.getTitle()   + "\n"
                 + "Author:  " + report.getAuthor()  + "\n"
                 + "Content: " + report.getContent() + "\n"
                 + "=========";
        }
    }

    static class CsvExporter implements ReportExporter {
        @Override public String getFormat() { return "CSV"; }

        @Override
        public String export(Report report) {
            return "reportId,title,author,content\n"
                 + report.getReportId() + ","
                 + report.getTitle()    + ","
                 + report.getAuthor()   + ","
                 + report.getContent();
        }
    }

    static class HtmlExporter implements ReportExporter {
        @Override public String getFormat() { return "HTML"; }

        @Override
        public String export(Report report) {
            return "<html>\n"
                 + "  <h1>" + report.getTitle()   + "</h1>\n"
                 + "  <em>" + report.getAuthor()  + "</em>\n"
                 + "  <p>"  + report.getContent() + "</p>\n"
                 + "</html>";
        }
    }

    // ── NEW FORMAT — added WITHOUT modifying any existing class ───────────────
    // OCP in action: JsonExporter is a new class, not a new else-if.
    // PdfExporter, CsvExporter, HtmlExporter, and ReportService are all
    // completely unchanged. This is what "closed for modification" means.
    static class JsonExporter implements ReportExporter {
        @Override public String getFormat() { return "JSON"; }

        @Override
        public String export(Report report) {
            return "{\n"
                 + "  \"reportId\": \""  + report.getReportId() + "\",\n"
                 + "  \"title\": \""     + report.getTitle()    + "\",\n"
                 + "  \"author\": \""    + report.getAuthor()   + "\",\n"
                 + "  \"content\": \""   + report.getContent()  + "\"\n"
                 + "}";
        }
    }

    // ── Template Method variant — OCP via abstract class ─────────────────────
    // Use this when several formats share a common skeleton (header + body + footer).
    // Extension point: writeBody() is abstract — each subclass only fills in what differs.
    abstract static class BaseExporter implements ReportExporter {

        // Template Method — final because the skeleton must not change
        @Override
        public final String export(Report report) {
            return writeHeader(report) + writeBody(report) + writeFooter();
        }

        // SHARED behaviour — all subclasses inherit this; only override if needed
        protected String writeHeader(Report report) {
            return "=== " + getFormat() + " | " + report.getTitle() + " ===\n";
        }

        // EXTENSION POINT — subclasses define this; the Template Method calls it
        protected abstract String writeBody(Report report);

        protected String writeFooter() {
            return "\n--- end of report ---\n";
        }
    }

    // Subclass only implements the body; header and footer are inherited
    static class MarkdownExporter extends BaseExporter {
        @Override public String getFormat() { return "MARKDOWN"; }

        @Override
        protected String writeBody(Report report) {
            return "# " + report.getTitle() + "\n"
                 + "*" + report.getAuthor() + "*\n\n"
                 + report.getContent() + "\n";
        }
    }

    // ── ReportService — CLOSED for modification, OPEN for new exporters ───────
    // Uses a Map-based registry: no if/else, no switch.
    // Caller registers a new exporter → service dispatches to it automatically.
    static class ReportService {

        // PLUGIN REGISTRY — new formats registered here, never in the code below
        private final Map<String, ReportExporter> exporters = new HashMap<>();

        // EXTENSION POINT — anyone can add a new format without modifying this class
        public void register(ReportExporter exporter) {
            exporters.put(exporter.getFormat(), exporter);
        }

        // CLOSED — this method never changes regardless of how many formats exist
        public String generate(String format, Report report) {
            ReportExporter exporter = exporters.get(format);
            if (exporter == null)
                throw new IllegalArgumentException("No exporter registered for format: " + format);
            return exporter.export(report);  // POLYMORPHIC dispatch — no if/else
        }

        public List<String> supportedFormats() {
            return new ArrayList<>(exporters.keySet());
        }
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" OCP Demo: Report Generation System");
        System.out.println("═══════════════════════════════════════════════════════\n");

        Report report = new Report("RPT-001", "Q3 Sales Summary",
                "Revenue grew 12% year-over-year driven by enterprise segment.", "Alice");

        // ── 1. Build the service and register existing exporters ─────────────
        ReportService service = new ReportService();
        service.register(new PdfExporter());
        service.register(new CsvExporter());
        service.register(new HtmlExporter());

        System.out.println("── 1. Existing formats ──────────────────────────────────");
        System.out.println("[PDF]\n"  + service.generate("PDF",  report));
        System.out.println("\n[CSV]\n" + service.generate("CSV",  report));
        System.out.println("\n[HTML]\n"+ service.generate("HTML", report));

        // ── 2. OCP PROOF: add JSON without touching any existing class ────────
        System.out.println("\n── 2. OCP proof: adding JSON (zero modification to existing code) ──");
        service.register(new JsonExporter());   // ← only new code; no if/else added anywhere
        System.out.println("[JSON]\n" + service.generate("JSON", report));

        // ── 3. Template Method variant ────────────────────────────────────────
        System.out.println("\n── 3. Template Method: MarkdownExporter ─────────────────");
        service.register(new MarkdownExporter());
        System.out.println("[MARKDOWN]\n" + service.generate("MARKDOWN", report));

        // ── 4. Supported formats (grows automatically) ────────────────────────
        System.out.println("\n── 4. Supported formats (no manual update needed) ────────");
        System.out.println(service.supportedFormats());

        // ── 5. Unknown format → clear error ──────────────────────────────────
        System.out.println("\n── 5. Unknown format → exception ────────────────────────");
        try {
            service.generate("YAML", report);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        System.out.println("\n── Summary ──────────────────────────────────────────────");
        System.out.println("Added JSON and MARKDOWN without modifying PdfExporter,");
        System.out.println("CsvExporter, HtmlExporter, or ReportService.");
        System.out.println("That is OCP: open for extension, closed for modification.");
    }
}

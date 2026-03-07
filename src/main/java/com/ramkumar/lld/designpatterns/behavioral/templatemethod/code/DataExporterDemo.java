package com.ramkumar.lld.designpatterns.behavioral.templatemethod.code;

// ─────────────────────────────────────────────────────────────────────────────
// Template Method Pattern — Scenario A: Data Export Pipeline
//
// Problem: A reporting system needs to export data in multiple formats (CSV, HTML).
//   The export algorithm always follows the same five steps: open output, write
//   header, write rows, write footer (optional), close output. The open/close steps
//   are identical for all formats; only the header, row, and footer formats differ.
//
//   Without Template Method: each exporter duplicates open/close logic, and
//   developers can accidentally skip steps or reorder them.
//
// Solution: Put the fixed sequence in a `final` template method `export()` in
//   the abstract base class. Subclasses override only the steps that differ.
//
// Participants:
//   DataExporter   [AbstractClass]    — owns the template method and concrete steps
//   CsvExporter    [ConcreteClass 1]  — fills in CSV-specific step implementations
//   HtmlExporter   [ConcreteClass 2]  — fills in HTML-specific step implementations
// ─────────────────────────────────────────────────────────────────────────────

// ── [AbstractClass] — owns the algorithm skeleton ─────────────────────────────
abstract class DataExporter {

    // [TemplateMethod] final = no subclass can reorder or skip the five steps.
    // This is the single most important modifier in the Template Method pattern.
    public final void export(String[] rows) {
        openOutput();           // [ConcreteStep] same for all formats
        writeHeader();          // [AbstractStep] varies per format
        writeRows(rows);        // [AbstractStep] varies per format
        writeFooter();          // [Hook] optional — default is no-op
        closeOutput();          // [ConcreteStep] same for all formats
    }

    // [ConcreteStep] Shared setup — every format uses the same resource open/close.
    // Declared here so subclasses cannot accidentally duplicate or skip it.
    protected void openOutput() {
        System.out.println("[Exporter] Opening output stream");
    }

    protected void closeOutput() {
        System.out.println("[Exporter] Closing output stream");
    }

    // [AbstractStep] Must differ per format — no sensible default exists.
    protected abstract void writeHeader();
    protected abstract void writeRows(String[] rows);

    // [Hook] Optional footer — most formats need one, but a bare-data export may not.
    // Default is empty body so subclasses are NOT forced to implement it.
    protected void writeFooter() {
        // default: no footer
    }
}

// ── [ConcreteClass 1] — CSV format ────────────────────────────────────────────
class CsvExporter extends DataExporter {

    private final String[] columnNames;

    CsvExporter(String... columnNames) {
        this.columnNames = columnNames;
    }

    // [Override] Provide the CSV-specific header — comma-separated column names.
    @Override
    protected void writeHeader() {
        System.out.println(String.join(",", columnNames));
    }

    // [Override] Each row is already comma-separated — just print it.
    @Override
    protected void writeRows(String[] rows) {
        for (String row : rows) {
            System.out.println(row);
        }
    }

    // [HookOverride] CSV files conventionally end with a blank line.
    @Override
    protected void writeFooter() {
        System.out.println();  // blank line footer
    }
}

// ── [ConcreteClass 2] — HTML table format ─────────────────────────────────────
class HtmlExporter extends DataExporter {

    private final String tableClass;

    HtmlExporter(String tableClass) {
        this.tableClass = tableClass;
    }

    @Override
    protected void writeHeader() {
        // [Override] HTML table opening tags with class attribute.
        System.out.printf("<table class=\"%s\">%n", tableClass);
        System.out.println("  <tbody>");
    }

    @Override
    protected void writeRows(String[] rows) {
        for (String row : rows) {
            System.out.printf("    <tr><td>%s</td></tr>%n", row);
        }
    }

    // [HookOverride] HTML table needs closing tags — this exporter uses the hook.
    @Override
    protected void writeFooter() {
        System.out.println("  </tbody>");
        System.out.println("</table>");
    }
}

// ── Demo ──────────────────────────────────────────────────────────────────────
public class DataExporterDemo {

    public static void main(String[] args) {

        String[] data = {"Alice,30,Engineering", "Bob,25,Marketing", "Carol,35,Design"};

        // ── 1. CSV export — skeleton is identical; only header/rows/footer differ ─
        System.out.println("─── CSV Export ───");
        // [Polymorphism] Typed as DataExporter — caller only knows export().
        DataExporter csv = new CsvExporter("Name", "Age", "Department");
        csv.export(data);

        // ── 2. HTML export — same template method call, different output ────────
        System.out.println("\n─── HTML Export ───");
        DataExporter html = new HtmlExporter("employee-table");
        html.export(data);

        // ── 3. Polymorphic batch export ──────────────────────────────────────────
        System.out.println("\n─── Batch Export (polymorphic) ───");
        DataExporter[] exporters = {
            new CsvExporter("ID", "Value"),
            new HtmlExporter("data-table")
        };
        String[] shortData = {"1,100", "2,200"};
        for (DataExporter e : exporters) {
            System.out.println(">> " + e.getClass().getSimpleName());
            e.export(shortData);
            System.out.println();
        }

        // ── 4. Anonymous subclass — only override the abstract steps ────────────
        System.out.println("─── Custom (anonymous) export ───");
        DataExporter custom = new DataExporter() {
            @Override
            protected void writeHeader() { System.out.println("=== CUSTOM HEADER ==="); }

            @Override
            protected void writeRows(String[] rows) {
                for (int i = 0; i < rows.length; i++) {
                    System.out.printf("  %d. %s%n", i + 1, rows[i]);
                }
            }
            // [HookNotOverridden] writeFooter() uses the base no-op default.
        };
        custom.export(new String[]{"Row A", "Row B"});
    }
}

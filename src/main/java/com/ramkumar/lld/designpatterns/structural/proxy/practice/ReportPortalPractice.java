package com.ramkumar.lld.designpatterns.structural.proxy.practice;

/**
 * Practice Exercise — Proxy Pattern: Report Portal Access Control
 *
 * <p><b>Scenario B — Protection Proxy</b>
 *
 * <p>You are building a reporting portal for a data platform. Three report operations
 * exist; each is restricted by the caller's role:
 *
 * <pre>
 *   Operation              VIEWER   ANALYST   ADMIN
 *   ─────────────────────────────────────────────────
 *   generateReport(type)     ✓         ✓        ✓
 *   exportData(format)       ✗         ✓        ✓
 *   purgeOldReports(days)    ✗         ✗        ✓
 * </pre>
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   ReportService          [Subject interface]         ← TODO 1
 *   RealReportService      [RealSubject]               ← pre-written, DO NOT MODIFY
 *   AccessControlProxy     [Protection Proxy]          ← TODOs 2–7
 *   Role                   [enum: VIEWER, ANALYST, ADMIN]  ← pre-written
 * </pre>
 *
 * <p><b>ReportService interface</b> (TODO 1) — declare these three methods:
 * <ol>
 *   <li>{@code String generateReport(String type)} — returns a report-ID string.</li>
 *   <li>{@code String exportData(String format)} — returns a filename string.</li>
 *   <li>{@code void purgeOldReports(int daysOld)} — returns nothing.</li>
 * </ol>
 *
 * <p><b>AccessControlProxy</b> (TODOs 2–7):
 * <ol>
 *   <li>Field {@code private final ReportService service} — the real subject.</li>
 *   <li>Field {@code private final Role role} — the current user's role.</li>
 *   <li>Constructor {@code AccessControlProxy(ReportService service, Role role)}.</li>
 *   <li>{@code generateReport}: all roles allowed; log then delegate.</li>
 *   <li>{@code exportData}: ANALYST + ADMIN only; throw {@code SecurityException} for VIEWER.</li>
 *   <li>{@code purgeOldReports}: ADMIN only; validate {@code daysOld >= 1} first,
 *       then throw {@code SecurityException} for other roles.</li>
 * </ol>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>AccessControlProxy must use <em>composition</em>, not inheritance of RealReportService.</li>
 *   <li>No {@code instanceof} checks anywhere.</li>
 *   <li>No validation inside RealReportService — it is a trusted internal service.</li>
 *   <li>Validation order in purgeOldReports: input check first, then role check.</li>
 * </ul>
 */
public class ReportPortalPractice {

    // ── Pre-written: user roles ────────────────────────────────────────────────
    enum Role { VIEWER, ANALYST, ADMIN }

    // ── Subject interface ──────────────────────────────────────────────────────
    interface ReportService {
        String generateReport(String type);
        String exportData(String format);
        void purgeOldReports(int daysOld);
    }

    // ── Real subject — DO NOT MODIFY ───────────────────────────────────────────
    static class RealReportService implements ReportService {

        @Override
        public String generateReport(String type) {
            System.out.printf("[ReportService] Generating %s report%n", type);
            return "report-" + type + "-2024";
        }

        @Override
        public String exportData(String format) {
            System.out.printf("[ReportService] Exporting data as %s%n", format);
            return "data." + format.toLowerCase();
        }

        @Override
        public void purgeOldReports(int daysOld) {
            System.out.printf("[ReportService] Purging reports older than %d days%n", daysOld);
        }
    }

    // ── Protection Proxy — implement below ────────────────────────────────────
    static class AccessControlProxy implements ReportService {

        private final ReportService realReportService;
        private final Role role;

        public AccessControlProxy(ReportService reportService, Role role){
            this.realReportService = reportService;
            this.role = role;
        }

        @Override
        public String generateReport(String type) {
            System.out.printf("[Proxy] %s → generateReport(%s)%n", role, type);
            return realReportService.generateReport(type);
        }

        @Override
        public String exportData(String format){
            if(role.equals(Role.VIEWER)) {
                throw new SecurityException("Access denied: " + role + " cannot export data");
            }
            System.out.printf("[Proxy] %s → exportData(%s)%n", role, format);
            return realReportService.exportData(format);
        }


        @Override
        public void purgeOldReports(int daysOld){
            if(daysOld < 1) {
                throw new IllegalArgumentException("daysOld must be >= 1");
            }
            if(!role.equals(Role.ADMIN)) {
                throw new SecurityException("Access denied: " +  role + " cannot purge reports");
            }
            System.out.printf("[Proxy] %s → purgeOldReports(%d)%n", role, daysOld);
            realReportService.purgeOldReports(daysOld);

        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: ADMIN can generateReport (uncomment after TODO 5) ─────────────────────
         ReportService adminProxy = new AccessControlProxy(new RealReportService(), Role.ADMIN);
         String r1 = adminProxy.generateReport("Sales");
         System.out.println("Test 1 — ADMIN generateReport: "
             + ("report-Sales-2024".equals(r1) ? "PASSED" : "FAILED (got: " + r1 + ")"));

        // ── Test 2: VIEWER can also generateReport — all roles allowed (uncomment after TODO 5) ──
         ReportService viewerProxy = new AccessControlProxy(new RealReportService(), Role.VIEWER);
         String r2 = viewerProxy.generateReport("Summary");
         System.out.println("Test 2 — VIEWER generateReport: "
             + ("report-Summary-2024".equals(r2) ? "PASSED" : "FAILED (got: " + r2 + ")"));

        // ── Test 3: ANALYST can exportData (uncomment after TODO 6) ──────────────────────
         ReportService analystProxy = new AccessControlProxy(new RealReportService(), Role.ANALYST);
         String r3 = analystProxy.exportData("CSV");
         System.out.println("Test 3 — ANALYST exportData: "
             + ("data.csv".equals(r3) ? "PASSED" : "FAILED (got: " + r3 + ")"));

        // ── Test 4: VIEWER cannot exportData — SecurityException (uncomment after TODO 6) ──
         try {
             ReportService vp = new AccessControlProxy(new RealReportService(), Role.VIEWER);
             vp.exportData("PDF");
             System.out.println("Test 4 — VIEWER exportData: FAILED (no exception thrown)");
         } catch (SecurityException e) {
             System.out.println("Test 4 — VIEWER exportData blocked: "
                 + ("Access denied: VIEWER cannot export data".equals(e.getMessage())
                     ? "PASSED" : "FAILED (got: " + e.getMessage() + ")"));
         }

        // ── Test 5: ADMIN can purgeOldReports (uncomment after TODO 7) ────────────────────
         System.out.println("Test 5 — ADMIN purgeOldReports(30) (expect [Proxy] + [ReportService] lines below):");
         ReportService ap = new AccessControlProxy(new RealReportService(), Role.ADMIN);
         ap.purgeOldReports(30);
         System.out.println("Test 5 — PASSED if both lines printed above");

        // ── Test 6: ANALYST cannot purgeOldReports — SecurityException (uncomment after TODO 7) ──
         try {
             ReportService anlp = new AccessControlProxy(new RealReportService(), Role.ANALYST);
             anlp.purgeOldReports(30);
             System.out.println("Test 6 — ANALYST purge: FAILED (no exception thrown)");
         } catch (SecurityException e) {
             System.out.println("Test 6 — ANALYST purge blocked: "
                 + ("Access denied: ANALYST cannot purge reports".equals(e.getMessage())
                     ? "PASSED" : "FAILED (got: " + e.getMessage() + ")"));
         }

        // ── Test 7: daysOld < 1 → IllegalArgumentException, even for ADMIN (uncomment after TODO 7) ──
         try {
             ReportService ap2 = new AccessControlProxy(new RealReportService(), Role.ADMIN);
             ap2.purgeOldReports(0);
             System.out.println("Test 7 — daysOld=0: FAILED (no exception thrown)");
         } catch (IllegalArgumentException e) {
             System.out.println("Test 7 — daysOld validation: "
                 + ("daysOld must be >= 1".equals(e.getMessage())
                     ? "PASSED" : "FAILED (got: " + e.getMessage() + ")"));
         }

        // ── Tdest 8: Polymorphic — RealReportService and proxy used as ReportService (uncomment after all TODOs) ──
         System.out.println("\nTest 8 — polymorphic usage:");
         ReportService direct  = new RealReportService();
         ReportService proxied = new AccessControlProxy(new RealReportService(), Role.ADMIN);
         String d = direct.generateReport("Annual");    // expected: [ReportService] line then return value
         String p = proxied.generateReport("Annual");   // expected: [Proxy] line, then [ReportService] line
         System.out.println("  direct  result: " + d); // expected: report-Annual-2024
         System.out.println("  proxied result: " + p); // expected: report-Annual-2024
         System.out.println("Test 8 — PASSED if both results equal 'report-Annual-2024'");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   AccessControlProxy must look identical to RealReportService from the outside —
    //   same interface, same method signatures. Internally it holds a reference to the
    //   real thing and a piece of state about the current caller. Before forwarding any
    //   call it checks whether that state allows the operation.

    // HINT 2 (Direct):
    //   Use the Protection Proxy pattern.
    //   AccessControlProxy implements ReportService (same interface as RealReportService).
    //   It stores:
    //     private final ReportService service  — composition, NOT inheritance
    //     private final Role role              — the caller's access level
    //   For restricted methods: throw SecurityException before delegating.
    //   For input-invalid calls: throw IllegalArgumentException before the role check.

    // HINT 3 (Near-solution skeleton — outlines only, no method bodies):
    //
    //   static class AccessControlProxy implements ReportService {
    //       private final ReportService service;
    //       private final Role role;
    //
    //       AccessControlProxy(ReportService service, Role role) { ... }
    //
    //       @Override public String generateReport(String type)  { ... }
    //       @Override public String exportData(String format)    { ... }
    //       @Override public void purgeOldReports(int daysOld)   { ... }
    //   }
}

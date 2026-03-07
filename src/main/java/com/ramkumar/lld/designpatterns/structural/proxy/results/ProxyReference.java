package com.ramkumar.lld.designpatterns.structural.proxy.results;

/**
 * Reference Solution — Proxy Pattern: Report Portal Access Control
 *
 * Key fix vs student solution:
 *   Field type is ReportService (interface), not RealReportService (concrete class).
 *   Constructor parameter is ReportService, not RealReportService.
 *   This allows the proxy to wrap any ReportService — a mock, another proxy, etc.
 *
 * Test 9 (most-common-mistake catcher):
 *   Wraps one AccessControlProxy inside another AccessControlProxy.
 *   This only compiles if the constructor and field use the ReportService interface type.
 *   If the student typed RealReportService, this test would not compile.
 */
public class ProxyReference {

    enum Role { VIEWER, ANALYST, ADMIN }

    // ── [Subject interface] ────────────────────────────────────────────────────
    interface ReportService {
        String generateReport(String type);
        String exportData(String format);
        void purgeOldReports(int daysOld);
    }

    // ── [RealSubject] — trusted internal service; no guards of its own ─────────
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

    // ── [Protection Proxy] ─────────────────────────────────────────────────────
    static class AccessControlProxy implements ReportService {

        // [KEY FIX] Field typed as the Subject interface — not RealReportService.
        // This allows wrapping any ReportService: a mock, a caching proxy, another
        // AccessControlProxy, or RealReportService. Typing it as RealReportService
        // would couple the proxy to one specific implementation and break stacking.
        private final ReportService service;  // [Interface type, not concrete]
        private final Role role;              // [OwnState] caller's access level

        // [KEY FIX] Constructor accepts ReportService, not RealReportService.
        AccessControlProxy(ReportService service, Role role) {
            this.service = service;
            this.role = role;
        }

        @Override
        public String generateReport(String type) {
            // All roles permitted — log, then delegate.
            System.out.printf("[Proxy] %s → generateReport(%s)%n", role, type);
            return service.generateReport(type);
        }

        @Override
        public String exportData(String format) {
            // [ProtectionCheck] Block before printing or delegating.
            if (role == Role.VIEWER) {  // [EnumEquality] == is always correct for enum constants
                throw new SecurityException("Access denied: " + role + " cannot export data");
            }
            System.out.printf("[Proxy] %s → exportData(%s)%n", role, format);
            return service.exportData(format);
        }

        @Override
        public void purgeOldReports(int daysOld) {
            // [ValidationFirst] Input guard fires before authorization check.
            // Even ADMIN gets IllegalArgumentException for invalid input.
            if (daysOld < 1) {
                throw new IllegalArgumentException("daysOld must be >= 1");
            }
            // [ProtectionCheck] After input is validated, check role.
            if (role != Role.ADMIN) {  // [EnumEquality] != is cleaner than !role.equals(Role.ADMIN)
                throw new SecurityException("Access denied: " + role + " cannot purge reports");
            }
            System.out.printf("[Proxy] %s → purgeOldReports(%d)%n", role, daysOld);
            service.purgeOldReports(daysOld);
        }
    }

    // ── Test harness ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        int passed = 0;
        int total  = 9;  // Tests 1–8 from practice file + Test 9 (mistake-catcher)

        // ── Test 1: ADMIN can generateReport ──────────────────────────────────
        ReportService adminProxy = new AccessControlProxy(new RealReportService(), Role.ADMIN);
        String r1 = adminProxy.generateReport("Sales");
        passed += check("Test 1 — ADMIN generateReport", "report-Sales-2024".equals(r1));

        // ── Test 2: VIEWER can generateReport — all roles allowed ─────────────
        ReportService viewerProxy = new AccessControlProxy(new RealReportService(), Role.VIEWER);
        String r2 = viewerProxy.generateReport("Summary");
        passed += check("Test 2 — VIEWER generateReport", "report-Summary-2024".equals(r2));

        // ── Test 3: ANALYST can exportData ────────────────────────────────────
        ReportService analystProxy = new AccessControlProxy(new RealReportService(), Role.ANALYST);
        String r3 = analystProxy.exportData("CSV");
        passed += check("Test 3 — ANALYST exportData", "data.csv".equals(r3));

        // ── Test 4: VIEWER cannot exportData — SecurityException ──────────────
        try {
            ReportService vp = new AccessControlProxy(new RealReportService(), Role.VIEWER);
            vp.exportData("PDF");
            passed += check("Test 4 — VIEWER exportData blocked", false);
        } catch (SecurityException e) {
            passed += check("Test 4 — VIEWER exportData blocked",
                "Access denied: VIEWER cannot export data".equals(e.getMessage()));
        }

        // ── Test 5: ADMIN can purgeOldReports ─────────────────────────────────
        System.out.println("Test 5 — ADMIN purgeOldReports(30) (expect [Proxy] + [ReportService] below):");
        ReportService ap = new AccessControlProxy(new RealReportService(), Role.ADMIN);
        ap.purgeOldReports(30);
        passed += check("Test 5 — ADMIN purgeOldReports", true);  // verified by printed output

        // ── Test 6: ANALYST cannot purgeOldReports — SecurityException ─────────
        try {
            ReportService anlp = new AccessControlProxy(new RealReportService(), Role.ANALYST);
            anlp.purgeOldReports(30);
            passed += check("Test 6 — ANALYST purge blocked", false);
        } catch (SecurityException e) {
            passed += check("Test 6 — ANALYST purge blocked",
                "Access denied: ANALYST cannot purge reports".equals(e.getMessage()));
        }

        // ── Test 7: daysOld < 1 → IllegalArgumentException, even for ADMIN ───
        try {
            ReportService ap2 = new AccessControlProxy(new RealReportService(), Role.ADMIN);
            ap2.purgeOldReports(0);
            passed += check("Test 7 — daysOld validation", false);
        } catch (IllegalArgumentException e) {
            passed += check("Test 7 — daysOld validation",
                "daysOld must be >= 1".equals(e.getMessage()));
        }

        // ── Test 8: Polymorphic — RealReportService and proxy as ReportService ─
        System.out.println("\nTest 8 — polymorphic usage:");
        ReportService direct  = new RealReportService();
        ReportService proxied = new AccessControlProxy(new RealReportService(), Role.ADMIN);
        String d = direct.generateReport("Annual");
        String p = proxied.generateReport("Annual");
        System.out.println("  direct  result: " + d);
        System.out.println("  proxied result: " + p);
        passed += check("Test 8 — polymorphic",
            "report-Annual-2024".equals(d) && "report-Annual-2024".equals(p));

        // ── Test 9 (most-common-mistake catcher): proxy wraps another proxy ────
        // The most common mistake: field typed as RealReportService instead of ReportService.
        // If that mistake was made, the next two lines will NOT compile because
        // AccessControlProxy is not a RealReportService.
        System.out.println("\nTest 9 — proxy wraps another proxy (compiles only if field is ReportService):");
        ReportService innerProxy = new AccessControlProxy(new RealReportService(), Role.ADMIN);
        ReportService outerProxy = new AccessControlProxy(innerProxy, Role.ADMIN); // [KeyLine] passes a proxy, not RealReportService
        String r9 = outerProxy.generateReport("Quarterly");
        System.out.println("  result: " + r9);  // expect double log: outer proxy + inner proxy + real service
        passed += check("Test 9 — proxy wraps proxy", "report-Quarterly-2024".equals(r9));

        System.out.printf("%n%d / %d PASSED%n", passed, total);
    }

    private static int check(String label, boolean condition) {
        System.out.println(label + ": " + (condition ? "PASSED" : "FAILED"));
        return condition ? 1 : 0;
    }
}

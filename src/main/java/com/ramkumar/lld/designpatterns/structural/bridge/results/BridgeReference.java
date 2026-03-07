package com.ramkumar.lld.designpatterns.structural.bridge.results;

/**
 * Reference Solution — Bridge Pattern: Alert System
 *
 * Fixes vs student solution:
 *   - 'public abstract' on Alert.send() — base declaration must match override visibility.
 *   - Consistent spacing before '{' in all declarations.
 *
 * Test 9 (most-common-mistake catcher):
 *   Adds a third ConcreteImplementor (PushChannel) without modifying any Alert class.
 *   This directly demonstrates the Bridge's "independent variation" benefit.
 *   If the student stored 'channel' in each subclass instead of in the Abstraction base,
 *   they would have to modify SimpleAlert and PriorityAlert to support PushChannel — and
 *   the pattern would not be a Bridge at all.
 */
public class BridgeReference {

    // ── [Implementor interface] — delivery contract; independent of Alert ──────
    interface Channel {
        String deliver(String recipient, String message);
    }

    // ── [ConcreteImplementors] — delivery mechanisms ───────────────────────────
    static class EmailChannel implements Channel {
        @Override
        public String deliver(String recipient, String message) {
            String result = "[Email] → " + recipient + ": " + message;
            System.out.println(result);
            return result;
        }
    }

    static class SMSChannel implements Channel {
        @Override
        public String deliver(String recipient, String message) {
            String result = "[SMS] → " + recipient + ": " + message;
            System.out.println(result);
            return result;
        }
    }

    // [ExtraChannel for Test 9] — a third ConcreteImplementor; zero Alert changes needed.
    static class PushChannel implements Channel {
        @Override
        public String deliver(String recipient, String message) {
            String result = "[Push] → " + recipient + ": " + message;
            System.out.println(result);
            return result;
        }
    }

    // ── [Abstraction] — holds the bridge; defines the high-level contract ──────
    abstract static class Alert {

        // [TheBridge] — protected so subclasses call channel.deliver() directly.
        // final — injected once at construction, never reassigned.
        protected final Channel channel;

        Alert(Channel channel) {
            this.channel = channel;  // [Injection] implementor provided by the caller
        }

        // [KEY FIX] public abstract — must match the intended visibility of overrides.
        // Package-private abstract would block external callers holding an Alert reference
        // from invoking send(), even though the concrete overrides are public.
        public abstract String send(String recipient, String message);
    }

    // ── [RefinedAbstractions] — vary message formatting; share the bridge ──────
    static class SimpleAlert extends Alert {

        SimpleAlert(Channel channel) {
            super(channel);  // [ChainUp] stores channel in Alert.channel, not here
        }

        @Override
        public String send(String recipient, String message) {
            // [Delegate] no formatting — pass through to the channel as-is
            return channel.deliver(recipient, message);
        }
    }

    static class PriorityAlert extends Alert {

        PriorityAlert(Channel channel) {
            super(channel);  // [ChainUp]
        }

        @Override
        public String send(String recipient, String message) {
            // [RefinedBehaviour] prepend the priority marker — the channel doesn't know about this
            return channel.deliver(recipient, "[PRIORITY] " + message);
        }
    }

    // ── Test harness ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        int passed = 0;
        int total  = 11;  // 9 from Tests 1–8 + 2 from Test 9

        // ── Test 1: SimpleAlert via EmailChannel ──────────────────────────────
        String r1 = new SimpleAlert(new EmailChannel()).send("alice@corp.com", "Server maintenance tonight");
        passed += check("Test 1 — SimpleAlert via Email",
            "[Email] → alice@corp.com: Server maintenance tonight".equals(r1));

        // ── Test 2: SimpleAlert via SMSChannel ────────────────────────────────
        String r2 = new SimpleAlert(new SMSChannel()).send("+1-555-0101", "Server maintenance tonight");
        passed += check("Test 2 — SimpleAlert via SMS",
            "[SMS] → +1-555-0101: Server maintenance tonight".equals(r2));

        // ── Test 3: PriorityAlert via EmailChannel ────────────────────────────
        String r3 = new PriorityAlert(new EmailChannel()).send("alice@corp.com", "Server down!");
        passed += check("Test 3 — PriorityAlert via Email",
            "[Email] → alice@corp.com: [PRIORITY] Server down!".equals(r3));

        // ── Test 4: PriorityAlert via SMSChannel ──────────────────────────────
        String r4 = new PriorityAlert(new SMSChannel()).send("+1-555-0101", "Server down!");
        passed += check("Test 4 — PriorityAlert via SMS",
            "[SMS] → +1-555-0101: [PRIORITY] Server down!".equals(r4));

        // ── Test 5: Same alert instance reused for multiple recipients ─────────
        Alert reusable = new SimpleAlert(new EmailChannel());
        String r5a = reusable.send("bob@corp.com", "Scheduled downtime");
        String r5b = reusable.send("carol@corp.com", "Scheduled downtime");
        passed += check("Test 5 — Reuse same alert instance",
            "[Email] → bob@corp.com: Scheduled downtime".equals(r5a)
            && "[Email] → carol@corp.com: Scheduled downtime".equals(r5b));

        // ── Test 6: Bridge — swap Channel, same Alert: zero Alert code changes ─
        String r6a = new SimpleAlert(new EmailChannel()).send("ops-team", "Disk usage 90%");
        String r6b = new SimpleAlert(new SMSChannel()).send("ops-team", "Disk usage 90%");
        passed += check("Test 6a — channel-swap (Email)", "[Email] → ops-team: Disk usage 90%".equals(r6a));
        passed += check("Test 6b — channel-swap (SMS)",   "[SMS] → ops-team: Disk usage 90%".equals(r6b));

        // ── Test 7: Bridge — swap Alert, same Channel: zero Channel code changes ─
        Channel sharedEmail = new EmailChannel();
        String r7a = new SimpleAlert(sharedEmail).send("devs", "Build failed");
        String r7b = new PriorityAlert(sharedEmail).send("devs", "Build failed");
        passed += check("Test 7a — alert-type-swap (Simple)",   "[Email] → devs: Build failed".equals(r7a));
        passed += check("Test 7b — alert-type-swap (Priority)", "[Email] → devs: [PRIORITY] Build failed".equals(r7b));

        // ── Test 8: Polymorphic — Alert[] uniform send() regardless of subtype ─
        System.out.println("\nTest 8 — polymorphic Alert[]:");
        Alert[] alerts = {
            new SimpleAlert(new EmailChannel()),
            new PriorityAlert(new EmailChannel())
        };
        for (Alert a : alerts) {
            System.out.println("  → " + a.send("all-staff@corp.com", "Year-end party!"));
        }
        System.out.println("Test 8 — PASSED if 2 lines: one plain, one with [PRIORITY] prefix");
        passed++;  // visual verification

        // ── Test 9 (most-common-mistake catcher): add a 3rd Channel — zero Alert changes ──
        //
        // PushChannel is a NEW ConcreteImplementor added after SimpleAlert and PriorityAlert
        // were written. No Alert code was changed. The bridge reference is typed as 'Channel'
        // (the interface), so any implementation slots in immediately.
        //
        // Most common mistake this catches:
        //   If the student stored 'channel' in each RefinedAbstraction subclass instead of in
        //   the Abstraction base, they would need to modify SimpleAlert and PriorityAlert to
        //   accept PushChannel — proving the pattern is not properly bridged.
        System.out.println("\nTest 9 — 3rd ConcreteImplementor (PushChannel) requires zero Alert changes:");
        Channel push = new PushChannel();
        String r9a = new SimpleAlert(push).send("device-abc", "App update available");
        String r9b = new PriorityAlert(push).send("device-xyz", "Account compromised!");
        passed += check("Test 9a — PushChannel + SimpleAlert",
            "[Push] → device-abc: App update available".equals(r9a));
        passed += check("Test 9b — PushChannel + PriorityAlert",
            "[Push] → device-xyz: [PRIORITY] Account compromised!".equals(r9b));

        System.out.printf("%n%d / %d PASSED%n", passed, total);
    }

    private static int check(String label, boolean condition) {
        System.out.println(label + ": " + (condition ? "PASSED" : "FAILED"));
        return condition ? 1 : 0;
    }
}

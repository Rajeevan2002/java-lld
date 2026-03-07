package com.ramkumar.lld.designpatterns.structural.bridge.practice;

/**
 * Practice Exercise — Bridge Pattern: Alert System
 *
 * <p><b>Scenario B — Two independent hierarchies</b>
 *
 * <p>An alert system has two orthogonal dimensions:
 * <ul>
 *   <li><b>Alert type</b> (Abstraction side): how the message is formatted before sending.</li>
 *   <li><b>Channel</b> (Implementor side): which delivery mechanism is used.</li>
 * </ul>
 *
 * <p>Without Bridge: SimpleAlertViaEmail, SimpleAlertViaSMS, PriorityAlertViaEmail,
 * PriorityAlertViaSMS → 4 classes for just 2×2. Add a third channel (Push) and two
 * more alert types and you need 15 classes. With Bridge: 2 + 2 = 4 classes, infinite
 * combinations.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   Channel         [Implementor interface]           ← TODO 1
 *   EmailChannel    [ConcreteImplementor — pre-written, DO NOT MODIFY]
 *   SMSChannel      [ConcreteImplementor — pre-written, DO NOT MODIFY]
 *   Alert           [Abstraction — abstract class]    ← TODOs 2–4
 *   SimpleAlert     [RefinedAbstraction]              ← TODO 5
 *   PriorityAlert   [RefinedAbstraction]              ← TODO 6
 * </pre>
 *
 * <p><b>Channel interface</b> (TODO 1) — declare exactly one method:
 * <ol>
 *   <li>{@code String deliver(String recipient, String message)} — sends the formatted
 *       message to the recipient via this channel. Returns the formatted string
 *       (used by tests for programmatic verification).</li>
 * </ol>
 *
 * <p><b>Alert abstract class</b> (TODOs 2–4):
 * <ul>
 *   <li>Field: {@code protected final Channel channel} — the bridge reference.
 *       {@code protected} so subclasses can call {@code channel.deliver()} directly.
 *       {@code final} — set once in constructor, never reassigned.</li>
 *   <li>Constructor: {@code Alert(Channel channel)} — assigns the field.</li>
 *   <li>Abstract method: {@code abstract String send(String recipient, String message)}
 *       — each RefinedAbstraction overrides this to format the message differently
 *       before handing it to the channel.</li>
 * </ul>
 *
 * <p><b>SimpleAlert (RefinedAbstraction)</b> (TODO 5):
 * <ul>
 *   <li>Constructor: {@code SimpleAlert(Channel channel)} — chains to {@code super(channel)}.</li>
 *   <li>{@code send(String recipient, String message) → String} — no modification to the
 *       message; delegates as-is: {@code return channel.deliver(recipient, message)}</li>
 * </ul>
 *
 * <p><b>PriorityAlert (RefinedAbstraction)</b> (TODO 6):
 * <ul>
 *   <li>Constructor: {@code PriorityAlert(Channel channel)} — chains to {@code super(channel)}.</li>
 *   <li>{@code send(String recipient, String message) → String} — prepends {@code "[PRIORITY] "}
 *       to the message before delivering:
 *       {@code return channel.deliver(recipient, "[PRIORITY] " + message)}</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code Alert} uses <em>composition</em> (holds a {@code Channel}) — does NOT extend
 *       {@code EmailChannel} or {@code SMSChannel}.</li>
 *   <li>RefinedAbstraction constructors must call {@code super(channel)} — the bridge
 *       reference lives in {@code Alert}, not in the subclass.</li>
 *   <li>No {@code instanceof} in {@code send()} — call {@code channel.deliver()} uniformly.</li>
 * </ul>
 */
public class AlertSystemPractice {

    // ── Implementor interface ──────────────────────────────────────────────────
    interface Channel {
        String deliver(String recipient, String message);
    }

    // ── ConcreteImplementors — DO NOT MODIFY ──────────────────────────────────
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

    // ── Abstraction ───────────────────────────────────────────────────────────
    abstract static class Alert {
        protected final Channel channel;
        Alert(Channel channel){
            this.channel = channel;
        }

        abstract String send(String recipient, String message);
    }

    // ── RefinedAbstractions ───────────────────────────────────────────────────
    static class SimpleAlert extends Alert {

        SimpleAlert(Channel channel){
            super(channel);
        }

        @Override
        public String send(String recipient, String message) {
            return channel.deliver(recipient, message);
        }
    }

    static class PriorityAlert extends Alert {
        PriorityAlert(Channel channel){
            super(channel);
        }

        @Override
        public String send(String recipient, String message){
            return channel.deliver(recipient, "[PRIORITY] " + message);
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: SimpleAlert via EmailChannel (uncomment after TODO 5) ────────────────
         String r1 = new SimpleAlert(new EmailChannel()).send("alice@corp.com", "Server maintenance tonight");
         System.out.println("Test 1 — SimpleAlert via Email: "
             + ("[Email] → alice@corp.com: Server maintenance tonight".equals(r1) ? "PASSED" : "FAILED (got: " + r1 + ")"));

        // ── Test 2: SimpleAlert via SMSChannel (uncomment after TODO 5) ──────────────────
         String r2 = new SimpleAlert(new SMSChannel()).send("+1-555-0101", "Server maintenance tonight");
         System.out.println("Test 2 — SimpleAlert via SMS: "
             + ("[SMS] → +1-555-0101: Server maintenance tonight".equals(r2) ? "PASSED" : "FAILED (got: " + r2 + ")"));

        // ── Test 3: PriorityAlert via EmailChannel — message prefixed (uncomment after TODO 6) ──
         String r3 = new PriorityAlert(new EmailChannel()).send("alice@corp.com", "Server down!");
         System.out.println("Test 3 — PriorityAlert via Email: "
             + ("[Email] → alice@corp.com: [PRIORITY] Server down!".equals(r3) ? "PASSED" : "FAILED (got: " + r3 + ")"));

        // ── Test 4: PriorityAlert via SMSChannel — message prefixed (uncomment after TODO 6) ──
         String r4 = new PriorityAlert(new SMSChannel()).send("+1-555-0101", "Server down!");
         System.out.println("Test 4 — PriorityAlert via SMS: "
             + ("[SMS] → +1-555-0101: [PRIORITY] Server down!".equals(r4) ? "PASSED" : "FAILED (got: " + r4 + ")"));

        // ── Test 5: Same alert instance reused for multiple recipients (uncomment after TODO 5) ──
         Alert reusable = new SimpleAlert(new EmailChannel());
         String r5a = reusable.send("bob@corp.com", "Scheduled downtime");
         String r5b = reusable.send("carol@corp.com", "Scheduled downtime");
         System.out.println("Test 5 — Reuse same alert instance: "
             + ("[Email] → bob@corp.com: Scheduled downtime".equals(r5a)
                 && "[Email] → carol@corp.com: Scheduled downtime".equals(r5b) ? "PASSED" : "FAILED"));

        // ── Test 6: Bridge — swap Channel, same Alert type: zero Alert code changes (uncomment after all TODOs) ──
         String r6a = new SimpleAlert(new EmailChannel()).send("ops-team", "Disk usage 90%");
         String r6b = new SimpleAlert(new SMSChannel()).send("ops-team", "Disk usage 90%");
         System.out.println("Test 6a — channel-swap (Email): "
             + ("[Email] → ops-team: Disk usage 90%".equals(r6a) ? "PASSED" : "FAILED (got: " + r6a + ")"));
         System.out.println("Test 6b — channel-swap (SMS):   "
             + ("[SMS] → ops-team: Disk usage 90%".equals(r6b) ? "PASSED" : "FAILED (got: " + r6b + ")"));

        // ── Test 7: Bridge — swap Alert type, same Channel: zero Channel code changes (uncomment after all TODOs) ──
         Channel sharedEmail = new EmailChannel();
         String r7a = new SimpleAlert(sharedEmail).send("devs", "Build failed");
         String r7b = new PriorityAlert(sharedEmail).send("devs", "Build failed");
         System.out.println("Test 7a — alert-type-swap (Simple):   "
             + ("[Email] → devs: Build failed".equals(r7a) ? "PASSED" : "FAILED (got: " + r7a + ")"));
         System.out.println("Test 7b — alert-type-swap (Priority): "
             + ("[Email] → devs: [PRIORITY] Build failed".equals(r7b) ? "PASSED" : "FAILED (got: " + r7b + ")"));

        // ── Test 8: Polymorphic — Alert[] with different subtypes, uniform send() call (uncomment after all TODOs) ──
         System.out.println("Test 8 — polymorphic Alert[]:");
         Alert[] alerts = {
             new SimpleAlert(new EmailChannel()),
             new PriorityAlert(new EmailChannel())
         };
         for (Alert a : alerts) {
             String result = a.send("all-staff@corp.com", "Year-end party!");  // expected: one plain, one [PRIORITY]
             System.out.println("  → " + result);
         }
         System.out.println("Test 8 — PASSED if 2 lines printed: one plain, one with [PRIORITY] prefix");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Alert and Channel are two independent hierarchies. Alert describes WHAT is
    //   sent (the formatting); Channel describes HOW it is delivered. Neither should
    //   know the other's concrete type — they communicate through interfaces and a
    //   single reference that is set at construction time.

    // HINT 2 (Direct):
    //   Use the Bridge pattern.
    //   Alert (abstract) holds:  protected final Channel channel  [the bridge]
    //   SimpleAlert extends Alert — passes message straight to channel.deliver().
    //   PriorityAlert extends Alert — prepends "[PRIORITY] " then calls channel.deliver().
    //   Both RefinedAbstractions call super(channel) in their constructors to store
    //   the bridge reference in the Abstraction base.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface Channel {
    //       String deliver(String recipient, String message);
    //   }
    //
    //   abstract static class Alert {
    //       protected final Channel channel;
    //       Alert(Channel channel) { ... }
    //       abstract String send(String recipient, String message);
    //   }
    //
    //   static class SimpleAlert extends Alert {
    //       SimpleAlert(Channel channel) { super(channel); }
    //       @Override public String send(String recipient, String message) { ... }
    //   }
    //
    //   static class PriorityAlert extends Alert {
    //       PriorityAlert(Channel channel) { super(channel); }
    //       @Override public String send(String recipient, String message) { ... }
    //   }
}

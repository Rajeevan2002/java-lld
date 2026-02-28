package com.ramkumar.lld.solid.lsp.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference solution — Liskov Substitution Principle (LSP)
 * Phase 2, Topic 3 | Scenario B: Notification Channel System
 *
 * Fixes from the practice review:
 *   Issue 1: EmailNotification/SMSNotification/PushNotification — no-arg constructors
 *            that hardcode channelName and maxMessageLength (not 2-arg)
 *   Issue 2: assertLSP(null) removed — was a placeholder that causes NPE after
 *            the method body is uncommented
 *   Issue 3: prepare() — only null → ""; blank/whitespace messages pass through
 */
public class NotificationReference {

    // =========================================================================
    // Notification — abstract base class (LSP contracts enforced here)
    // =========================================================================
    abstract static class Notification {

        private final String channelName;       // invariant: non-null, non-blank
        private final int    maxMessageLength;  // invariant: >= 10

        protected Notification(String channelName, int maxMessageLength) {
            if (channelName == null || channelName.isBlank())
                throw new IllegalArgumentException("Channel name cannot be blank");
            if (maxMessageLength < 10)
                throw new IllegalArgumentException("Max message length must be >= 10");
            this.channelName      = channelName;
            this.maxMessageLength = maxMessageLength;
        }

        // FINAL — subclasses cannot override to return null or empty (invariant preserved)
        public final String getChannelName() { return channelName; }

        // FINAL — subclasses cannot override to return 0 or negative (invariant preserved)
        public final int getMaxMessageLength() { return maxMessageLength; }

        // FINAL — the Template Method: all subtypes share the same truncation logic.
        // This is the key LSP enforcement: no subclass can override this to throw.
        // KEY FIX 3: only null → ""; blank/whitespace messages pass through unchanged
        public final String prepare(String message) {
            if (message == null) return "";   // only null triggers the empty fallback
            return message.length() > maxMessageLength
                   ? message.substring(0, maxMessageLength)   // truncate (never throw)
                   : message;                                  // short enough — pass as-is
        }

        // Abstract — subclasses implement the actual delivery mechanism
        // CONTRACT (must hold for ALL subtypes):
        //   (a) Returns false  — if recipient null or blank (graceful, no exception)
        //   (b) Returns true   — on successful delivery
        //   (c) NEVER throws   — for any non-null message; use prepare() to truncate
        //   (d) NEVER throws   — for null message; prepare() returns ""
        public abstract boolean send(String recipient, String message);
    }

    // =========================================================================
    // EmailNotification — CLOSED; 500-char limit
    // KEY FIX 1: no-arg constructor hardcodes "EMAIL" and 500
    // =========================================================================
    static class EmailNotification extends Notification {

        // No-arg constructor: EmailNotification is the authority on its own identity.
        // Callers do NOT supply the channel name or limit — those are internal constants.
        public EmailNotification() {
            super("EMAIL", 500);
        }

        @Override
        public boolean send(String recipient, String message) {
            if (recipient == null || recipient.isBlank()) return false;
            String prepared = prepare(message);   // truncates or passes through
            System.out.println("[EMAIL] To: " + recipient + " | " + prepared);
            return true;
        }
    }

    // =========================================================================
    // SMSNotification — 160-char limit; MUST truncate, never throw
    // KEY FIX 1: no-arg constructor hardcodes "SMS" and 160
    // =========================================================================
    static class SMSNotification extends Notification {

        public SMSNotification() {
            super("SMS", 160);
        }

        @Override
        public boolean send(String recipient, String message) {
            if (recipient == null || recipient.isBlank()) return false;
            // prepare() truncates to 160 chars — NO length check here, NO throw
            // This is the LSP fix: ViolatingSMS threw; correct SMS truncates via prepare()
            String prepared = prepare(message);
            System.out.println("[SMS] To: " + recipient + " | " + prepared);
            return true;
        }
    }

    // =========================================================================
    // PushNotification — 100-char limit (>= 10, invariant satisfied)
    // KEY FIX 1: no-arg constructor hardcodes "PUSH" and 100
    // =========================================================================
    static class PushNotification extends Notification {

        // 100 >= 10 → invariant satisfied; ViolatingPush passed 0 → invariant broken
        public PushNotification() {
            super("PUSH", 100);
        }

        @Override
        public boolean send(String recipient, String message) {
            if (recipient == null || recipient.isBlank()) return false;
            String prepared = prepare(message);
            System.out.println("[PUSH] To: " + recipient + " | " + prepared);
            return true;
        }
    }

    // =========================================================================
    // NotificationService — CLOSED orchestrator; no instanceof
    // =========================================================================
    static class NotificationService {

        private final List<Notification> channels = new ArrayList<>();

        public void register(Notification n) {
            if (n == null) throw new IllegalArgumentException("Notification cannot be null");
            channels.add(n);
        }

        // Polymorphic dispatch — no if/else, no instanceof
        // Every channel is substitutable: this is LSP enabling OCP in the service
        public Map<String, Boolean> broadcast(String recipient, String message) {
            Map<String, Boolean> result = new HashMap<>();
            for (Notification ch : channels) {
                result.put(ch.getChannelName(), ch.send(recipient, message));
            }
            return Collections.unmodifiableMap(result);
        }

        public List<String> getRegisteredChannels() {
            List<String> names = new ArrayList<>();
            for (Notification ch : channels) names.add(ch.getChannelName());
            return Collections.unmodifiableList(names);
        }
    }

    // =========================================================================
    // assertLSP — type-safe (takes Notification directly, not Object)
    // Represents "all code that uses Notification as a type".
    // Every subtype must pass ALL six contracts for LSP to hold.
    // KEY FIX 2: no assertLSP(null) call in main — that was a placeholder NPE
    // =========================================================================
    static void assertLSP(Notification n) {
        System.out.println("  Testing: " + n.getChannelName());

        // Contract 1: getChannelName() returns non-null, non-blank
        String name = n.getChannelName();
        System.out.println("  channelName: '" + name + "' — " +
                (name != null && !name.isBlank() ? "✅ OK" : "❌ VIOLATED"));

        // Contract 2: getMaxMessageLength() returns >= 10
        int maxLen = n.getMaxMessageLength();
        System.out.println("  maxLen: " + maxLen + " — " +
                (maxLen >= 10 ? "✅ OK" : "❌ VIOLATED (invariant broken)"));

        // Contract 3: send() with valid input returns boolean without throwing
        boolean result = n.send("test@example.com", "Hello world");
        System.out.println("  send(valid): " + result + " — ✅ did not throw");

        // Contract 4: send() with 1000-char message must NOT throw — must truncate
        String longMsg = "A".repeat(1000);
        boolean longResult = n.send("test@example.com", longMsg);
        System.out.println("  send(1000-char msg): " + longResult + " — ✅ truncated, did not throw");

        // Contract 5: send() with null message must NOT throw (prepare returns "")
        boolean nullResult = n.send("test@example.com", null);
        System.out.println("  send(null msg): " + nullResult + " — ✅ handled gracefully");

        // Contract 6: send() with blank recipient must return false (not throw)
        boolean blankRecip = n.send("", "message");
        System.out.println("  send(blank recipient): " + blankRecip + " — " +
                (!blankRecip ? "✅ returned false" : "❌ should return false"));
    }

    // =========================================================================
    // Main — same 10 tests as practice + Test 11 (constructor identity)
    //                                 + Test 12 (prepare() blank passthrough)
    // =========================================================================
    public static void main(String[] args) {

        // KEY FIX 1: no-arg constructors
        EmailNotification email = new EmailNotification();
        SMSNotification   sms   = new SMSNotification();
        PushNotification  push  = new PushNotification();

        NotificationService service = new NotificationService();
        service.register(email);
        service.register(sms);
        service.register(push);

        System.out.println("═══ Test 1: Notification construction validation ═════════");
        System.out.println("channelName : " + email.getChannelName());     // EMAIL
        System.out.println("maxMsgLen   : " + email.getMaxMessageLength()); // 500
        System.out.println("Test 1 PASSED");

        System.out.println("\n═══ Test 2: EmailNotification — normal send ═════════════");
        boolean result = email.send("alice@example.com", "Hello Alice!");
        System.out.println("sent: " + result);
        System.out.println("Test 2 PASSED: " + result);

        System.out.println("\n═══ Test 3: EmailNotification — blank recipient = false ══");
        boolean blankRecip = email.send("", "Hello");
        System.out.println("blank recipient: " + blankRecip);
        System.out.println("Test 3 PASSED: " + !blankRecip);

        System.out.println("\n═══ Test 4: SMSNotification — truncates long message (LSP) ═");
        String longMsg = "X".repeat(200);
        boolean smsResult = sms.send("+91-9876543210", longMsg);
        System.out.println("sent (200-char): " + smsResult);   // true (truncated to 160)
        System.out.println("Test 4 PASSED — no exception thrown for 200-char SMS");

        System.out.println("\n═══ Test 5: PushNotification — sends successfully ════════");
        boolean pushResult = push.send("device-token-abc123", "You have a new message!");
        System.out.println("sent: " + pushResult);
        System.out.println("Test 5 PASSED: " + pushResult);

        System.out.println("\n═══ Test 6: NotificationService — getRegisteredChannels ══");
        List<String> channels = service.getRegisteredChannels();
        System.out.println("channels: " + channels);
        System.out.println("count: " + channels.size());
        System.out.println("Test 6 PASSED: " + (channels.size() == 3));

        System.out.println("\n═══ Test 7: LSP proof — assertLSP on all types ══════════");
        System.out.println("[EmailNotification]:");
        assertLSP(email);
        System.out.println("[SMSNotification]:");
        assertLSP(sms);
        System.out.println("[PushNotification]:");
        assertLSP(push);
        // KEY FIX 2: assertLSP(null) REMOVED — was a placeholder that caused NPE
        System.out.println("Test 7 PASSED — all channels satisfy the Notification contract");

        System.out.println("\n═══ Test 8: broadcast — all channels receive the message ═");
        Map<String, Boolean> results =
                service.broadcast("user@example.com", "Flash sale starts NOW!");
        System.out.println("broadcast results: " + results);
        System.out.println("EMAIL sent: " + results.get("EMAIL"));
        System.out.println("SMS   sent: " + results.get("SMS"));
        System.out.println("PUSH  sent: " + results.get("PUSH"));
        System.out.println("Test 8 PASSED");

        System.out.println("\n═══ Test 9: broadcast — very long message (LSP safety) ══");
        String bigMsg = "B".repeat(1000);
        Map<String, Boolean> bigResults = service.broadcast("user@example.com", bigMsg);
        System.out.println("All delivered without exception: " +
                bigResults.values().stream().allMatch(v -> v));
        System.out.println("Test 9 PASSED");

        System.out.println("\n═══ Test 10: prepare() truncates at maxMessageLength ═════");
        EmailNotification e2 = new EmailNotification();
        String prepared500 = e2.prepare("C".repeat(600));
        System.out.println("EMAIL prepared length: " + prepared500.length()); // 500
        System.out.println("Test 10 PASSED: " + (prepared500.length() == 500));
        SMSNotification sms2 = new SMSNotification();
        String prepared160 = sms2.prepare("D".repeat(200));
        System.out.println("SMS   prepared length: " + prepared160.length()); // 160
        System.out.println("Test 10b PASSED: " + (prepared160.length() == 160));

        // ── Test 11 (Extra) — catches Issue 1: wrong constructor signature ────
        // If a subclass takes 2 args (channelName, maxLength) from the caller, then
        // the caller could do new SMSNotification("EMAIL", 999) — identity is broken.
        // Correct: no-arg constructors hardcode the identity.
        System.out.println("\n═══ Test 11 (Extra): No-arg constructors lock in channel identity ═");
        System.out.println("EMAIL channelName: " + email.getChannelName()); // EMAIL
        System.out.println("EMAIL maxLen:      " + email.getMaxMessageLength()); // 500
        System.out.println("SMS   channelName: " + sms.getChannelName()); // SMS
        System.out.println("SMS   maxLen:      " + sms.getMaxMessageLength()); // 160
        System.out.println("PUSH  channelName: " + push.getChannelName()); // PUSH
        System.out.println("PUSH  maxLen:      " + push.getMaxMessageLength()); // 100
        boolean t11 = email.getChannelName().equals("EMAIL")
                   && email.getMaxMessageLength() == 500
                   && sms.getChannelName().equals("SMS")
                   && sms.getMaxMessageLength() == 160
                   && push.getChannelName().equals("PUSH")
                   && push.getMaxMessageLength() == 100;
        System.out.println("Test 11 PASSED: " + t11);

        // ── Test 12 (Extra) — catches Issue 3: isBlank() in prepare() ─────────
        // prepare("   ") should return "   " (3 spaces) — not "" (empty string).
        // The isBlank() check wrongly discards whitespace-only messages.
        System.out.println("\n═══ Test 12 (Extra): prepare() passes through whitespace-only msg ═");
        String whitespace = "   ";
        String preparedWhitespace = sms2.prepare(whitespace);
        System.out.println("prepare('   '): '" + preparedWhitespace + "'");
        boolean t12 = preparedWhitespace.equals("   ");
        System.out.println("Is 3 spaces (not empty): " + t12);
        if (t12) System.out.println("Test 12 PASSED — prepare() does not discard blank messages");
        else     System.out.println("Test 12 FAILED — prepare() has isBlank() check removing valid messages");

        // Verify null still → ""
        String preparedNull = sms2.prepare(null);
        System.out.println("prepare(null): '" + preparedNull + "' (should be empty)");
        System.out.println("Null still maps to empty: " + preparedNull.isEmpty());
    }
}

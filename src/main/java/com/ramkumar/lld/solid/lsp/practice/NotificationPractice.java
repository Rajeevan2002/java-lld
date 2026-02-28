package com.ramkumar.lld.solid.lsp.practice;

import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PRACTICE — Liskov Substitution Principle (LSP)
 * Phase 2, Topic 3 | Scenario B: Notification Channel System
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROBLEM STATEMENT
 * ─────────────────
 * A messaging platform has a notification system where every channel (Email,
 * SMS, Push) is expected to be usable interchangeably. A `NotificationService`
 * sends messages through all registered channels without knowing which concrete
 * type it has — it just calls `send()` on each.
 *
 * Two bad implementations were written (shown below as VIOLATION examples):
 *
 *   ViolatingSMS  — throws IllegalArgumentException if message > 160 chars
 *                   (strengthened precondition: parent accepts any message;
 *                    this subtype ADDS a restriction the parent never had)
 *
 *   ViolatingPush — passes maxMessageLength=0 to the Notification constructor,
 *                   breaking the invariant that maxMessageLength > 0
 *                   (any code calling getMaxMessageLength() would get 0 and
 *                    produce division-by-zero errors downstream)
 *
 * Your job: implement the CORRECT hierarchy where every subtype is truly
 * substitutable — honouring the behavioural contracts of the parent.
 *
 * ─── YOUR TASK ──────────────────────────────────────────────────────────────
 *  1. Notification  — abstract class with shared fields and template method
 *  2. EmailNotification  extends Notification — no message length limit issues
 *  3. SMSNotification    extends Notification — truncates (NEVER throws)
 *  4. PushNotification   extends Notification — truncates (NEVER throws)
 *  5. NotificationService — broadcasts to all registered channels
 *
 * ─── FIELD AND CONTRACT REQUIREMENTS ────────────────────────────────────────
 *
 * Notification (abstract):
 *   Fields (private final):
 *     • channelName       : String  — cannot be blank; set by subclass via super()
 *     • maxMessageLength  : int     — must be >= 10; enforces the invariant
 *   Methods:
 *     • final String  getChannelName()         — returns channelName (never null/blank)
 *     • final int     getMaxMessageLength()    — returns maxMessageLength (always >= 10)
 *     • final String  prepare(String message)  — TEMPLATE METHOD
 *         if message is null → return ""
 *         if message.length() > maxMessageLength → return message.substring(0, maxMessageLength)
 *         else → return message as-is
 *     • abstract boolean send(String recipient, String message)
 *         CONTRACT (must hold for ALL subtypes):
 *           (a) Returns false  — if recipient is null or blank (graceful, no exception)
 *           (b) Returns true   — on successful simulated delivery
 *           (c) NEVER throws   — for any non-null message (use prepare() to truncate)
 *           (d) NEVER throws   — for null message (prepare() handles it)
 *
 * EmailNotification extends Notification:
 *   • Constructor: super("EMAIL", 500)
 *   • send(recipient, message):
 *       if recipient null/blank → return false
 *       prepared = prepare(message)
 *       print: "[EMAIL] To: <recipient> | <prepared>"
 *       return true
 *
 * SMSNotification extends Notification:
 *   • Constructor: super("SMS", 160)
 *   • send(recipient, message):
 *       if recipient null/blank → return false
 *       prepared = prepare(message)   ← this truncates — NEVER throw for long messages
 *       print: "[SMS] To: <recipient> | <prepared>"
 *       return true
 *   • LSP RULE: must NOT throw for messages longer than 160 chars — truncate instead
 *
 * PushNotification extends Notification:
 *   • Constructor: super("PUSH", 100)
 *   • send(recipient, message):
 *       if recipient null/blank → return false
 *       prepared = prepare(message)
 *       print: "[PUSH] To: <recipient> | <prepared>"
 *       return true
 *   • LSP RULE: maxMessageLength must be >= 10 (never 0)
 *
 * NotificationService:
 *   • Field: private final List<Notification> channels (new ArrayList)
 *   • register(Notification n) : void
 *       throws IllegalArgumentException if n is null
 *   • broadcast(String recipient, String message) : Map<String, Boolean>
 *       Sends to every registered channel.
 *       Calls channel.send(recipient, prepare_message_per_channel_limit)
 *         — actually just calls channel.send(recipient, message);
 *           the channel's send() calls prepare() internally
 *       Returns Map<channelName, result> (unmodifiable)
 *   • getRegisteredChannels() : List<String>
 *       Returns list of channelName from each registered Notification (unmodifiable)
 *
 * ─── DESIGN CONSTRAINTS ─────────────────────────────────────────────────────
 *  • Notification constructor must validate channelName (null/blank → IAE) and
 *    maxMessageLength (< 10 → IAE("Max message length must be >= 10")).
 *  • getChannelName() and getMaxMessageLength() must be declared final — subclasses
 *    must NOT be able to override them and return invalid values.
 *  • prepare() must be declared final — it is the LSP contract enforcer.
 *  • SMSNotification.send() must call prepare() — it must NOT throw for long messages.
 *  • NotificationService must contain NO instanceof checks.
 *  • LSP proof: assertLSP(Notification n) must pass for all three channel types
 *    (called in Test 7 — see main).
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class NotificationPractice {

    // =========================================================================
    // VIOLATION EXAMPLES — read to understand what you must NOT do.
    // These are intentionally WRONG. Do NOT modify or call them from main().
    // =========================================================================

    /**
     * ❌ LSP VIOLATION: Strengthened precondition.
     * The Notification contract says send() must handle any non-null message.
     * ViolatingSMS ADDS a restriction the parent never had: throws if length > 160.
     * Any code calling send() on a Notification reference would unexpectedly crash.
     */
    @SuppressWarnings("unused")
    static class ViolatingSMS {
        // Pretend extends Notification
        public boolean send(String recipient, String message) {
            if (message != null && message.length() > 160) {
                // ← PRECONDITION STRENGTHENED: parent accepted any message; this throws
                throw new IllegalArgumentException(
                        "SMS message must be <= 160 characters, got: " + message.length());
            }
            System.out.println("[SMS-WRONG] To: " + recipient + " | " + message);
            return true;
        }

        public int getMaxMessageLength() {
            return 160;
        }
    }

    /**
     * ❌ LSP VIOLATION: Broken invariant (postcondition weakened).
     * The Notification invariant says getMaxMessageLength() >= 10.
     * ViolatingPush returns 0 — breaking the invariant.
     * Code that calls prepare() or uses maxMessageLength for pagination would
     * divide by zero or allocate a zero-length buffer.
     *
     * The ViolatingPush "fixes" this by also breaking the constructor contract
     * (passing maxMessageLength=0 which the validated constructor would reject).
     */
    @SuppressWarnings("unused")
    static class ViolatingPush {
        // If this class could extend Notification, it would pass maxMessageLength=0
        // to super("PUSH", 0) — which the validated constructor must REJECT.
        // This is the invariant enforcement mechanism: constructor prevents the violation.

        public int getMaxMessageLength() {
            return 0;   // ← INVARIANT BROKEN: any downstream division-by-zero
        }
        public boolean send(String recipient, String message) {
            // Without a valid maxMessageLength, prepare() would produce wrong results
            return true;
        }
    }

    // =========================================================================
    // ── TODO 1: Notification (abstract class) ────────────────────────────────
    // Fields: private final String channelName, private final int maxMessageLength
    // Constructor: Notification(String channelName, int maxMessageLength)
    //   • channelName null/blank → throw IAE("Channel name cannot be blank")
    //   • maxMessageLength < 10  → throw IAE("Max message length must be >= 10")
    //
    // Methods (mark final where indicated):
    //   public final String  getChannelName()
    //   public final int     getMaxMessageLength()
    //   public final String  prepare(String message)
    //     → null message → return ""
    //     → length > maxMessageLength → return message.substring(0, maxMessageLength)
    //     → else → return message
    //   public abstract boolean send(String recipient, String message)
    // =========================================================================

    // TODO 1a: declare private final fields
    // TODO 1b: protected constructor with validation
    // TODO 1c: final getChannelName()
    // TODO 1d: final getMaxMessageLength()
    // TODO 1e: final prepare(String message)  — the template method
    // TODO 1f: abstract send(String recipient, String message)
    static abstract class Notification {
        private final String channelName;
        private final int maxMessageLength;

        protected Notification(String channelName, int maxMessageLength){
            if(channelName == null || channelName.isBlank()){
                throw new IllegalArgumentException("Channel name cannot be blank");
            }
            if(maxMessageLength < 10) {
                throw new IllegalArgumentException("Max Message length must be >= 10");
            }
            this.channelName = channelName;
            this.maxMessageLength = maxMessageLength;
        }

        public final String getChannelName(){ return channelName; }
        public final int getMaxMessageLength() { return maxMessageLength; }
        public final String prepare(String message){
            if(message == null || message.isBlank()) {
                return "";
            } else if (message.length() > maxMessageLength){
                return message.substring(0, maxMessageLength);
            } else {
                return message;
            }
        }

        public abstract boolean send(String recipient, String message);
    }

    // =========================================================================
    // ── TODO 2: EmailNotification extends Notification ───────────────────────
    // Constructor: calls super("SMS", 160)
    // send(recipient, message):
    //   • recipient null/blank → return false
    //   • prepared = prepare(message)
    //   • System.out.println("[EMAIL] To: " + recipient + " | " + prepared)
    //   • return true
    // =========================================================================

    // TODO 2: implement EmailNotification
    static class EmailNotification extends Notification {

        public EmailNotification(){
            super("SMS", 160);
        }

        @Override
        public boolean send(String recipient, String message){
            if(recipient == null || recipient.isBlank()) {
                return false;
            }
            String prepared = prepare(message);
            System.out.println("[EMAIL] To: " + recipient + " | " + prepared);
            return true;
        }
    }

    // =========================================================================
    // ── TODO 3: SMSNotification extends Notification ─────────────────────────
    // Constructor: calls super("SMS", 160)
    // send(recipient, message):
    //   • recipient null/blank → return false
    //   • prepared = prepare(message)    ← THIS IS THE LSP FIX: truncate, never throw
    //   • System.out.println("[SMS] To: " + recipient + " | " + prepared)
    //   • return true
    //
    // LSP CHECK: if message.length() > 160, send() must NOT throw — it must truncate.
    // Calling prepare() automatically truncates — so DO NOT add any length check.
    // =========================================================================

    // TODO 3: implement SMSNotification
    static class SMSNotification extends Notification {
        public SMSNotification(){
            super("SMS", 160);
        }

        @Override
        public boolean send(String recipient, String message){
            if(recipient == null || recipient.isBlank()) {
                return false;
            }
            String prepared = prepare(message);
            System.out.println("[SMS] To: " + recipient + " | " + prepared);
            return true;
        }
    }

    // =========================================================================
    // ── TODO 4: PushNotification extends Notification ────────────────────────
    // Constructor: calls super("SMS", 160)
    // send(recipient, message):
    //   • recipient null/blank → return false
    //   • prepared = prepare(message)
    //   • System.out.println("[PUSH] To: " + recipient + " | " + prepared)
    //   • return true
    //
    // LSP CHECK: maxMessageLength must be >= 10 — the constructor validates this.
    // =========================================================================

    // TODO 4: implement PushNotification
    static class PushNotification extends Notification {
        public PushNotification(){
            super("SMS", 160);
        }

        @Override
        public boolean send(String recipient, String message){
            if(recipient == null || recipient.isBlank()) {
                return false;
            }
            String prepared = prepare(message);
            System.out.println("[PUSH] To: " + recipient + " | " + prepared);
            return true;
        }

    }

    // =========================================================================
    // ── TODO 5: NotificationService ──────────────────────────────────────────
    // Field: private final List<Notification> channels = new ArrayList<>()
    // Methods:
    //   register(Notification n)
    //     • null → throw IAE("Notification cannot be null")
    //     • channels.add(n)
    //
    //   broadcast(String recipient, String message) : Map<String, Boolean>
    //     • For each channel: result.put(channel.getChannelName(),
    //                                    channel.send(recipient, message))
    //     • Return Collections.unmodifiableMap(result)
    //     • NO instanceof checks allowed
    //
    //   getRegisteredChannels() : List<String>
    //     • Return list of getChannelName() from each registered channel (unmodifiable)
    // =========================================================================

    // TODO 5a: private final List<Notification> channels = new ArrayList<>()
    // TODO 5b: register(Notification n)
    // TODO 5c: broadcast(String recipient, String message) — no instanceof
    // TODO 5d: getRegisteredChannels()
    static class NotificationService {
        private final List<Notification> channels = new ArrayList<>();

        public void register(Notification n){
            if(n == null){
                throw new IllegalArgumentException("Notification cannot be null");
            }
            channels.add(n);
        }

        public Map<String, Boolean> broadcast(String recipient, String message){
            Map<String, Boolean> result = new HashMap<>();
            for(Notification channel : channels) {
                result.put(channel.getChannelName(), channel.send(recipient, message));
            }
            return Collections.unmodifiableMap(result);
        }

        public List<String> getRegisteredChannels() {
            List<String> registeredChannels = new ArrayList<>();
            for(Notification n : channels){
                registeredChannels.add(n.getChannelName());
            }
            return Collections.unmodifiableList(registeredChannels);
        }
    }
    // =========================================================================
    // LSP PROOF METHOD — do not modify; call from Test 7 ─────────────────────
    // This method represents "code that works with any Notification supertype".
    // If a subtype violates LSP, one of these assertions will fail or throw.
    // =========================================================================

    static void assertLSP(Object notificationObj) {
        // Use reflection-free assertions on the contracts via the abstract class API.
        // We receive Object here intentionally — in real code this would be Notification.
        // Uncomment the body once Notification is implemented:
        //
         Notification n = (Notification) notificationObj;
        //
        // Contract 1: getChannelName() must return non-null, non-blank
         String name = n.getChannelName();
         System.out.println("  channelName: '" + name + "' — " +
                 (name != null && !name.isBlank() ? "✅ OK" : "❌ VIOLATED"));

        // Contract 2: getMaxMessageLength() must return >= 10
         int maxLen = n.getMaxMessageLength();
         System.out.println("  maxLen: " + maxLen + " — " +
                 (maxLen >= 10 ? "✅ OK" : "❌ VIOLATED (invariant broken)"));

        // Contract 3: send() with valid input returns boolean without throwing
         boolean result = n.send("test@example.com", "Hello world");
         System.out.println("  send(valid): " + result + " — ✅ did not throw");
        //
        // Contract 4: send() with a very long message must NOT throw (must truncate)
         String longMsg = "A".repeat(1000);
         boolean longResult = n.send("test@example.com", longMsg);
         System.out.println("  send(1000-char msg): " + longResult + " — ✅ truncated, did not throw");
        //
        // Contract 5: send() with null message must NOT throw (prepare() handles it)
         boolean nullResult = n.send("test@example.com", null);
         System.out.println("  send(null msg): " + nullResult + " — ✅ handled gracefully");
        //
        // Contract 6: send() with blank recipient must return false (no exception)
         boolean blankRecip = n.send("", "message");
         System.out.println("  send(blank recipient): " + blankRecip + " — " +
                 (!blankRecip ? "✅ returned false" : "❌ should return false"));
        System.out.println("  [assertLSP] Uncomment body after implementing Notification");
    }

    // =========================================================================
    // DO NOT MODIFY — pre-written tests; fill in TODOs above to make them pass
    // =========================================================================

    public static void main(String[] args) {

        // Setup — uncomment after implementing your classes:
         EmailNotification email = new EmailNotification();
         SMSNotification   sms   = new SMSNotification();
         PushNotification  push  = new PushNotification();
         NotificationService service = new NotificationService();
         service.register(email);
         service.register(sms);
         service.register(push);

        System.out.println("═══ Test 1: Notification construction validation ═════════");
        // try {
        //     new EmailNotification() {};  // abstract — test via concrete subclass
        // } catch ...
        // Test via the abstract constructor validation (channelName blank):
        // Notification.constructor is protected — we test it via a concrete subclass
        // that passes a bad value to super(). We'll test via EmailNotification's super call.
        // Instead, test field invariants directly:
         EmailNotification e = new EmailNotification();
         System.out.println("channelName : " + e.getChannelName());   // EMAIL
         System.out.println("maxMsgLen   : " + e.getMaxMessageLength()); // 500
         System.out.println("Test 1 PASSED");

        System.out.println("\n═══ Test 2: EmailNotification — normal send ═════════════");
         boolean result = email.send("alice@example.com", "Hello Alice!");
         System.out.println("sent: " + result);       // true
         System.out.println("Test 2 PASSED: " + result);

        System.out.println("\n═══ Test 3: EmailNotification — blank recipient = false ══");
         boolean blankRecip = email.send("", "Hello");
         System.out.println("blank recipient: " + blankRecip); // false
         System.out.println("Test 3 PASSED: " + !blankRecip);

        System.out.println("\n═══ Test 4: SMSNotification — truncates long message (LSP) ═");
        // CRITICAL: must NOT throw; must truncate to 160 chars
         String longMsg = "X".repeat(200);
         boolean smsResult = sms.send("+91-9876543210", longMsg);
         System.out.println("sent (200-char): " + smsResult);   // true (truncated)
        // // Verify truncation via prepare():
         String prepared = sms.prepare(longMsg);  // we call it directly to inspect
        // Wait — prepare() is final on Notification, so subclass can expose it? No.
        // Actually we test this via send() not throwing:
         System.out.println("Test 4 PASSED — no exception thrown for 200-char SMS");

        System.out.println("\n═══ Test 5: PushNotification — sends successfully ════════");
         boolean pushResult = push.send("device-token-abc123", "You have a new message!");
         System.out.println("sent: " + pushResult);   // true
         System.out.println("Test 5 PASSED: " + pushResult);

        System.out.println("\n═══ Test 6: NotificationService — getRegisteredChannels ══");
         java.util.List<String> channels = service.getRegisteredChannels();
         System.out.println("channels: " + channels);  // [EMAIL, SMS, PUSH] (order may vary)
         System.out.println("count: " + channels.size());   // 3
         System.out.println("Test 6 PASSED: " + (channels.size() == 3));

        System.out.println("\n═══ Test 7: LSP proof — assertLSP on all types ══════════");
         System.out.println("[EmailNotification]:");
         assertLSP(email);
         System.out.println("[SMSNotification]:");
         assertLSP(sms);
         System.out.println("[PushNotification]:");
         assertLSP(push);
         System.out.println("Test 7 PASSED — all channels satisfy the Notification contract");
        assertLSP(null);  // placeholder until implemented

        System.out.println("\n═══ Test 8: broadcast — all channels receive the message ═");
         java.util.Map<String, Boolean> results =
                 service.broadcast("user@example.com", "Flash sale starts NOW!");
         System.out.println("broadcast results: " + results);
         System.out.println("EMAIL sent: " + results.get("EMAIL")); // true
         System.out.println("SMS   sent: " + results.get("SMS"));   // true
         System.out.println("PUSH  sent: " + results.get("PUSH"));  // true
         System.out.println("Test 8 PASSED");

        System.out.println("\n═══ Test 9: broadcast — very long message (LSP safety) ══");
        // A 1000-char message should be handled by ALL channels without throwing.
        // Each channel truncates to its own maxMessageLength.
         String bigMsg = "B".repeat(1000);
         java.util.Map<String, Boolean> bigResults = service.broadcast("user@example.com", bigMsg);
         System.out.println("All delivered without exception: "
                 + bigResults.values().stream().allMatch(v -> v));
         System.out.println("Test 9 PASSED");

        System.out.println("\n═══ Test 10: prepare() truncates exactly at maxMessageLength ═");
         EmailNotification e2 = new EmailNotification();
         String msg500 = "C".repeat(600);
         prepared = e2.prepare(msg500);
         System.out.println("prepared length: " + prepared.length()); // 500
         System.out.println("Test 10 PASSED: " + (prepared.length() == 500));
        // ---
         SMSNotification sms2 = new SMSNotification();
         String msg200 = "D".repeat(200);
         String smsPrepared = sms2.prepare(msg200);
         System.out.println("SMS prepared length: " + smsPrepared.length()); // 160
         System.out.println("Test 10b PASSED: " + (smsPrepared.length() == 160));

        System.out.println("\n[Uncomment the test code above after implementing your classes]");
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * HINTS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle) — Focus on CONTRACTS, not just types.
 *   The question is not "does this compile?" but "does every subtype honour
 *   every promise the parent makes?"
 *   For SMSNotification: the parent contract says send() never throws for any
 *   non-null message. An SMS only supports 160 chars. The LSP-compliant approach
 *   is to TRUNCATE the message before sending — not to throw an exception.
 *   For PushNotification: the parent guarantees getMaxMessageLength() >= 10.
 *   Never pass a value < 10 to the constructor, or the constructor will reject it.
 *   For assertLSP(): it calls send() with a 1000-char message — this should
 *   return true (truncated and sent), never throw.
 *
 * HINT 2 (Direct) — Implementation pointers:
 *   • Notification constructor: validate with throw IAE; mark getChannelName(),
 *     getMaxMessageLength(), prepare() as final so subclasses can't break invariants
 *   • prepare() : if (message == null) return ""; else use message.substring(0, maxMessageLength)
 *     with a length check to avoid StringIndexOutOfBoundsException
 *   • SMSNotification.send(): just call prepare(message) — NEVER add a length check
 *     that throws; prepare() handles truncation for you
 *   • PushNotification: super("PUSH", 100) — 100 >= 10, invariant satisfied
 *   • NotificationService.broadcast(): Map<String, Boolean> result = new HashMap<>();
 *     for (Notification ch : channels) result.put(ch.getChannelName(), ch.send(...));
 *     return Collections.unmodifiableMap(result);
 *
 * HINT 3 (Near-solution) — Class skeletons without method bodies:
 *
 *   abstract static class Notification {
 *       private final String channelName;
 *       private final int    maxMessageLength;
 *       protected Notification(String channelName, int maxMessageLength) {
 *           // validate: channelName blank → IAE; maxMessageLength < 10 → IAE
 *           this.channelName = channelName;
 *           this.maxMessageLength = maxMessageLength;
 *       }
 *       public final String  getChannelName()      { return channelName; }
 *       public final int     getMaxMessageLength() { return maxMessageLength; }
 *       public final String  prepare(String message) {
 *           if (message == null) return "";
 *           return message.length() > maxMessageLength
 *                  ? message.substring(0, maxMessageLength) : message;
 *       }
 *       public abstract boolean send(String recipient, String message);
 *   }
 *
 *   static class EmailNotification extends Notification {
 *       public EmailNotification() { super("EMAIL", 500); }
 *       @Override public boolean send(String recipient, String message) { ... }
 *   }
 *
 *   static class SMSNotification extends Notification {
 *       public SMSNotification() { super("SMS", 160); }
 *       @Override public boolean send(String recipient, String message) {
 *           if (recipient == null || recipient.isBlank()) return false;
 *           String prepared = prepare(message);   // ← handles truncation — NO throw
 *           System.out.println("[SMS] To: " + recipient + " | " + prepared);
 *           return true;
 *       }
 *   }
 *
 *   static class PushNotification extends Notification {
 *       public PushNotification() { super("PUSH", 100); }  // 100 >= 10 — invariant safe
 *       @Override public boolean send(String recipient, String message) { ... }
 *   }
 *
 *   static class NotificationService {
 *       private final List<Notification> channels = new ArrayList<>();
 *       public void register(Notification n) { ... }
 *       public Map<String, Boolean> broadcast(String recipient, String message) { ... }
 *       public List<String> getRegisteredChannels() { ... }
 *   }
 */

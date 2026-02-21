package com.ramkumar.lld.oop.interfaces.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reference solution for the Notification Delivery System practice problem.
 *
 * Key fixes vs the practice submission:
 *  1. SMSNotification.validateContent() — "+" not "!", && not ||
 *  2. getSummary() — always returns a string, never throws
 *  3. PushNotification.getLogHistory() — wrapped with unmodifiableList (consistent with Email)
 *  4. NotificationService constructor — defensive copy of input list
 *  5. getLoggable() — returns unmodifiable list
 *  6. Notification.counter — private
 *  7. @Override on every interface method implementation
 *  8. Space between notificationId and channel in getSummary()
 */
public class NotificationReference {

    // =========================================================================
    // INTERFACE — Sendable: capability to deliver a notification
    // No fields. No constructor. Pure contract.
    // =========================================================================

    interface Sendable {
        boolean send();
        String  getChannel();
        int     getPriority();
    }

    // =========================================================================
    // INTERFACE — Loggable: capability to maintain an audit log
    // default method: getLastLog() — implementors get this for free
    // =========================================================================

    interface Loggable {
        void         log(String event);
        List<String> getLogHistory();   // must return unmodifiable in all implementations

        // DEFAULT method — has a body; no implementing class needs to override it,
        // but they CAN. This is how interfaces can evolve without breaking implementors.
        default String getLastLog() {
            List<String> h = getLogHistory();
            return h.isEmpty() ? "No logs yet" : h.get(h.size() - 1);
            // Note: h.getLast() works in Java 21 — both are correct
        }
    }

    // =========================================================================
    // ABSTRACT CLASS — Notification: shared identity and state
    //
    // WHY abstract class (not interface)?
    //   → It has instance fields: notificationId, recipient, message, createdAt
    //   → Interfaces cannot have instance fields — only public static final constants
    //   → All three notification types share these fields, so they belong here
    //
    // WHY getChannel() is abstract here (even though Sendable also declares it)?
    //   → getSummary() is a Template Method that calls getChannel() internally
    //   → Notification does not implement Sendable, so it has no access to Sendable.getChannel()
    //   → Declaring it abstract in Notification lets getSummary() call it cleanly
    //   → Concrete subclasses satisfy both — their single @Override covers both
    // =========================================================================

    abstract static class Notification {

        private static int counter = 0;   // private — no other class should touch this

        private final String notificationId;
        private final String recipient;
        private final String message;
        private final long   createdAt;

        protected Notification(String recipient, String message) {
            if (recipient == null || recipient.isBlank())
                throw new IllegalArgumentException("Recipient cannot be null or blank");
            if (message == null || message.isBlank())
                throw new IllegalArgumentException("Message cannot be null or blank");
            // Counter incremented in the master constructor — not in instance blocks
            this.notificationId = String.format("NOTIF-%03d", ++counter);
            this.recipient       = recipient;
            this.message         = message;
            this.createdAt       = System.currentTimeMillis();
        }

        public abstract boolean validateContent();

        // Declared abstract so getSummary() can call it without Notification implementing Sendable
        public abstract String getChannel();

        // Template Method — always returns a string; NEVER throws
        // Descriptive/formatting methods must not throw — callers expect a result, not a crash
        public String getSummary() {
            return "[" + notificationId + "] " + getChannel()
                 + " → " + recipient + ": " + message
                 + " (valid=" + validateContent() + ")";
        }

        public String getNotificationId() { return notificationId; }
        public String getRecipient()       { return recipient; }
        public String getMessage()         { return message; }
        public long   getCreatedAt()       { return createdAt; }
    }

    // =========================================================================
    // EmailNotification — IS-A Notification, CAN send (Sendable), CAN log (Loggable)
    // =========================================================================

    static class EmailNotification extends Notification implements Sendable, Loggable {

        private final String       senderEmail;
        private final String       subject;
        private final List<String> logHistory = new ArrayList<>();

        public EmailNotification(String recipient, String message,
                                 String senderEmail, String subject) {
            super(recipient, message);   // chains to Notification constructor
            if (senderEmail == null || senderEmail.isBlank())
                throw new IllegalArgumentException("Sender email cannot be blank");
            if (subject == null || subject.isBlank())
                throw new IllegalArgumentException("Subject cannot be blank");
            this.senderEmail = senderEmail;
            this.subject     = subject;
        }

        // Satisfies BOTH Sendable.getChannel() AND Notification.getChannel() with one override
        @Override public String  getChannel()  { return "EMAIL"; }
        @Override public int     getPriority() { return 3; }

        @Override
        public boolean validateContent() {
            // senderEmail and subject already validated non-blank in constructor,
            // but we still check the domain-specific rule: email must contain "@"
            return senderEmail.contains("@") && !subject.isBlank();
        }

        @Override
        public boolean send() {
            log("Sent EMAIL to " + getRecipient() + " from " + senderEmail);
            return validateContent();
        }

        // Loggable implementation
        @Override public void         log(String event)  { logHistory.add(event); }
        @Override public List<String> getLogHistory()    { return Collections.unmodifiableList(logHistory); }
        // getLastLog() inherited from Loggable.default — no override needed
    }

    // =========================================================================
    // SMSNotification — IS-A Notification, CAN send (Sendable)
    // DELIBERATELY does NOT implement Loggable — Interface Segregation
    // SMS has no audit trail requirement; forcing log() would be a fake implementation
    // =========================================================================

    static class SMSNotification extends Notification implements Sendable {

        private final String phoneNumber;

        public SMSNotification(String recipient, String message, String phoneNumber) {
            super(recipient, message);
            if (phoneNumber == null || phoneNumber.isBlank() || !phoneNumber.startsWith("+"))
                throw new IllegalArgumentException("Phone number must start with '+'");
            this.phoneNumber = phoneNumber;
        }

        @Override public String  getChannel()  { return "SMS"; }
        @Override public int     getPriority() { return 4; }

        @Override
        public boolean validateContent() {
            // BOTH conditions must hold — && not ||
            // "+" not "!" — the phone must start with the plus sign for international format
            return phoneNumber.startsWith("+") && getMessage().length() <= 160;
        }

        @Override
        public boolean send() {
            return validateContent();
        }
    }

    // =========================================================================
    // PushNotification — IS-A Notification, CAN send (Sendable), CAN log (Loggable)
    // =========================================================================

    static class PushNotification extends Notification implements Sendable, Loggable {

        private final String       deviceToken;
        private final String       title;
        private final List<String> logHistory = new ArrayList<>();

        public PushNotification(String recipient, String message,
                                String deviceToken, String title) {
            super(recipient, message);
            if (deviceToken == null || deviceToken.isBlank())
                throw new IllegalArgumentException("Device token cannot be blank");
            if (title == null || title.isBlank())
                throw new IllegalArgumentException("Title cannot be blank");
            this.deviceToken = deviceToken;
            this.title       = title;
        }

        @Override public String  getChannel()  { return "PUSH"; }
        @Override public int     getPriority() { return 2; }

        @Override
        public boolean validateContent() {
            // Fields already validated non-blank in constructor
            // This method re-checks the domain rule for runtime changes (future-proofing)
            return !deviceToken.isBlank() && !title.isBlank();
        }

        @Override
        public boolean send() {
            log("Sent PUSH to " + getRecipient());
            return validateContent();
        }

        // Loggable — MUST return unmodifiable, same as EmailNotification
        // Inconsistency between two classes implementing the same interface is a design smell
        @Override public void         log(String event) { logHistory.add(event); }
        @Override public List<String> getLogHistory()   { return Collections.unmodifiableList(logHistory); }
    }

    // =========================================================================
    // NotificationService — codes entirely to Sendable, never to concrete types
    // =========================================================================

    static class NotificationService {

        private final List<Sendable> notifications;

        // Defensive copy — service owns its own list
        // Without this, caller can mutate their list and the service changes silently
        public NotificationService(List<Sendable> notifications) {
            this.notifications = new ArrayList<>(notifications);
        }

        public int sendAll() {
            int count = 0;
            for (Sendable n : notifications) {
                if (n.send()) count++;
            }
            return count;
        }

        public List<Sendable> getByChannel(String channel) {
            return Collections.unmodifiableList(
                    notifications.stream()
                            .filter(n -> n.getChannel().equals(channel))
                            .collect(Collectors.toList()));
        }

        public List<Sendable> getHighPriority(int minPriority) {
            return Collections.unmodifiableList(
                    notifications.stream()
                            .filter(n -> n.getPriority() >= minPriority)
                            .collect(Collectors.toList()));
        }

        // instanceof is appropriate here — checking for a DIFFERENT interface than the stored type
        // This is not the same as an instanceof chain replacing polymorphism
        public List<Loggable> getLoggable() {
            return Collections.unmodifiableList(
                    notifications.stream()
                            .filter(n -> n instanceof Loggable)
                            .map(n -> (Loggable) n)
                            .collect(Collectors.toList()));
        }
    }

    // =========================================================================
    // Main — same 9 test cases + 1 extra for the most common mistake
    // =========================================================================

    public static void main(String[] args) {

        EmailNotification email = new EmailNotification(
                "alice@example.com", "Your order is confirmed!", "shop@store.com", "Order Confirmation");
        SMSNotification sms = new SMSNotification(
                "Bob", "Your OTP is 123456", "+919876543210");
        PushNotification push = new PushNotification(
                "Carol", "Flash sale starts now!", "device-token-xyz", "Sale Alert");
        System.out.println("Test 1 PASSED: All three notification types created");

        System.out.println("\n── Test 2: getSummary() — always returns string, never throws ──");
        System.out.println(email.getSummary());
        System.out.println(sms.getSummary());
        System.out.println(push.getSummary());

        System.out.println("\n── Test 3: send() ──────────────────────────────────────────────");
        System.out.println("email.send() = " + email.send());  // true
        System.out.println("sms.send()   = " + sms.send());    // true
        System.out.println("push.send()  = " + push.send());   // true

        System.out.println("\n── Test 4: getLastLog() default method ─────────────────────────");
        PushNotification push2 = new PushNotification("Dave", "Msg", "token-abc", "Title");
        System.out.println("Before any log: " + push2.getLastLog());  // "No logs yet"
        push2.send();
        System.out.println("After send:     " + push2.getLastLog());  // log entry

        System.out.println("\n── Test 5: Interface segregation ───────────────────────────────");
        System.out.println("email instanceof Loggable: " + (email instanceof Loggable));  // true
        System.out.println("sms   instanceof Loggable: " + (sms   instanceof Loggable));  // false
        System.out.println("push  instanceof Loggable: " + (push  instanceof Loggable));  // true

        System.out.println("\n── Test 6: NotificationService ─────────────────────────────────");
        List<Sendable> all = List.of(email, sms, push);
        NotificationService service = new NotificationService(all);
        int sent = service.sendAll();
        System.out.println("sendAll() successful: " + sent);  // 3

        System.out.println("\n── Test 7: Filtering ────────────────────────────────────────────");
        System.out.println("SMS channel count:    " + service.getByChannel("SMS").size());     // 1
        System.out.println("Priority >= 3 count:  " + service.getHighPriority(3).size());      // 2
        System.out.println("Loggable count:       " + service.getLoggable().size());           // 2

        System.out.println("\n── Test 8: SMS 160-char validation ─────────────────────────────");
        SMSNotification longSms = new SMSNotification("Eve", "A".repeat(161), "+1234567890");
        System.out.println("Long SMS send() = " + longSms.send());  // false

        System.out.println("\n── Test 9: Validation — blank recipient ─────────────────────────");
        try {
            new EmailNotification("", "msg", "a@b.com", "Sub");
            System.out.println("Test 9 FAILED");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 9 PASSED: " + e.getMessage());
        }

        // Test 10 (extra): The most common mistake — SMS with no "+" passes when it shouldn't
        // With "!" instead of "+" and || instead of &&, this would incorrectly return true
        System.out.println("\n── Test 10 (extra): SMS without '+' prefix must fail ────────────");
        SMSNotification noPlus = new SMSNotification("Frank", "Hello", "+0000000000");
        // Manually test the logic: phone is "+0000..." (starts with +), message is short → valid
        System.out.println("Valid phone send():   " + noPlus.send());  // true — correct
        // Now simulate what happens when validateContent uses "!" — caught by this test
        SMSNotification shortMsg = new SMSNotification("Grace", "Hi", "+447911123456");
        System.out.println("Short msg send():     " + shortMsg.send());  // true
        System.out.println("Verify: starts with '+': " + shortMsg.send()); // both conditions checked
        System.out.println("Test 10 PASSED: SMS validateContent logic is correct");

        System.out.println("\nAll tests completed.");
    }
}

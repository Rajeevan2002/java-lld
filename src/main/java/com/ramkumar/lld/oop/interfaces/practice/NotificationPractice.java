package com.ramkumar.lld.oop.interfaces.practice;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *  PRACTICE: Notification Delivery System
 * ============================================================
 *
 * Problem Statement:
 * ------------------
 * Design a Notification Delivery System that models three types
 * of notifications. The system must demonstrate the correct choice
 * between interfaces and abstract classes, multiple interface
 * implementation, default methods, and coding to interface.
 *
 * ── Interface 1: Sendable ──────────────────────────────────────
 * Represents any object that can be delivered to a recipient.
 *
 * Methods:
 *   1. boolean send()        — attempt delivery; return true on success
 *   2. String getChannel()   — return delivery channel: "EMAIL", "SMS", or "PUSH"
 *   3. int getPriority()     — return 1 (low) to 5 (critical)
 *
 * ── Interface 2: Loggable ─────────────────────────────────────
 * Represents any object that maintains an audit log of events.
 *
 * Methods:
 *   1. void log(String event)          — add an event string to the history
 *   2. List<String> getLogHistory()    — return an UNMODIFIABLE list of events
 *   3. default String getLastLog()     — return the last entry, or "No logs yet"
 *                                        if history is empty (implement as default)
 *
 * ── Abstract Class: Notification ──────────────────────────────
 * Shared identity and state for all notification types.
 * WHY abstract class (not interface): it has instance fields.
 *
 * Fields (all immutable — final):
 *   1. notificationId (String) — auto-generated: "NOTIF-001", "NOTIF-002", ...
 *   2. recipient      (String) — who receives it; validate not null/blank
 *   3. message        (String) — content; validate not null/blank
 *   4. createdAt      (long)   — System.currentTimeMillis() at construction
 *
 * Static field:
 *   - counter (int) for generating sequential notification IDs
 *
 * Constructor:
 *   - Notification(recipient, message) — validates both; auto-generates notificationId
 *   - All concrete subclasses must chain via super()
 *
 * Methods:
 *   1. abstract boolean validateContent() — subclass validates its own extra fields
 *   2. String getSummary()               — concrete; format:
 *                                          "[NOTIF-001] EMAIL → alice@x.com: Hello"
 *                                          Uses getChannel() and validateContent() internally.
 *                                          (Template Method — calls abstract methods)
 *   3. Standard getters for all fields
 *
 * ── Class 1: EmailNotification extends Notification
 *             implements Sendable, Loggable ──────────────────────
 * Additional fields (all final):
 *   1. senderEmail (String) — validate it contains "@"
 *   2. subject     (String) — validate not blank
 *
 * Constructor: EmailNotification(recipient, message, senderEmail, subject)
 *   → chain to super(recipient, message)
 *
 * Implement:
 *   - getChannel()        → "EMAIL"
 *   - getPriority()       → 3
 *   - validateContent()   → true if senderEmail contains "@" AND subject not blank
 *   - send()              → logs "Sent EMAIL to <recipient>", returns validateContent()
 *   - log(event)          → adds to internal ArrayList
 *   - getLogHistory()     → returns Collections.unmodifiableList(...)
 *
 * ── Class 2: SMSNotification extends Notification
 *             implements Sendable ─────────────────────────────────
 * NOTE: SMSNotification does NOT implement Loggable — it has no audit trail.
 * This demonstrates Interface Segregation: don't force classes to implement
 * methods they don't need.
 *
 * Additional field (final):
 *   1. phoneNumber (String) — validate it starts with "+"
 *
 * Constructor: SMSNotification(recipient, message, phoneNumber)
 *   → chain to super(recipient, message)
 *
 * Implement:
 *   - getChannel()       → "SMS"
 *   - getPriority()      → 4
 *   - validateContent()  → true if phoneNumber starts with "+" AND message.length() <= 160
 *   - send()             → returns validateContent()
 *
 * ── Class 3: PushNotification extends Notification
 *             implements Sendable, Loggable ─────────────────────────
 * Additional fields (all final):
 *   1. deviceToken (String) — validate not blank
 *   2. title       (String) — validate not blank
 *
 * Constructor: PushNotification(recipient, message, deviceToken, title)
 *   → chain to super(recipient, message)
 *
 * Implement:
 *   - getChannel()       → "PUSH"
 *   - getPriority()      → 2
 *   - validateContent()  → true if deviceToken and title are not blank
 *   - send()             → logs "Sent PUSH to <recipient>", returns validateContent()
 *   - log(event)         → adds to internal ArrayList
 *   - getLogHistory()    → returns Collections.unmodifiableList(...)
 *
 * ── Class 4: NotificationService ─────────────────────────────────
 * Works ONLY with Sendable — it has no knowledge of concrete types.
 *
 * Fields:
 *   1. notifications — List<Sendable> (private, set via constructor)
 *
 * Constructor: NotificationService(List<Sendable> notifications)
 *
 * Methods:
 *   1. int sendAll()
 *      → calls send() on each; counts and returns successful sends (send() == true)
 *   2. List<Sendable> getByChannel(String channel)
 *      → returns unmodifiable list of notifications matching the channel
 *   3. List<Sendable> getHighPriority(int minPriority)
 *      → returns unmodifiable list where getPriority() >= minPriority
 *   4. List<Loggable> getLoggable()
 *      → returns only those notifications that also implement Loggable
 *        (one instanceof check is acceptable here — checking for a different interface)
 *
 * Design Constraints:
 * -------------------
 *  - Sendable and Loggable MUST be interfaces (not abstract classes)
 *  - Notification MUST be abstract (cannot be instantiated directly)
 *  - NotificationService stores List<Sendable>, never a concrete type
 *  - getLogHistory() must return an UNMODIFIABLE list
 *  - getLastLog() MUST be implemented as a default method in Loggable
 *  - getSummary() MUST call getChannel() and validateContent() (Template Method)
 *  - SMSNotification must NOT implement Loggable
 *
 * ============================================================
 *  Write your solution below. Delete this comment block when done.
 * ============================================================
 */
public class NotificationPractice {

    // =========================================================================
    // ── TODO 1: Declare interface Sendable ───────────────────────────────────
    //    Methods: send(), getChannel(), getPriority()
    //    No fields, no constructor.
    // =========================================================================
    interface Sendable {
        boolean send();
        String getChannel();
        int getPriority();
    }

    // =========================================================================
    // ── TODO 2: Declare interface Loggable ───────────────────────────────────
    //    Methods: log(String), getLogHistory()
    //    Default method: getLastLog() → last entry or "No logs yet"
    // =========================================================================
    interface Loggable {

        void log(String event);
        List<String> getLogHistory();
        default String getLastLog(){
            if(getLogHistory().isEmpty()){
                return "No logs yet";
            }
            return getLogHistory().getLast();
        }
    }

    // =========================================================================
    // ── TODO 3: Declare abstract class Notification ───────────────────────────
    //    Fields: notificationId (final), recipient (final), message (final),
    //            createdAt (final), static counter
    //    Constructor: validates recipient + message; auto-generates ID
    //    Abstract: validateContent()
    //    Concrete: getSummary() — calls getChannel() + validateContent()
    //    Getters for all fields
    // =========================================================================
    static abstract class Notification {
        private final String notificationId;
        private final String recipient;
        private final String message;
        private final long createdAt;

        private static int counter = 0;

        public Notification(String recipient, String message){
            if(recipient == null || recipient.isBlank()){
                throw new IllegalArgumentException("Illegal Argument for Recipient, cannot null or blank");
            }

            if(message == null || message.isBlank()){
                throw new IllegalArgumentException("Illegal Argument for Message, cannot null or blank");
            }
            ++counter;
            this.notificationId = String.format("[NOTIF-%03d]", counter);
            this.recipient = recipient;
            this.message = message;
            this.createdAt = Instant.now().toEpochMilli();
        }

        public abstract boolean validateContent();

        public abstract String getChannel();

        public String getSummary(){
            return "[" + notificationId + "] " + getChannel()
                    + " → " + recipient + ": " + message
                    + " (valid=" + validateContent() + ")";
        }

        public String getNotificationId() { return notificationId;}
        public String getRecipient() { return recipient;}
        public String getMessage() { return message;}
        public long getCreatedAt() { return createdAt;}

    }

    // =========================================================================
    // ── TODO 4: Implement EmailNotification ──────────────────────────────────
    //    extends Notification, implements Sendable, Loggable
    //    Fields: senderEmail (final), subject (final)
    //    Constructor: chains to super()
    //    getChannel() → "EMAIL" | getPriority() → 3
    //    validateContent(): senderEmail has "@" AND subject not blank
    //    send(): logs event, returns validateContent()
    //    log() / getLogHistory()
    // =========================================================================
    static class EmailNotification extends Notification implements Loggable, Sendable {
        private final String senderEmail;
        private final String subject;
        private final List<String> logs;


        public EmailNotification(String recipient, String message,
                                 String senderEmail, String subject){
            super(recipient, message);
            if(senderEmail == null || senderEmail.isBlank()){
                throw new IllegalArgumentException("Sender Email Cannot be Blank or NULL");
            }

            if(subject == null || subject.isBlank()){
                throw new IllegalArgumentException("subject Cannot be Blank or NULL");
            }
            this.senderEmail = senderEmail;
            this.subject = subject;
            this.logs = new ArrayList<>();
        }

        @Override
        public String getChannel(){ return "EMAIL"; }


        @Override
        public int getPriority(){ return 3; }

        @Override
        public boolean validateContent() {
            return senderEmail.contains("@") && (subject != null && !subject.isBlank());
        }

        @Override
        public void log(String event){
            System.out.println(event);
            logs.add(event);
        }

        @Override
        public List<String> getLogHistory(){
            return Collections.unmodifiableList(logs);
        }

        @Override
        public boolean send(){
            log("Sent Email to : " + getRecipient() + "from : " + senderEmail);
            return  validateContent();
        }
    }

    // =========================================================================
    // ── TODO 5: Implement SMSNotification ────────────────────────────────────
    //    extends Notification, implements Sendable ONLY (not Loggable)
    //    Field: phoneNumber (final) — must start with "+"
    //    Constructor: chains to super()
    //    getChannel() → "SMS" | getPriority() → 4
    //    validateContent(): phoneNumber starts "+" AND message <= 160 chars
    //    send(): returns validateContent()
    // =========================================================================
    static class SMSNotification extends Notification implements Sendable {
        private final String phoneNumber;

        public SMSNotification(String recipient,
                               String message,
                               String phoneNumber){
            super(recipient, message);
            if(phoneNumber == null || phoneNumber.isBlank() || !phoneNumber.startsWith("+")){
                throw new IllegalArgumentException("Phone Number is a Mandatory Property and cannot be null and" +
                        " should start with '+'");
            }
            this.phoneNumber = phoneNumber;
        }

        @Override
        public boolean validateContent() {
            return (phoneNumber.startsWith("+") && getMessage().length() <= 160);
        }

        @Override public boolean send() { return validateContent(); }
        @Override public String getChannel() { return "SMS"; }
        @Override public int getPriority() { return  4; }

    }

    // =========================================================================
    // ── TODO 6: Implement PushNotification ───────────────────────────────────
    //    extends Notification, implements Sendable, Loggable
    //    Fields: deviceToken (final), title (final)
    //    Constructor: chains to super()
    //    getChannel() → "PUSH" | getPriority() → 2
    //    validateContent(): deviceToken + title not blank
    //    send(): logs event, returns validateContent()
    //    log() / getLogHistory()
    // =========================================================================
    static class PushNotification extends Notification implements Loggable, Sendable {
        private final String deviceToken;
        private final String title;
        private final List<String> logs;

        public PushNotification(String recipient,
                                String message,
                                String deviceToken,
                                String title){
            super(recipient, message);
            if(deviceToken == null || deviceToken.isBlank()){
                throw new IllegalArgumentException("Device Token Cannot be null or valid");
            }
            if(title == null || title.isBlank()){
                throw new IllegalArgumentException("Title Cannot be null or invalid");
            }
            this.deviceToken = deviceToken;
            this.title = title;
            this.logs = new ArrayList<>();
        }

        @Override
        public boolean validateContent() {
            return ((deviceToken != null && !deviceToken.isBlank()) && (title != null && !title.isBlank()));
        }

        @Override public List<String> getLogHistory() { return logs; }
        @Override public void log(String event){
            System.out.println(event);
            logs.add(event);
        }
        @Override public boolean send() {
            log("Sent PUSH to " + getRecipient());
            return validateContent();
        }

        @Override public String getChannel() { return "PUSH"; }
        @Override public int getPriority() { return 2; }
    }

    // =========================================================================
    // ── TODO 7: Implement NotificationService ────────────────────────────────
    //    Field: List<Sendable> notifications (private)
    //    Constructor: takes List<Sendable>
    //    sendAll() → count of successful send() calls
    //    getByChannel(String channel) → filtered unmodifiable list
    //    getHighPriority(int minPriority) → filtered unmodifiable list
    //    getLoggable() → List<Loggable> of those that implement Loggable
    // =========================================================================
    static class NotificationService {
        private List<Sendable> notifications;

        public NotificationService(List<Sendable> notifications){
            this.notifications = new ArrayList<>(notifications);
        }

        public int sendAll(){
            int successfulCalls = 0;
            for(Sendable notification : notifications){
                if(notification.send()){
                    successfulCalls +=1;
                }
            }
            return successfulCalls;
        }

        public List<Sendable> getByChannel(String channel){
            return Collections.unmodifiableList(notifications.stream()
                    .filter(notification -> notification.getChannel().equals(channel))
                    .toList());
        }

        public List<Sendable> getHighPriority(int minPriority){
            return Collections.unmodifiableList(notifications.stream()
                    .filter(notification -> notification.getPriority() >= minPriority)
                    .toList());
        }

        public List<Loggable> getLoggable(){
            List<Loggable> loggables = new ArrayList<>();
            for(Sendable sendable : notifications){
                if(sendable instanceof  Loggable){
                    loggables.add((Loggable) sendable);
                }
            }
            return Collections.unmodifiableList(loggables);
        }
    }

    // =========================================================================
    // Main — DO NOT MODIFY — implement the TODOs above to make these pass
    // =========================================================================

    public static void main(String[] args) {

        // Test 1: Create all three types — valid construction
        EmailNotification email = new EmailNotification(
                "alice@example.com", "Your order is confirmed!", "shop@store.com", "Order Confirmation");
        SMSNotification sms = new SMSNotification(
                "Bob", "Your OTP is 123456", "+919876543210");
        PushNotification push = new PushNotification(
                "Carol", "Flash sale starts now!", "device-token-xyz", "Sale Alert");
        System.out.println("Test 1 PASSED: All three notification types created");

        // Test 2: getSummary() uses Template Method (getChannel + validateContent)
        System.out.println("\n── Test 2: getSummary() ──────────────────────────────");
        System.out.println(email.getSummary());
        System.out.println(sms.getSummary());
        System.out.println(push.getSummary());

        // Test 3: send() and logging
        System.out.println("\n── Test 3: send() ────────────────────────────────────");
        System.out.println("email.send() = " + email.send());  // true
        System.out.println("sms.send()   = " + sms.send());    // true (valid phone)
        System.out.println("push.send()  = " + push.send());   // true

        // Test 4: Loggable default method — getLastLog()
        System.out.println("\n── Test 4: getLastLog() default method ───────────────");
        PushNotification push2 = new PushNotification("Dave", "Msg", "token-abc", "Title");
        System.out.println("Before any log: " + push2.getLastLog());   // "No logs yet"
        push2.send();
        System.out.println("After send: " + push2.getLastLog());       // log entry

        // Test 5: Interface segregation — SMS cannot be used as Loggable
        System.out.println("\n── Test 5: Interface segregation ─────────────────────");
        System.out.println("email instanceof Loggable: " + (email instanceof Loggable));  // true
        System.out.println("sms   instanceof Loggable: " + (sms   instanceof Loggable));  // false
        System.out.println("push  instanceof Loggable: " + (push  instanceof Loggable));  // true

        // Test 6: Coding to interface — NotificationService takes List<Sendable>
        System.out.println("\n── Test 6: NotificationService ───────────────────────");
        List<Sendable> all = List.of(email, sms, push);
        NotificationService service = new NotificationService(all);
        int sent = service.sendAll();
        System.out.println("sendAll() successful: " + sent);  // 3

        // Test 7: Filtering by channel and priority
        System.out.println("\n── Test 7: Filtering ─────────────────────────────────");
        List<Sendable> smsOnly  = service.getByChannel("SMS");
        List<Sendable> highPri  = service.getHighPriority(3);
        List<Loggable> loggable = service.getLoggable();
        System.out.println("SMS channel count: " + smsOnly.size());          // 1
        System.out.println("Priority >= 3 count: " + highPri.size());        // 2 (EMAIL=3, SMS=4)
        System.out.println("Loggable count: " + loggable.size());            // 2 (EMAIL + PUSH)

        // Test 8: SMSNotification fails validateContent with message > 160 chars
        System.out.println("\n── Test 8: SMS 160-char validation ───────────────────");
        String longMsg = "A".repeat(161);
        SMSNotification longSms = new SMSNotification("Eve", longMsg, "+1234567890");
        System.out.println("Long SMS send() = " + longSms.send());  // false

        // Test 9: Invalid construction — blank recipient throws
        System.out.println("\n── Test 9: Validation ────────────────────────────────");
        try {
            new EmailNotification("", "msg", "a@b.com", "Sub");
            System.out.println("Test 9 FAILED: blank recipient should throw");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 9 PASSED: " + e.getMessage());
        }

        System.out.println("\nAll tests completed.");
    }

    // =========================================================================
    // HINTS — read only when stuck
    // =========================================================================

    /*
     * ── HINT LEVEL 1 (Gentle) ─────────────────────────────────────────────────
     *
     *  - Why is Notification an abstract class and not an interface?
     *    Think: does it need to HOLD state (notificationId, recipient, message)?
     *    Interfaces cannot have instance fields.
     *
     *  - Why are Sendable and Loggable interfaces and not abstract classes?
     *    A class can only extends ONE abstract class. EmailNotification and
     *    PushNotification need BOTH capabilities — that's only possible with interfaces.
     *
     *  - Where does getLastLog() live and how is it declared?
     *    It's a method that any Loggable can use — but it's not abstract.
     *    Which kind of method lives in an interface but has a body?
     */

    /*
     * ── HINT LEVEL 2 (Direct) ─────────────────────────────────────────────────
     *
     *  - Sendable and Loggable are declared as `interface`, not `abstract class`.
     *
     *  - Notification is declared as `abstract static class Notification`.
     *    It has a static counter: `private static int counter = 0;`
     *    notificationId is generated as: `"NOTIF-" + String.format("%03d", ++counter)`
     *
     *  - getLastLog() is a `default` method in Loggable:
     *      default String getLastLog() {
     *          List<String> h = getLogHistory();
     *          return h.isEmpty() ? "No logs yet" : h.get(h.size() - 1);
     *      }
     *
     *  - getSummary() in Notification should look like:
     *      "[" + notificationId + "] " + getChannel() + " → " + recipient + ": " + message
     *      + " (valid=" + validateContent() + ")"
     *    getChannel() is declared in Sendable — but Notification doesn't implement Sendable.
     *    Hint: make Notification declare getChannel() as abstract too, OR let concrete classes
     *    expose it. The cleanest approach: declare it abstract in Notification so getSummary()
     *    can call it.
     *
     *  - NotificationService.getLoggable():
     *      return notifications.stream()
     *             .filter(n -> n instanceof Loggable)
     *             .map(n -> (Loggable) n)
     *             .collect(Collectors.toList());
     */

    /*
     * ── HINT LEVEL 3 (Near-Solution) ──────────────────────────────────────────
     *
     *  interface Sendable {
     *      boolean send();
     *      String getChannel();
     *      int getPriority();
     *  }
     *
     *  interface Loggable {
     *      void log(String event);
     *      List<String> getLogHistory();
     *      default String getLastLog() {
     *          List<String> h = getLogHistory();
     *          return h.isEmpty() ? "No logs yet" : h.get(h.size() - 1);
     *      }
     *  }
     *
     *  abstract static class Notification {
     *      private static int counter = 0;
     *      private final String notificationId;
     *      private final String recipient, message;
     *      private final long   createdAt;
     *
     *      protected Notification(String recipient, String message) {
     *          if (recipient == null || recipient.isBlank()) throw new IllegalArgumentException("...");
     *          if (message   == null || message.isBlank())   throw new IllegalArgumentException("...");
     *          this.notificationId = "NOTIF-" + String.format("%03d", ++counter);
     *          this.recipient = recipient;
     *          this.message   = message;
     *          this.createdAt = System.currentTimeMillis();
     *      }
     *
     *      public abstract boolean validateContent();
     *      public abstract String  getChannel();   // abstract here so getSummary() can call it
     *
     *      public String getSummary() {
     *          return "[" + notificationId + "] " + getChannel() + " → " + recipient + ": " + message;
     *      }
     *      // getters
     *  }
     *
     *  static class EmailNotification extends Notification implements Sendable, Loggable {
     *      private final String senderEmail, subject;
     *      private final List<String> logHistory = new ArrayList<>();
     *
     *      public EmailNotification(String recipient, String message, String senderEmail, String subject) {
     *          super(recipient, message);
     *          // validate senderEmail and subject
     *          this.senderEmail = senderEmail;
     *          this.subject = subject;
     *      }
     *      @Override public String  getChannel()       { return "EMAIL"; }
     *      @Override public int     getPriority()      { return 3; }
     *      @Override public boolean validateContent()  { return senderEmail.contains("@") && !subject.isBlank(); }
     *      @Override public boolean send()             { log("Sent EMAIL to " + getRecipient()); return validateContent(); }
     *      @Override public void    log(String event)  { logHistory.add(event); }
     *      @Override public List<String> getLogHistory() { return Collections.unmodifiableList(logHistory); }
     *  }
     *  // SMSNotification and PushNotification follow the same pattern.
     */
}

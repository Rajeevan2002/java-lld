package com.ramkumar.lld.designpatterns.creational.factorymethod.code;

/**
 * Scenario A — Notification Service
 *
 * Demonstrates the Factory Method pattern in a notification system that
 * supports Email, SMS, and Slack channels.
 *
 * ── Participants ────────────────────────────────────────────────────────────
 *   Product          →  Notification (interface)
 *   ConcreteProduct  →  EmailNotification, SmsNotification, SlackNotification
 *   Creator          →  NotificationSender (abstract class)
 *   ConcreteCreator  →  EmailSender, SmsSender, SlackSender
 * ────────────────────────────────────────────────────────────────────────────
 */
public class NotificationServiceDemo {

    // =========================================================================
    // PRODUCT — the interface every concrete notification must satisfy
    // =========================================================================

    // [Factory Method — Product interface]
    interface Notification {
        /** Delivers the message to the recipient. */
        void send(String recipient, String message);

        /** Human-readable channel name, e.g. "Email", "SMS". */
        String getChannel();

        /** Metadata tag identifying the sender config, e.g. smtp server or API key prefix. */
        String getSenderTag();
    }

    // =========================================================================
    // CONCRETE PRODUCTS
    // =========================================================================

    // [ConcreteProduct A] — Email channel
    static class EmailNotification implements Notification {
        private final String smtpServer;    // immutable — set at construction
        private final String fromAddress;   // immutable — set at construction

        EmailNotification(String smtpServer, String fromAddress) {
            this.smtpServer  = smtpServer;
            this.fromAddress = fromAddress;
        }

        // [Polymorphism] — EmailNotification-specific implementation of send()
        @Override
        public void send(String recipient, String message) {
            System.out.printf("  [Email] SMTP(%s) → FROM:%s TO:%s%n  MSG: %s%n",
                    smtpServer, fromAddress, recipient, message);
        }

        @Override public String getChannel()   { return "Email"; }
        @Override public String getSenderTag() { return "smtp:" + smtpServer; }
    }

    // [ConcreteProduct B] — SMS channel
    static class SmsNotification implements Notification {
        private final String apiKey;     // immutable — masked in tag for security
        private final String shortCode;  // immutable — the sender short code

        SmsNotification(String apiKey, String shortCode) {
            this.apiKey    = apiKey;
            this.shortCode = shortCode;
        }

        @Override
        public void send(String recipient, String message) {
            System.out.printf("  [SMS] Short-code:%s → TO:%s%n  MSG: %s%n",
                    shortCode, recipient, message);
        }

        @Override public String getChannel()   { return "SMS"; }
        // [Encapsulation] — API key is never fully exposed; only a prefix shown
        @Override public String getSenderTag() { return "sms:" + shortCode + "(" + apiKey.substring(0, 4) + "…)"; }
    }

    // [ConcreteProduct C] — Slack channel
    static class SlackNotification implements Notification {
        private final String webhookUrl;  // immutable
        private final String channel;     // immutable, e.g. "#alerts"

        SlackNotification(String webhookUrl, String channel) {
            this.webhookUrl = webhookUrl;
            this.channel    = channel;
        }

        @Override
        public void send(String recipient, String message) {
            System.out.printf("  [Slack] channel:%s → MENTION:@%s%n  MSG: %s%n",
                    channel, recipient, message);
        }

        @Override public String getChannel()   { return "Slack"; }
        @Override public String getSenderTag() { return "slack:" + channel; }
    }

    // =========================================================================
    // CREATOR — abstract class that owns the Factory Method
    //           and the business logic that uses the Product
    // =========================================================================

    // [Factory Method — Creator]
    static abstract class NotificationSender {

        // ── FACTORY METHOD (abstract) ─────────────────────────────────────────
        // Subclasses decide which Notification implementation to create.
        // This method is the "hinge" of the pattern.
        abstract Notification createNotification();

        // ── BUSINESS METHOD (concrete) ────────────────────────────────────────
        // Uses whatever Notification the subclass provides — completely decoupled
        // from the concrete type. This is the core benefit of Factory Method.
        final void sendAlert(String recipient, String message) {
            // [Factory Method — calling the factory]
            Notification notification = createNotification();

            System.out.printf("[%s] Sending alert to %s via %s%n",
                    notification.getSenderTag(), recipient, notification.getChannel());
            notification.send(recipient, message);   // [Polymorphism] correct impl called
            System.out.println();
        }

        // ── SECOND BUSINESS METHOD ────────────────────────────────────────────
        // Demonstrates that ANY business method can call createNotification()
        // and get the right product without any if/switch.
        final String describeChannel() {
            Notification notification = createNotification();
            return notification.getChannel() + " (tag: " + notification.getSenderTag() + ")";
        }
    }

    // =========================================================================
    // CONCRETE CREATORS — each one "overrides the decision" about which
    //                     concrete Notification to instantiate
    // =========================================================================

    // [ConcreteCreator A]
    static class EmailSender extends NotificationSender {
        private final String smtpServer;
        private final String fromAddress;

        EmailSender(String smtpServer, String fromAddress) {
            this.smtpServer  = smtpServer;
            this.fromAddress = fromAddress;
        }

        // [Factory Method — overriding] — only line that knows about EmailNotification
        @Override
        Notification createNotification() {
            return new EmailNotification(smtpServer, fromAddress);
        }
    }

    // [ConcreteCreator B]
    static class SmsSender extends NotificationSender {
        private final String apiKey;
        private final String shortCode;

        SmsSender(String apiKey, String shortCode) {
            this.apiKey    = apiKey;
            this.shortCode = shortCode;
        }

        @Override
        Notification createNotification() {
            return new SmsNotification(apiKey, shortCode);
        }
    }

    // [ConcreteCreator C]
    static class SlackSender extends NotificationSender {
        private final String webhookUrl;
        private final String channel;

        SlackSender(String webhookUrl, String channel) {
            this.webhookUrl = webhookUrl;
            this.channel    = channel;
        }

        @Override
        Notification createNotification() {
            return new SlackNotification(webhookUrl, channel);
        }
    }

    // =========================================================================
    // DEMO
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("═══ Factory Method — Notification Service Demo ══════════════");
        System.out.println();

        // [Polymorphism] — all three are referenced as NotificationSender
        // No if/switch anywhere — each object knows what to create
        NotificationSender[] senders = {
            new EmailSender("smtp.company.com", "alerts@company.com"),
            new SmsSender("sk-abc123def456", "40404"),
            new SlackSender("https://hooks.slack.com/T01/B01/xyz", "#incidents")
        };

        System.out.println("── Channel descriptions (factory method called) ─────────────");
        for (NotificationSender sender : senders) {
            System.out.println("  " + sender.describeChannel());
        }
        System.out.println();

        System.out.println("── Sending alerts (business method calls factory internally) ─");
        for (NotificationSender sender : senders) {
            sender.sendAlert("oncall-engineer", "Database CPU at 95%");
        }

        System.out.println("── OCP in action: add SlackSender without changing Creator ───");
        System.out.println("  → sendAlert() in NotificationSender is UNCHANGED");
        System.out.println("  → Only SlackSender + SlackNotification were added");
        System.out.println();

        System.out.println("── Type check: factory method returns correct concrete type ──");
        NotificationSender emailSender = new EmailSender("smtp.test.com", "test@test.com");
        Notification n = emailSender.createNotification();
        System.out.println("  createNotification() instanceof EmailNotification: "
                + (n instanceof EmailNotification));
    }
}

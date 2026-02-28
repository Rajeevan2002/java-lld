package com.ramkumar.lld.solid.dip.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Scenario A: E-commerce Order Processing System
 *
 * Demonstrates the Dependency Inversion Principle (DIP).
 *
 * STEP 1 — VIOLATION: ViolatingOrderProcessor
 *   The high-level class creates its own low-level dependencies with `new`.
 *   It is permanently hardwired to MySQL, Gmail, and Stripe.
 *   You cannot test it without a real DB, SMTP server, and payment API.
 *
 * STEP 2 — FIX: OrderProcessor with constructor injection
 *   Three abstractions: OrderRepository, MessageSender, PaymentGateway.
 *   OrderProcessor depends on these interfaces — never on concrete classes.
 *   Low-level implementations (InMemoryOrderRepo, ConsoleMessageSender,
 *   MockPaymentGateway) are created in main() and injected.
 *
 * Key DIP concepts illustrated:
 *   - `new` inside a constructor = DIP violation (tight coupling)
 *   - Constructor injection = DIP compliant (dependencies from outside)
 *   - Swapping implementations without touching the high-level class
 *   - Testability: use InMemory + Mock implementations in tests
 */
public class OrderProcessorDemo {

    // =========================================================================
    // ❌  STEP 1 — VIOLATION: High-level depends on low-level
    // =========================================================================

    /**
     * ❌ Fake "low-level" concrete classes — in real life these would connect
     * to actual MySQL, Gmail SMTP, and Stripe APIs.
     * Shown here as stubs so the violation is clear without external infra.
     */
    static class MySQLOrderRepository {
        public void save(String orderId, double amount) {
            System.out.println("[MySQL] Saving order: " + orderId + " amount: ₹" + amount);
        }
    }

    static class GmailEmailSender {
        public void sendEmail(String to, String subject, String body) {
            System.out.println("[Gmail SMTP] To: " + to + " | Subject: " + subject);
        }
    }

    static class StripePaymentGateway {
        public boolean charge(String customerId, double amount) {
            System.out.println("[Stripe API] Charging ₹" + amount + " to customer: " + customerId);
            return true;   // simulated success
        }
    }

    /**
     * ❌ DIP VIOLATED: OrderProcessor creates its own dependencies.
     *
     * Problems:
     *   1. You cannot swap MySQL for an in-memory DB without editing this class.
     *   2. You cannot swap Stripe for PayPal without editing this class.
     *   3. You cannot test this class without a real DB, SMTP server, and Stripe key.
     *   4. The `new` calls are hidden — dependency is invisible to the caller.
     */
    static class ViolatingOrderProcessor {

        // ← DIP VIOLATION: concrete classes as fields (not interfaces)
        private final MySQLOrderRepository   orderRepo;
        private final GmailEmailSender       emailSender;
        private final StripePaymentGateway   paymentGateway;

        public ViolatingOrderProcessor() {
            // ← DIP VIOLATION: high-level creates its own low-level dependencies
            this.orderRepo      = new MySQLOrderRepository();
            this.emailSender    = new GmailEmailSender();
            this.paymentGateway = new StripePaymentGateway();
        }

        public boolean processOrder(String orderId, String customerEmail,
                                    String customerId, double amount) {
            // ← payment call — forever Stripe
            boolean paid = paymentGateway.charge(customerId, amount);
            if (!paid) return false;
            // ← save call — forever MySQL
            orderRepo.save(orderId, amount);
            // ← email call — forever Gmail
            emailSender.sendEmail(customerEmail, "Order confirmed", "Your order " + orderId + " is confirmed.");
            return true;
        }
    }

    // =========================================================================
    // ✅  STEP 2 — FIX: Abstractions + constructor injection
    // =========================================================================

    // ── Abstractions (interfaces — defined by the high-level layer) ──────────

    /**
     * ✅ HIGH-LEVEL MODULE defines this interface.
     * Low-level modules (MySQL, Postgres, InMemory) implement it.
     * The dependency arrow is INVERTED: MySQLOrderRepo depends on THIS interface,
     * not the other way around.
     */
    interface OrderRepository {
        String          save(Order order);
        Optional<Order> findById(String orderId);
        List<Order>     findByCustomer(String customerId);
    }

    /** ✅ Abstraction for any message-sending mechanism: email, SMS, push, etc. */
    interface MessageSender {
        void send(String recipient, String subject, String body);
    }

    /** ✅ Abstraction for any payment provider: Stripe, PayPal, Razorpay, etc. */
    interface PaymentGateway {
        PaymentResult charge(String customerId, double amount);
    }

    // ── Value objects ────────────────────────────────────────────────────────

    enum OrderStatus { PENDING, PAID, FAILED }

    static class Order {
        private final String      orderId;
        private final String      customerId;
        private final double      amount;
        private       OrderStatus status;

        public Order(String orderId, String customerId, double amount) {
            if (orderId    == null || orderId.isBlank())    throw new IllegalArgumentException("orderId cannot be blank");
            if (customerId == null || customerId.isBlank()) throw new IllegalArgumentException("customerId cannot be blank");
            if (amount     <= 0)                           throw new IllegalArgumentException("amount must be > 0");
            this.orderId    = orderId;
            this.customerId = customerId;
            this.amount     = amount;
            this.status     = OrderStatus.PENDING;
        }

        public String      getOrderId()    { return orderId; }
        public String      getCustomerId() { return customerId; }
        public double      getAmount()     { return amount; }
        public OrderStatus getStatus()     { return status; }
        public void        setStatus(OrderStatus s) { this.status = s; }

        @Override
        public String toString() {
            return String.format("Order[%s, customer=%s, amount=₹%.2f, status=%s]",
                                 orderId, customerId, amount, status);
        }
    }

    static class PaymentResult {
        private final boolean success;
        private final String  transactionId;

        public PaymentResult(boolean success, String transactionId) {
            this.success       = success;
            this.transactionId = transactionId;
        }
        public boolean isSuccess()       { return success; }
        public String  getTransactionId(){ return transactionId; }
    }

    // ── High-level module — depends ONLY on abstractions ────────────────────

    /**
     * ✅ DIP COMPLIANT: OrderProcessor never creates its own dependencies.
     * All three fields are interfaces — the caller decides which implementation.
     *
     * DIP benefits shown here:
     *   (a) Swap MySQL → InMemory without changing this class
     *   (b) Swap Stripe → PayPal without changing this class
     *   (c) Unit-test without any real infrastructure
     */
    static class OrderProcessor {

        // ← DIP: abstractions as fields — not concrete classes
        private final OrderRepository orderRepository;   // interface
        private final MessageSender   messageSender;     // interface
        private final PaymentGateway  paymentGateway;    // interface

        // CONSTRUCTOR INJECTION — the central DIP mechanism
        // Fields are final: once set in constructor, never changed
        public OrderProcessor(OrderRepository orderRepository,
                              MessageSender   messageSender,
                              PaymentGateway  paymentGateway) {
            // null-checks at the boundary — if any dependency is null, fail fast
            this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository cannot be null");
            this.messageSender   = Objects.requireNonNull(messageSender,   "messageSender cannot be null");
            this.paymentGateway  = Objects.requireNonNull(paymentGateway,  "paymentGateway cannot be null");
        }

        /**
         * Place and pay for an order.
         * High-level logic lives here — the concrete implementation details
         * (MySQL, Stripe, Gmail) are completely invisible to this method.
         */
        public Order processOrder(Order order, String recipientEmail) {
            if (order == null || recipientEmail == null || recipientEmail.isBlank())
                throw new IllegalArgumentException("Order and email cannot be null/blank");

            // DIP in action: calls an interface method — doesn't care if it's Stripe or PayPal
            PaymentResult result = paymentGateway.charge(order.getCustomerId(), order.getAmount());

            if (result.isSuccess()) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);   // DIP: interface call — doesn't care if it's MySQL or Mongo
                // DIP: interface call — doesn't care if it's Gmail or SNS
                messageSender.send(recipientEmail,
                    "Order confirmed: " + order.getOrderId(),
                    "Your order is confirmed. Txn: " + result.getTransactionId());
            } else {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                messageSender.send(recipientEmail,
                    "Order failed: " + order.getOrderId(),
                    "Payment could not be processed. Please retry.");
            }
            return order;
        }

        /** Retrieve all orders for a customer — fully interface-driven */
        public List<Order> getCustomerOrders(String customerId) {
            if (customerId == null || customerId.isBlank())
                throw new IllegalArgumentException("customerId cannot be blank");
            return orderRepository.findByCustomer(customerId);
        }
    }

    // ── Low-level implementations — depend on abstractions (DIP satisfied) ──

    /**
     * ✅ In-memory implementation of OrderRepository.
     * Used in tests and demos — swappable with MySQLOrderRepository at runtime.
     * MySQLOrderRepository would also implement OrderRepository — zero changes to OrderProcessor.
     */
    static class InMemoryOrderRepository implements OrderRepository {

        private final Map<String, Order>       store    = new HashMap<>();
        private final Map<String, List<Order>> byCustomer = new HashMap<>();

        @Override
        public String save(Order order) {
            store.put(order.getOrderId(), order);
            byCustomer.computeIfAbsent(order.getCustomerId(), k -> new ArrayList<>());
            // Only add once (avoid duplicates on status update)
            List<Order> customerOrders = byCustomer.get(order.getCustomerId());
            if (!customerOrders.contains(order)) customerOrders.add(order);
            System.out.println("[InMemoryRepo] Saved: " + order);
            return order.getOrderId();
        }

        @Override
        public Optional<Order> findById(String orderId) {
            return Optional.ofNullable(store.get(orderId));
        }

        @Override
        public List<Order> findByCustomer(String customerId) {
            return Collections.unmodifiableList(
                byCustomer.getOrDefault(customerId, Collections.emptyList()));
        }
    }

    /**
     * ✅ Console-based message sender — simulates email/SMS output.
     * Swappable with a real SMTP sender without touching OrderProcessor.
     */
    static class ConsoleMessageSender implements MessageSender {

        @Override
        public void send(String recipient, String subject, String body) {
            System.out.printf("[ConsoleEmail] To: %s | Subject: %s | Body: %s%n",
                              recipient, subject, body);
        }
    }

    /**
     * ✅ Mock payment gateway — always succeeds with a fake transaction ID.
     * Used for testing and demo. Swappable with StripePaymentGateway
     * without touching OrderProcessor.
     */
    static class MockPaymentGateway implements PaymentGateway {

        private int txnCounter = 0;

        @Override
        public PaymentResult charge(String customerId, double amount) {
            String txnId = String.format("MOCK-TXN-%03d", ++txnCounter);
            System.out.printf("[MockGateway] Charged ₹%.2f to customer %s → %s%n",
                              amount, customerId, txnId);
            return new PaymentResult(true, txnId);
        }
    }

    /**
     * ✅ Failing payment gateway — always fails.
     * Demonstrates that you can inject different implementations
     * to test different scenarios — DIP testability in action.
     */
    static class AlwaysFailPaymentGateway implements PaymentGateway {

        @Override
        public PaymentResult charge(String customerId, double amount) {
            System.out.println("[FailGateway] Payment declined for customer: " + customerId);
            return new PaymentResult(false, null);
        }
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" DIP Demo: Order Processing System");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ── STEP 1: Show the violation ────────────────────────────────────────
        System.out.println("── VIOLATION: ViolatingOrderProcessor ──────────────────");
        System.out.println("ViolatingOrderProcessor creates its own MySQL + Gmail + Stripe");
        System.out.println("You cannot change or test them without modifying the class.\n");

        ViolatingOrderProcessor bad = new ViolatingOrderProcessor();
        bad.processOrder("ORD-000", "alice@example.com", "CUST-001", 1500.0);

        System.out.println("\n── FIX: DIP-compliant OrderProcessor ──────────────────");

        // ── STEP 2: Wire dependencies manually (manual DI, no framework) ─────
        // CALLER creates and injects concrete implementations — high-level class never `new`s them
        OrderRepository orderRepo      = new InMemoryOrderRepository();
        MessageSender   consoleSender  = new ConsoleMessageSender();
        PaymentGateway  mockGateway    = new MockPaymentGateway();

        // Constructor injection: OrderProcessor gets interfaces, not concrete classes
        OrderProcessor processor = new OrderProcessor(orderRepo, consoleSender, mockGateway);

        System.out.println("\n[Order 1 — Successful payment]");
        Order o1 = new Order("ORD-001", "CUST-001", 1200.0);
        processor.processOrder(o1, "alice@example.com");
        System.out.println("Status: " + o1.getStatus());   // PAID

        System.out.println("\n[Order 2 — Another order, same customer]");
        Order o2 = new Order("ORD-002", "CUST-001", 850.0);
        processor.processOrder(o2, "alice@example.com");
        System.out.println("Status: " + o2.getStatus());   // PAID

        System.out.println("\n[Customer order history — DIP: OrderProcessor calls repository interface]");
        List<Order> history = processor.getCustomerOrders("CUST-001");
        System.out.println("Orders for CUST-001: " + history.size());   // 2

        // ── STEP 3: Swap payment gateway — ZERO changes to OrderProcessor ────
        System.out.println("\n── DIP Proof: swap PaymentGateway without touching OrderProcessor ─");

        // New OrderProcessor instance with FAILING gateway — same repo, same sender
        OrderProcessor failProcessor = new OrderProcessor(orderRepo, consoleSender,
                                                          new AlwaysFailPaymentGateway());
        Order o3 = new Order("ORD-003", "CUST-002", 500.0);
        failProcessor.processOrder(o3, "bob@example.com");
        System.out.println("Status: " + o3.getStatus());   // FAILED

        // ── STEP 4: Verify DIP via findById ──────────────────────────────────
        System.out.println("\n── Verify saved orders via repository ──────────────────");
        orderRepo.findById("ORD-001").ifPresent(o -> System.out.println("Found: " + o));
        orderRepo.findById("ORD-003").ifPresent(o -> System.out.println("Found: " + o));

        System.out.println("\n── Key DIP takeaways ────────────────────────────────────");
        System.out.println("✅ OrderProcessor never called `new` for its dependencies");
        System.out.println("✅ Swapped AlwaysFailGateway without touching OrderProcessor");
        System.out.println("✅ InMemoryRepo replaces MySQL — zero OrderProcessor changes");
        System.out.println("✅ All fields are final interfaces — structurally enforced DIP");
    }
}

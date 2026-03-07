package com.ramkumar.lld.designpatterns.creational.factorymethod.results;

import java.util.List;
import java.util.Set;

/**
 * Reference Solution — Factory Method Pattern (Creational)
 * Phase 3, Topic 3.2 | Scenario B: Payment Gateway
 *
 * Key fixes over the practice submission:
 *   1. processPayment() prints the charging message before returning
 *   2. BankTransferProcessor uses amount < 10.0 (not <=) — $10 is valid
 *   3. BankTransferProcessor validates amount > 0 FIRST, then < 10
 *   4. calculateFee() stores getTransactionFee() result before printing and returning
 *   5. Consistent print tags matching getGatewayName() in refund()
 */
public class FactoryMethodReference {

    // =========================================================================
    // PRODUCT — interface all concrete processors must satisfy
    // =========================================================================

    // [Factory Method — Product]
    interface PaymentProcessor {
        String processPayment(double amount, String currency);
        void refund(String transactionId, double amount);
        String getGatewayName();
        double getTransactionFee(double amount);
    }

    // =========================================================================
    // CONCRETE PRODUCTS
    // =========================================================================

    // [ConcreteProduct A] — Stripe
    static class StripeProcessor implements PaymentProcessor {
        // Instance field (NOT static) — each processor has its own counter
        private int transactionCounter = 0;
        private static final Set<String> SUPPORTED = Set.of("USD", "EUR", "GBP");

        @Override
        public String processPayment(double amount, String currency) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            if (currency == null || currency.isBlank())
                throw new IllegalArgumentException("currency must not be blank");
            if (!SUPPORTED.contains(currency))
                throw new IllegalArgumentException("Stripe does not support: " + currency);

            String txId = String.format("STRIPE-%05d", ++transactionCounter);
            // [Fix 1] — print BEFORE returning; creates the audit trail
            System.out.printf("[Stripe] Charging $%.2f %s → tx: %s%n", amount, currency, txId);
            return txId;
        }

        @Override
        public void refund(String transactionId, double amount) {
            if (transactionId == null || transactionId.isBlank())
                throw new IllegalArgumentException("transactionId must not be blank");
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            // [Fix 5] — tag matches getGatewayName()
            System.out.printf("[Stripe] Refunding $%.2f for tx: %s%n", amount, transactionId);
        }

        @Override public String getGatewayName() { return "Stripe"; }

        @Override
        public double getTransactionFee(double amount) {
            // No validation needed here — PaymentService.calculateFee() already validated
            return (amount * 0.029) + 0.30;
        }
    }

    // [ConcreteProduct B] — PayPal
    static class PayPalProcessor implements PaymentProcessor {
        private int transactionCounter = 0;

        @Override
        public String processPayment(double amount, String currency) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            if (currency == null || currency.isBlank())
                throw new IllegalArgumentException("currency must not be blank");
            // PayPal accepts all currencies — no further check needed

            String txId = String.format("PAYPAL-%05d", ++transactionCounter);
            System.out.printf("[PayPal] Charging $%.2f %s → tx: %s%n", amount, currency, txId);
            return txId;
        }

        @Override
        public void refund(String transactionId, double amount) {
            if (transactionId == null || transactionId.isBlank())
                throw new IllegalArgumentException("transactionId must not be blank");
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            // [Fix 5] — "PayPal" matches getGatewayName() exactly (capital P)
            System.out.printf("[PayPal] Refunding $%.2f for tx: %s%n", amount, transactionId);
        }

        @Override public String getGatewayName() { return "PayPal"; }

        @Override
        public double getTransactionFee(double amount) {
            return (amount * 0.0349) + 0.49;
        }
    }

    // [ConcreteProduct C] — Bank Transfer
    static class BankTransferProcessor implements PaymentProcessor {
        private int transactionCounter = 0;

        @Override
        public String processPayment(double amount, String currency) {
            // [Fix 2/3] — generic check FIRST, then domain-specific minimum
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            // [Fix 2] — use < 10.0 not <= 10; exactly $10 is a valid transaction
            if (amount < 10.0)
                throw new IllegalArgumentException("Bank Transfer minimum is $10.00");
            if (currency == null || currency.isBlank())
                throw new IllegalArgumentException("currency must not be blank");
            if (!"USD".equals(currency))
                throw new IllegalArgumentException("Bank Transfer only supports USD, got: " + currency);

            String txId = String.format("BANK-%05d", ++transactionCounter);
            System.out.printf("[Bank Transfer] Charging $%.2f %s → tx: %s%n", amount, currency, txId);
            return txId;
        }

        @Override
        public void refund(String transactionId, double amount) {
            if (transactionId == null || transactionId.isBlank())
                throw new IllegalArgumentException("transactionId must not be blank");
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            // [Fix 5] — "Bank Transfer" matches getGatewayName()
            System.out.printf("[Bank Transfer] Refunding $%.2f for tx: %s%n", amount, transactionId);
        }

        @Override public String getGatewayName() { return "Bank Transfer"; }

        @Override
        public double getTransactionFee(double amount) {
            // [Fix 3] — same validation order as processPayment
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            if (amount < 10.0) throw new IllegalArgumentException("Bank Transfer minimum is $10.00");
            return 1.50;   // flat fee regardless of amount
        }
    }

    // =========================================================================
    // CREATOR — abstract class with the Factory Method + business logic
    // =========================================================================

    // [Factory Method — Creator]
    static abstract class PaymentService {

        // ── FACTORY METHOD (abstract) ────────────────────────────────────────
        // No logic here. Subclass decides which processor to instantiate.
        abstract PaymentProcessor createProcessor();

        // ── BUSINESS METHODS — call factory, work through interface ──────────

        String charge(double amount, String currency) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            // [Factory Method call] — no type check, no instanceof, no switch
            PaymentProcessor processor = createProcessor();
            String txId = processor.processPayment(amount, currency);
            System.out.println("[PaymentService] Transaction complete: " + txId);
            return txId;
        }

        void refundTransaction(String transactionId, double amount) {
            if (transactionId == null || transactionId.isBlank())
                throw new IllegalArgumentException("transactionId must not be blank");
            createProcessor().refund(transactionId, amount);
        }

        double calculateFee(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
            // [Fix 4] — store result once; never call the same method twice
            double fee = createProcessor().getTransactionFee(amount);
            System.out.printf("[PaymentService] Fee for $%.2f: $%.2f%n", amount, fee);
            return fee;
        }
    }

    // =========================================================================
    // CONCRETE CREATORS — one line each, which is all they should need
    // =========================================================================

    // [ConcreteCreator A]
    static class StripePaymentService extends PaymentService {
        @Override PaymentProcessor createProcessor() { return new StripeProcessor(); }
    }

    // [ConcreteCreator B]
    static class PayPalPaymentService extends PaymentService {
        @Override PaymentProcessor createProcessor() { return new PayPalProcessor(); }
    }

    // [ConcreteCreator C]
    static class BankTransferPaymentService extends PaymentService {
        @Override PaymentProcessor createProcessor() { return new BankTransferProcessor(); }
    }

    // =========================================================================
    // Tests — same 12 as practice + Test 13 (catches the <= vs < boundary bug)
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: Stripe charge returns STRIPE- tx ID ════════════");
        PaymentService stripe = new StripePaymentService();
        String stripeTx = stripe.charge(100.0, "USD");
        System.out.println("TX ID: " + stripeTx);
        System.out.println("Test 1 " + (stripeTx.startsWith("STRIPE-") ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 2: PayPal charge returns PAYPAL- tx ID ════════════");
        PaymentService paypal = new PayPalPaymentService();
        String paypalTx = paypal.charge(50.0, "USD");
        System.out.println("TX ID: " + paypalTx);
        System.out.println("Test 2 " + (paypalTx.startsWith("PAYPAL-") ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 3: Bank Transfer charge returns BANK- tx ID ═══════");
        PaymentService bank = new BankTransferPaymentService();
        String bankTx = bank.charge(250.0, "USD");
        System.out.println("TX ID: " + bankTx);
        System.out.println("Test 3 " + (bankTx.startsWith("BANK-") ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 4: Stripe fee = 2.9% + $0.30 ═════════════════════");
        double stripeFee = stripe.calculateFee(100.0);
        double expectedStripeFee = (100.0 * 0.029) + 0.30;
        System.out.printf("Stripe fee on $100.00: $%.4f (expected: $%.4f)%n", stripeFee, expectedStripeFee);
        System.out.println("Test 4 " + (Math.abs(stripeFee - expectedStripeFee) < 0.001 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: PayPal fee = 3.49% + $0.49 ════════════════════");
        double paypalFee = paypal.calculateFee(100.0);
        double expectedPaypalFee = (100.0 * 0.0349) + 0.49;
        System.out.printf("PayPal fee on $100.00: $%.4f (expected: $%.4f)%n", paypalFee, expectedPaypalFee);
        System.out.println("Test 5 " + (Math.abs(paypalFee - expectedPaypalFee) < 0.001 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 6: Bank Transfer fee = flat $1.50 ════════════════");
        double bankFee500 = bank.calculateFee(500.0);
        double bankFee10  = bank.calculateFee(10.0);
        System.out.println("Bank fee on $500.00: $" + bankFee500 + " (expected: $1.5)");
        System.out.println("Bank fee on $10.00:  $" + bankFee10  + " (expected: $1.5)");
        System.out.println("Test 6 " + (bankFee500 == 1.50 && bankFee10 == 1.50 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 7: refundTransaction does not throw ════════════════");
        try {
            stripe.refundTransaction(stripeTx, 100.0);
            System.out.println("Test 7 PASSED — refund completed without exception");
        } catch (Exception e) {
            System.out.println("Test 7 FAILED — unexpected: " + e.getMessage());
        }

        System.out.println("\n═══ Test 8: Factory method returns correct processor type ═══");
        PaymentProcessor stripeProc = new StripePaymentService().createProcessor();
        PaymentProcessor paypalProc = new PayPalPaymentService().createProcessor();
        PaymentProcessor bankProc   = new BankTransferPaymentService().createProcessor();
        boolean t8 = stripeProc instanceof StripeProcessor
                  && paypalProc instanceof PayPalProcessor
                  && bankProc   instanceof BankTransferProcessor;
        System.out.println("StripePaymentService  → StripeProcessor:       " + (stripeProc instanceof StripeProcessor));
        System.out.println("PayPalPaymentService  → PayPalProcessor:       " + (paypalProc instanceof PayPalProcessor));
        System.out.println("BankTransferService   → BankTransferProcessor: " + (bankProc   instanceof BankTransferProcessor));
        System.out.println("Test 8 " + (t8 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 9: Polymorphic usage — gateway names ═══════════════");
        List<PaymentService> services = List.of(
                new StripePaymentService(),
                new PayPalPaymentService(),
                new BankTransferPaymentService()
        );
        List<String> expectedNames = List.of("Stripe", "PayPal", "Bank Transfer");
        boolean t9 = true;
        for (int i = 0; i < services.size(); i++) {
            String name = services.get(i).createProcessor().getGatewayName();
            System.out.println("  Service[" + i + "].getGatewayName() = \"" + name + "\"");
            if (!expectedNames.get(i).equals(name)) t9 = false;
        }
        System.out.println("Test 9 " + (t9 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 10: Stripe rejects unsupported currency ═══════════");
        try {
            stripe.charge(100.0, "BTC");
            System.out.println("Test 10 FAILED — should have thrown for BTC");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 10 PASSED — Stripe rejects BTC");
        }

        System.out.println("\n═══ Test 11: Bank Transfer rejects amount below $10 ════════");
        try {
            bank.charge(5.0, "USD");
            System.out.println("Test 11 FAILED — should have thrown for amount < $10");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 11 PASSED — BankTransfer enforces $10 minimum");
        }

        System.out.println("\n═══ Test 12: PaymentService has no if/switch on gateway type ═");
        System.out.println("  Test 12 PASSED — charge/refundTransaction/calculateFee reference only PaymentProcessor");

        // ── Test 13: $10.00 exactly must PASS for Bank Transfer ───────────────
        // Most common mistake: using `amount <= 10` instead of `amount < 10`
        // With the wrong condition, exactly $10.00 is incorrectly rejected.
        System.out.println("\n═══ Test 13: Bank Transfer accepts exactly $10.00 ══════════");
        try {
            String tenDollarTx = bank.charge(10.0, "USD");
            System.out.println("TX ID: " + tenDollarTx);
            System.out.println("Test 13 PASSED — $10.00 is a valid Bank Transfer amount");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 13 FAILED — $10.00 rejected: " + e.getMessage());
            System.out.println("  Fix: use amount < 10.0 not amount <= 10");
        }
    }
}

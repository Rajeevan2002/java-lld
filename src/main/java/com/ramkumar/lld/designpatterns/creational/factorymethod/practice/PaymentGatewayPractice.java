package com.ramkumar.lld.designpatterns.creational.factorymethod.practice;

import java.util.List;
import java.util.Set;

/**
 * Practice Exercise — Factory Method Pattern (Creational)
 * Phase 3, Topic 3.2 | Scenario B: Payment Gateway
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building a checkout system for an e-commerce platform. The
 * platform integrates with three payment gateways: Stripe, PayPal, and
 * Bank Transfer. Each gateway has its own fee structure, supported
 * currencies, and transaction ID format.
 *
 * The business logic for charging, refunding, and fee calculation is
 * IDENTICAL regardless of gateway. Only the concrete gateway behaviour
 * (fees, currency support, tx ID format) differs per integration.
 *
 * Implement using the Factory Method pattern so that adding a new gateway
 * (e.g., Razorpay) requires ONLY a new ConcreteProduct + ConcreteCreator
 * pair — no changes to the existing PaymentService business methods.
 *
 * ── PRODUCT: PaymentProcessor (interface) ──────────────────────────────
 *
 *   String processPayment(double amount, String currency)
 *     - Validates amount > 0 → IllegalArgumentException("amount must be > 0")
 *     - Validates currency not null/blank → IllegalArgumentException
 *     - Validates the gateway accepts this currency → IllegalArgumentException
 *     - Increments an internal per-instance transactionCounter (starts at 0)
 *     - Returns a transaction ID in gateway-specific format
 *     - Prints: "[<GatewayName>] Charging $<amount> <currency> → tx: <txId>"
 *
 *   void refund(String transactionId, double amount)
 *     - Validates transactionId not null/blank → IllegalArgumentException
 *     - Validates amount > 0 → IllegalArgumentException("amount must be > 0")
 *     - Prints: "[<GatewayName>] Refunding $<amount> for tx: <transactionId>"
 *
 *   String getGatewayName()
 *     - Returns the gateway display name: "Stripe", "PayPal", "Bank Transfer"
 *
 *   double getTransactionFee(double amount)
 *     - Returns the fee for a transaction of the given amount.
 *
 * ── CONCRETE PRODUCTS ───────────────────────────────────────────────────
 *
 *   StripeProcessor
 *     - Fee: 2.9% of amount + $0.30 flat  →  (amount * 0.029) + 0.30
 *     - Supported currencies: "USD", "EUR", "GBP" (case-sensitive)
 *     - TX ID format: String.format("STRIPE-%05d", ++transactionCounter)
 *     - Gateway name: "Stripe"
 *
 *   PayPalProcessor
 *     - Fee: 3.49% of amount + $0.49 flat  →  (amount * 0.0349) + 0.49
 *     - Supported currencies: any non-null, non-blank string (accepts all)
 *     - TX ID format: String.format("PAYPAL-%05d", ++transactionCounter)
 *     - Gateway name: "PayPal"
 *
 *   BankTransferProcessor
 *     - Fee: flat $1.50 regardless of amount
 *     - Supported currencies: "USD" only
 *     - Minimum transaction amount: $10.00
 *       → throw IllegalArgumentException("Bank Transfer minimum is $10.00")
 *         if amount < 10.0 (check AFTER the generic amount > 0 check)
 *     - TX ID format: String.format("BANK-%05d", ++transactionCounter)
 *     - Gateway name: "Bank Transfer"
 *
 * ── CREATOR: PaymentService (abstract class) ─────────────────────────────
 *
 *   abstract PaymentProcessor createProcessor()
 *     - The Factory Method. No logic — just return a new PaymentProcessor.
 *
 *   String charge(double amount, String currency)
 *     - Validates amount > 0 → IllegalArgumentException("amount must be > 0")
 *     - Calls createProcessor() to get the gateway
 *     - Calls processor.processPayment(amount, currency)
 *     - Prints: "[PaymentService] Transaction complete: <txId>"
 *     - Returns the txId
 *
 *   void refundTransaction(String transactionId, double amount)
 *     - Validates transactionId not null/blank → IllegalArgumentException
 *     - Calls createProcessor() to get the gateway
 *     - Calls processor.refund(transactionId, amount)
 *
 *   double calculateFee(double amount)
 *     - Validates amount > 0 → IllegalArgumentException("amount must be > 0")
 *     - Calls createProcessor() to get the gateway
 *     - Returns processor.getTransactionFee(amount)
 *     - Prints: "[PaymentService] Fee for $<amount>: $<fee>"
 *
 * ── CONCRETE CREATORS ───────────────────────────────────────────────────
 *
 *   StripePaymentService extends PaymentService
 *     - createProcessor() → return new StripeProcessor()
 *
 *   PayPalPaymentService extends PaymentService
 *     - createProcessor() → return new PayPalProcessor()
 *
 *   BankTransferPaymentService extends PaymentService
 *     - createProcessor() → return new BankTransferProcessor()
 *
 * ── DESIGN CONSTRAINTS ──────────────────────────────────────────────────
 *   1. No if/switch on payment type anywhere in PaymentService.
 *   2. PaymentService.charge(), refundTransaction(), and calculateFee()
 *      must NOT reference StripeProcessor, PayPalProcessor, or
 *      BankTransferProcessor by name — they work only through the
 *      PaymentProcessor interface.
 *   3. createProcessor() must NOT be static.
 *   4. Each PaymentProcessor instance has its own transactionCounter
 *      (instance field, NOT static).
 *
 * ═══════════════════════════════════════════════════════════════════════
 * DO NOT MODIFY the main() method — fill in the TODOs to make tests pass
 * ═══════════════════════════════════════════════════════════════════════
 */
public class PaymentGatewayPractice {

    // =========================================================================
    // ── TODO 1: Declare the PaymentProcessor interface
    //            - String processPayment(double amount, String currency)
    //            - void refund(String transactionId, double amount)
    //            - String getGatewayName()
    //            - double getTransactionFee(double amount)
    // =========================================================================
    interface  PaymentProcessor {
        String processPayment(double amount, String currency);
        void refund(String transactionId, double amount);
        String getGatewayName();
        double getTransactionFee(double amount);
    }


    // =========================================================================
    // ── TODO 2: Implement StripeProcessor
    //            Fields:
    //              private int transactionCounter   (starts at 0, NOT static)
    //            Methods: processPayment, refund, getGatewayName, getTransactionFee
    //            Fee:       (amount * 0.029) + 0.30
    //            Currencies: "USD", "EUR", "GBP" only
    //            TX ID:      String.format("STRIPE-%05d", ++transactionCounter)
    // =========================================================================
    static class StripeProcessor implements PaymentProcessor {
        private int transactionCounter;
        private Set<String> supportedCurrencies;
        public StripeProcessor(){
            this.transactionCounter = 0;
            this.supportedCurrencies = Set.of("USD", "EUR", "GBP");
        }

        @Override
        public void refund(String transactionId, double amount){
            if(transactionId == null || transactionId.isBlank()){
                throw new IllegalArgumentException("TransactionId should not be null or blank!!");
            }
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to > 0");
            }
            System.out.printf("[Stripe] Refunding $%.2f for tx: %s%n", amount, transactionId);
        }

        @Override
        public double getTransactionFee(double amount){
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to be > 0");
            }
            return (amount * 0.029)  + 0.30;
        }

        @Override
        public String getGatewayName() {
            return "Stripe";
        }

        @Override
        public String processPayment(double amount, String currency) {
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to > 0");
            }
            if(currency == null || currency.isBlank()){
                throw new IllegalArgumentException("Currency cannot be null or blank!!");
            }
            if(!supportedCurrencies.contains(currency)){
                throw new IllegalArgumentException("One of USD, EUR, GBP is supported!!");
            }
            return String.format("STRIPE-%05d", ++transactionCounter);
        }
    }

    // =========================================================================
    // ── TODO 3: Implement PayPalProcessor
    //            Fields:
    //              private int transactionCounter   (starts at 0, NOT static)
    //            Methods: processPayment, refund, getGatewayName, getTransactionFee
    //            Fee:       (amount * 0.0349) + 0.49
    //            Currencies: accepts any non-null/non-blank
    //            TX ID:      String.format("PAYPAL-%05d", ++transactionCounter)
    // =========================================================================
    static class PayPalProcessor implements PaymentProcessor {
        private int transactionCounter;

        public PayPalProcessor(){
            this.transactionCounter = 0;
        }

        @Override
        public void refund(String transactionId, double amount){
            if(transactionId == null || transactionId.isBlank()){
                throw new IllegalArgumentException("TransactionId should not be null or blank!!");
            }
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to > 0");
            }
            System.out.printf("[Paypal] Refunding $%.2f for tx: %s%n", amount, transactionId);
        }

        @Override
        public String getGatewayName() {
            return "PayPal";
        }

        @Override
        public double getTransactionFee(double amount){
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to be > 0");
            }
            return (amount * 0.0349)  + 0.49;
        }

        @Override
        public String processPayment(double amount, String currency) {
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to > 0");
            }
            if(currency == null || currency.isBlank()){
                throw new IllegalArgumentException("Currency cannot be null or blank!!");
            }
            return String.format("PAYPAL-%05d", ++transactionCounter);
        }
    }

    // =========================================================================
    // ── TODO 4: Implement BankTransferProcessor
    //            Fields:
    //              private int transactionCounter   (starts at 0, NOT static)
    //            Methods: processPayment, refund, getGatewayName, getTransactionFee
    //            Fee:       flat 1.50
    //            Currencies: "USD" only
    //            Minimum:   $10.00 (throw IAE AFTER generic amount > 0 check)
    //            TX ID:      String.format("BANK-%05d", ++transactionCounter)
    // =========================================================================
    static class BankTransferProcessor implements PaymentProcessor {
        private int transactionCounter;
        private Set<String> supportedCurrencies;
        public BankTransferProcessor(){
            this.transactionCounter = 0;
            this.supportedCurrencies = Set.of("USD");
        }

        @Override
        public void refund(String transactionId, double amount){
            if(transactionId == null || transactionId.isBlank()){
                throw new IllegalArgumentException("TransactionId should not be null or blank!!");
            }
            if(amount <= 0){
                throw new IllegalArgumentException("Amount has to > 0");
            }
            System.out.printf("[Bank] Refunding $%.2f for tx: %s%n", amount, transactionId);
        }

        @Override
        public double getTransactionFee(double amount){
            if(amount < 10){
                throw new IllegalArgumentException("Amount has to be > 0");
            }
            return 1.50;
        }

        @Override
        public String getGatewayName() {
            return "Bank Transfer";
        }

        @Override
        public String processPayment(double amount, String currency) {
            if(amount <= 10){
                throw new IllegalArgumentException("Amount has to > 10");
            }
            if(currency == null || currency.isBlank()){
                throw new IllegalArgumentException("Currency cannot be null or blank!!");
            }
            if(!supportedCurrencies.contains(currency)){
                throw new IllegalArgumentException("One of USD, EUR, GBP is supported!!");
            }
            return String.format("BANK-%05d", ++transactionCounter);
        }

    }


    // =========================================================================
    // ── TODO 5: Declare abstract class PaymentService
    //            - abstract PaymentProcessor createProcessor()   ← factory method
    //   (Do not add any other abstract methods — only this one)
    // =========================================================================
    static abstract class PaymentService {
        abstract PaymentProcessor createProcessor();


        // =========================================================================
        // ── TODO 6: In PaymentService — implement String charge(double amount, String currency)
        //            - Validate amount > 0
        //            - Call createProcessor() to get the gateway
        //            - Call processor.processPayment(amount, currency)
        //            - Print: "[PaymentService] Transaction complete: <txId>"
        //            - Return txId
        // =========================================================================
        public String charge(double amount, String currency) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount has to be > 0");
            }
            PaymentProcessor paymentProcessor = createProcessor();
            String transactionId = paymentProcessor.processPayment(amount, currency);
            System.out.println("[PaymentService] Transaction complete: " + transactionId);
            return transactionId;
        }

        // =========================================================================
        // ── TODO 7: In PaymentService — implement void refundTransaction(String txId, double amount)
        //            - Validate txId not null/blank
        //            - Call createProcessor() and then processor.refund(txId, amount)
        // =========================================================================
        public void refundTransaction(String txId, double amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount has to be > 0");
            }
            if (txId == null || txId.isBlank()) {
                throw new IllegalArgumentException("TxId cannot be null or blank");
            }

            PaymentProcessor paymentProcessor = createProcessor();
            paymentProcessor.refund(txId, amount);
        }

        // =========================================================================
        // ── TODO 8: In PaymentService — implement double calculateFee(double amount)
        //            - Validate amount > 0
        //            - Call createProcessor() and then processor.getTransactionFee(amount)
        //            - Print: "[PaymentService] Fee for $<amount>: $<fee>"
        //            - Return the fee
        // =========================================================================
        public double calculateFee(double amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount has to be > 0");
            }
            PaymentProcessor paymentProcessor = createProcessor();
            System.out.printf("[PaymentService]  Fee for $%.2f: $%.2f\n", amount,
                    paymentProcessor.getTransactionFee(amount));
            return paymentProcessor.getTransactionFee(amount);
        }
    }

        // =========================================================================
        // ── TODO 9: Implement StripePaymentService extends PaymentService
        //            - createProcessor() → return new StripeProcessor()
        // =========================================================================
    static class StripePaymentService extends PaymentService {
        @Override
        public PaymentProcessor createProcessor() {
            return new StripeProcessor();
        }
    }

        // =========================================================================
        // ── TODO 10: Implement PayPalPaymentService extends PaymentService
        //             - createProcessor() → return new PayPalProcessor()
        // =========================================================================
    static class PayPalPaymentService extends PaymentService {
        @Override
        public PaymentProcessor createProcessor() {
            return new PayPalProcessor();
        }
    }
        // =========================================================================
        // ── TODO 11: Implement BankTransferPaymentService extends PaymentService
        //             - createProcessor() → return new BankTransferProcessor()
        // =========================================================================
    static class BankTransferPaymentService extends PaymentService {
        @Override
        public PaymentProcessor createProcessor() {
                return new BankTransferProcessor();
        }
    }

    // =========================================================================
    // DO NOT MODIFY — fill in TODOs above to make all tests pass
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
        System.out.printf("Stripe fee on $100.00: $%.4f (expected: $%.4f)\n", stripeFee, expectedStripeFee);
        System.out.println("Test 4 " + (Math.abs(stripeFee - expectedStripeFee) < 0.001 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: PayPal fee = 3.49% + $0.49 ════════════════════");
        double paypalFee = paypal.calculateFee(100.0);
        double expectedPaypalFee = (100.0 * 0.0349) + 0.49;
        System.out.printf("PayPal fee on $100.00: $%.4f (expected: $%.4f)\n", paypalFee, expectedPaypalFee);
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
        System.out.println("  (Verified by code review — charge(), refundTransaction(), calculateFee()");
        System.out.println("   must reference only PaymentProcessor, never a concrete class)");
        System.out.println("  Test 12 PASSED if all previous tests pass and no switch exists in PaymentService");
    }

    // =========================================================================
    // HINTS (read only if stuck)
    // =========================================================================

    /*
     * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
     * The key insight: PaymentService.charge(), refundTransaction(), and
     * calculateFee() should NOT know which gateway they are talking to.
     * They should call a method that "magically" gives them the right gateway,
     * and that method is different in each subclass.
     *
     * For the fee: each processor computes it differently. PaymentService
     * should not switch on the type — it should ask the processor itself.
     *
     * ── HINT 2 (Direct) ────────────────────────────────────────────────────
     * Structure:
     *
     *   interface PaymentProcessor {
     *       String processPayment(double amount, String currency);
     *       void refund(String transactionId, double amount);
     *       String getGatewayName();
     *       double getTransactionFee(double amount);
     *   }
     *
     *   abstract class PaymentService {
     *       abstract PaymentProcessor createProcessor();  // ← Factory Method
     *
     *       String charge(double amount, String currency) {
     *           // validate, then:
     *           PaymentProcessor p = createProcessor();   // no type check here!
     *           String txId = p.processPayment(amount, currency);
     *           System.out.println("[PaymentService] Transaction complete: " + txId);
     *           return txId;
     *       }
     *       // ... similarly for refundTransaction and calculateFee
     *   }
     *
     *   class StripePaymentService extends PaymentService {
     *       @Override
     *       PaymentProcessor createProcessor() { return new StripeProcessor(); }
     *   }
     *
     * ── HINT 3 (Near-Solution) ─────────────────────────────────────────────
     * StripeProcessor skeleton:
     *
     *   static class StripeProcessor implements PaymentProcessor {
     *       private int transactionCounter = 0;  // instance field — NOT static
     *
     *       @Override
     *       public String processPayment(double amount, String currency) {
     *           if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
     *           if (currency == null || currency.isBlank())
     *               throw new IllegalArgumentException("currency must not be blank");
     *           if (!List.of("USD", "EUR", "GBP").contains(currency))
     *               throw new IllegalArgumentException("Stripe does not support: " + currency);
     *           String txId = String.format("STRIPE-%05d", ++transactionCounter);
     *           System.out.printf("[Stripe] Charging $%.2f %s → tx: %s%n", amount, currency, txId);
     *           return txId;
     *       }
     *
     *       @Override
     *       public void refund(String transactionId, double amount) {
     *           if (transactionId == null || transactionId.isBlank())
     *               throw new IllegalArgumentException("transactionId must not be blank");
     *           if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
     *           System.out.printf("[Stripe] Refunding $%.2f for tx: %s%n", amount, transactionId);
     *       }
     *
     *       @Override public String getGatewayName() { return "Stripe"; }
     *
     *       @Override
     *       public double getTransactionFee(double amount) { return (amount * 0.029) + 0.30; }
     *   }
     */
}

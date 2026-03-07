package com.ramkumar.lld.designpatterns.structural.adapter.practice;

// ─────────────────────────────────────────────────────────────────────────────
// Adapter Pattern — Scenario B: Payment Gateway
//
// Problem: Your e-commerce system defines a PaymentProcessor interface.
//          Two payment SDKs already exist (Stripe, PayPal) with their own
//          incompatible APIs. You cannot modify those SDK classes.
//
// Your task: Implement the two adapters so the system can charge and refund
//            through either gateway using a single unified interface.
//
// How to work through this file:
//   1. Read the pre-written adaptees (StripeClient, PayPalSDK, PayPalResponse).
//   2. Read each TODO in order — they tell you exactly what to write.
//   3. Uncomment the corresponding test block in main() after each TODO.
//   4. Run main() and verify the output matches the expected comments.
// ─────────────────────────────────────────────────────────────────────────────

public class PaymentProcessorPractice {

    // ─────────────────────────────────────────────────────────────────────────
    // PRE-WRITTEN — Adaptee 1: Stripe SDK
    // Do NOT modify this class.
    // ─────────────────────────────────────────────────────────────────────────
    static class StripeClient {

        /**
         * Creates a charge on Stripe.
         *
         * @param cents          amount in integer cents (e.g. $15.50 → 1550)
         * @param stripeCustomer customer token expected by Stripe (e.g. "cus-alice")
         * @return               a Stripe charge ID (e.g. "ch_60e...")
         */
        public String createCharge(int cents, String stripeCustomer) {
            System.out.printf("  [StripeClient] createCharge(%d cents, %s)%n", cents, stripeCustomer);
            return "ch_" + Integer.toHexString(cents) + stripeCustomer.hashCode();
        }

        /**
         * Reverses a previously created charge.
         *
         * @param chargeId the Stripe charge ID returned by createCharge
         */
        public void reverseCharge(String chargeId) {
            System.out.printf("  [StripeClient] reverseCharge(%s)%n", chargeId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRE-WRITTEN — helper returned by PayPalSDK
    // Do NOT modify this class.
    // ─────────────────────────────────────────────────────────────────────────
    static class PayPalResponse {
        private final String  confirmationCode;
        private final boolean approved;

        PayPalResponse(String confirmationCode, boolean approved) {
            this.confirmationCode = confirmationCode;
            this.approved         = approved;
        }

        public String  getConfirmationCode() { return confirmationCode; }
        public boolean isApproved()          { return approved; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRE-WRITTEN — Adaptee 2: PayPal SDK
    // Do NOT modify this class.
    // ─────────────────────────────────────────────────────────────────────────
    static class PayPalSDK {

        /**
         * Submits a payment request to PayPal.
         *
         * @param amount       amount in USD (e.g. 15.50)
         * @param paypalEmail  the customer's PayPal email used as the customer ID
         * @return             a PayPalResponse with approval status and confirmation code
         */
        public PayPalResponse submitPayment(double amount, String paypalEmail) {
            boolean approved = !paypalEmail.contains("fail");
            String code = approved
                    ? "PP-" + (int)(amount * 100) + "-" + paypalEmail.length()
                    : "PP-DECLINED";
            System.out.printf("  [PayPalSDK] submitPayment(%.2f, %s) → approved=%b%n",
                    amount, paypalEmail, approved);
            return new PayPalResponse(code, approved);
        }

        /**
         * Cancels (refunds) a previously confirmed PayPal transaction.
         *
         * @param confirmationCode the code returned by submitPayment
         * @return true if cancellation succeeded, false otherwise
         */
        public boolean cancelTransaction(String confirmationCode) {
            boolean success = !confirmationCode.contains("FAIL");
            System.out.printf("  [PayPalSDK] cancelTransaction(%s) → %b%n", confirmationCode, success);
            return success;
        }
    }

    // =========================================================================
    // YOUR WORK STARTS HERE
    // Complete each TODO in order. Uncomment the matching test block in main()
    // after finishing each TODO.
    // =========================================================================

    // ── TODO 1: Define the PaymentProcessor interface (Target)
    //    Declare two methods:
    //
    //    charge(double amountUsd, String customerId) → String
    //      Processes a payment. Returns a transaction ID string.
    //
    //    refund(String transactionId) → boolean
    //      Refunds a previously charged transaction. Returns true if successful.

    interface PaymentProcessor {
        String charge(double amountUsd, String customerId);
        boolean refund(String transactionId);
    }   // shell — student writes the two method declarations inside


    // ── TODO 2: Declare a private final StripeClient field named "client" inside StripeAdapter
    //    The adaptee; set in constructor, never reassigned.

    // ── TODO 3: Write StripeAdapter(StripeClient client) constructor
    //    Store the parameter in the field from TODO 2.

    // ── TODO 4: Implement StripeAdapter.charge(double amountUsd, String customerId) → String
    //    Convert amountUsd to integer cents: (int) Math.round(amountUsd * 100)
    //    Build the Stripe customer string: "cus-" + customerId
    //    Call client.createCharge(cents, stripeCustomer) → chargeId
    //    Print: System.out.printf("[Stripe] Charged $%.2f → %s%n", amountUsd, chargeId)
    //    Return chargeId

    // ── TODO 5: Implement StripeAdapter.refund(String transactionId) → boolean
    //    Call client.reverseCharge(transactionId)   (returns void)
    //    Print: System.out.printf("[Stripe] Refunded %s%n", transactionId)
    //    Return true

    static class StripeAdapter implements PaymentProcessor {
        private final StripeClient client;

        public StripeAdapter(StripeClient client){
            this.client = client;
        }

        @Override
        public String charge(double amountUsd, String customerId) {
            if(amountUsd <= 0){
                throw new IllegalArgumentException("Amount in USD should be > 0");
            }
            if(customerId == null || customerId.isBlank()){
                throw new IllegalArgumentException("CustomerId should not be Blank or NULL");
            }
            int amountCents = (int) Math.round(amountUsd * 100);
            String stripeCustomerString = "cus-" + customerId;
            String chargeId = client.createCharge(amountCents, stripeCustomerString);
            System.out.printf("[Stripe] Charged $%.2f -> %s%n", amountUsd, chargeId);
            return chargeId;
        }

        @Override
        public boolean refund(String transactionId){
            if(transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException("TransactionID cannot be null or Blank");
            }
            client.reverseCharge(transactionId);
            System.out.printf("[Stripe] Refunded %s%n",  transactionId);
            return true;
        }

    }   // shell — student writes all members


    // ── TODO 6: Declare a private final PayPalSDK field named "sdk" inside PayPalAdapter
    //    The adaptee; set in constructor, never reassigned.

    // ── TODO 7: Write PayPalAdapter(PayPalSDK sdk) constructor
    //    Store the parameter in the field from TODO 6.

    // ── TODO 8: Implement PayPalAdapter.charge(double amountUsd, String customerId) → String
    //    Call sdk.submitPayment(amountUsd, customerId) → PayPalResponse response
    //    If !response.isApproved() → throw new IllegalStateException("PayPal payment declined")
    //    Print: System.out.printf("[PayPal] Charged $%.2f → %s%n", amountUsd, response.getConfirmationCode())
    //    Return response.getConfirmationCode()

    // ── TODO 9: Implement PayPalAdapter.refund(String transactionId) → boolean
    //    Call sdk.cancelTransaction(transactionId) → boolean success
    //    Print: System.out.printf("[PayPal] Refund %s: %s%n", transactionId, success ? "OK" : "FAILED")
    //    Return success

    static class PayPalAdapter implements PaymentProcessor {
        private final PayPalSDK sdk;
        public PayPalAdapter(PayPalSDK sdk){
            this.sdk = sdk;
        }

        @Override
        public String charge(double amountUsd, String customerId) {
            if(amountUsd <= 0){
                throw new IllegalArgumentException("Amount in USD should be > 0");
            }
            if(customerId == null || customerId.isBlank()){
                throw new IllegalArgumentException("CustomerId should not be Blank or NULL");
            }
            PayPalResponse response = sdk.submitPayment(amountUsd, customerId);
            if(!response.isApproved()) {
                throw new IllegalStateException("Paypal payment declined");
            }
            System.out.printf("[PayPal] Charged $%.2f → %s%n", amountUsd, response.getConfirmationCode());
            return response.getConfirmationCode();
        }

        @Override
        public boolean refund(String transactionId){
            if(transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException("TransactionID cannot be null or Blank");
            }
            boolean success = sdk.cancelTransaction(transactionId);
            System.out.printf("[PayPal] Refund %s : %s%n", transactionId, success ?  "OK" : "FAILED");
            return success;
        }
    }   // shell — student writes all members


    // ─────────────────────────────────────────────────────────────────────────
    // DO NOT MODIFY main() — uncomment each block after finishing the TODO it names
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: StripeAdapter.charge returns a non-blank ID (uncomment after TODO 4) ───
         StripeClient stripe = new StripeClient();
         StripeAdapter stripeAdapter = new StripeAdapter(stripe);
         String id1 = stripeAdapter.charge(15.50, "alice");
         System.out.println("Test 1 — non-blank charge ID: " + (!id1.isBlank() ? "PASSED" : "FAILED"));

        // ── Test 2: cents conversion — $15.50 → hex(1550)="60e" in charge ID (uncomment after TODO 4) ─
         String id2 = stripeAdapter.charge(15.50, "bob");
         System.out.println("Test 2 — cents conversion: " + (id2.startsWith("ch_60e") ? "PASSED" : "FAILED (got: " + id2 + ")"));

        // ── Test 3: StripeAdapter.refund returns true (uncomment after TODO 5) ────────────
         boolean r3 = stripeAdapter.refund("ch_abc123");
         System.out.println("Test 3 — stripe refund returns true: " + (r3 ? "PASSED" : "FAILED"));

        // ── Test 4: PayPalAdapter.charge approved returns PP-... code (uncomment after TODO 8) ──
         PayPalSDK paypal = new PayPalSDK();
         PayPalAdapter paypalAdapter = new PayPalAdapter(paypal);
         String id4 = paypalAdapter.charge(25.00, "user@example.com");
         System.out.println("Test 4 — paypal charge approved: " + (id4.startsWith("PP-") ? "PASSED" : "FAILED"));

        // ── Test 5: PayPalAdapter.charge declined throws ISE (uncomment after TODO 8) ──────
         boolean t5 = false;
         try {
             paypalAdapter.charge(10.00, "fail@example.com");
         } catch (IllegalStateException e) {
             t5 = "PayPal payment declined".equals(e.getMessage());
         }
         System.out.println("Test 5 — paypal charge declined throws ISE: " + (t5 ? "PASSED" : "FAILED"));

        // ── Test 6: PayPalAdapter.refund success returns true (uncomment after TODO 9) ─────
         boolean r6 = paypalAdapter.refund("PP-2500-15");
         System.out.println("Test 6 — paypal refund success: " + (r6 ? "PASSED" : "FAILED"));

        // ── Test 7: PayPalAdapter.refund fail returns false (uncomment after TODO 9) ───────
         boolean r7 = paypalAdapter.refund("PP-FAIL-001");
         System.out.println("Test 7 — paypal refund fail: " + (!r7 ? "PASSED" : "FAILED"));

        // ── Test 8: Polymorphic use via PaymentProcessor[] (uncomment after all TODOs) ─────
         PaymentProcessor[] gateways = { stripeAdapter, paypalAdapter };
         String[] customers = { "carol", "dave@example.com" };
         boolean t8 = true;
         for (int i = 0; i < gateways.length; i++) {
             String txId = gateways[i].charge(5.00, customers[i]);
             if (txId == null || txId.isBlank()) t8 = false;
         }
         System.out.println("Test 8 — polymorphic charge: " + (t8 ? "PASSED" : "FAILED"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HINTS — read only if stuck
    // ─────────────────────────────────────────────────────────────────────────

    /*
     * HINT 1 (Gentle)
     * ───────────────
     * The two SDK classes (StripeClient, PayPalSDK) have useful behaviour but speak
     * a different "language" than the interface your system expects. You need a
     * translator for each one — a class that speaks both languages at once.
     * Think about how you'd hold a reference to the SDK object and forward calls to it.
     */

    /*
     * HINT 2 (Direct)
     * ───────────────
     * This is the Object Adapter pattern. Each adapter:
     *   - implements PaymentProcessor  (so the system can use it polymorphically)
     *   - holds the SDK as a private final field set in the constructor  (composition)
     *   - translates each PaymentProcessor method call into the SDK's API
     * For StripeAdapter: amountUsd → cents = (int) Math.round(amountUsd * 100)
     *                    customerId → stripeCustomer = "cus-" + customerId
     * For PayPalAdapter: check response.isApproved() and throw ISE if false
     */

    /*
     * HINT 3 (Near-solution — class skeleton only, no method bodies)
     * ───────────────────────────────────────────────────────────────
     *
     * interface PaymentProcessor {
     *     String  charge(double amountUsd, String customerId);
     *     boolean refund(String transactionId);
     * }
     *
     * static class StripeAdapter implements PaymentProcessor {
     *     private final StripeClient client;
     *     StripeAdapter(StripeClient client) { ... }
     *
     *     @Override public String  charge(double amountUsd, String customerId) { ... }
     *     @Override public boolean refund(String transactionId)                { ... }
     * }
     *
     * static class PayPalAdapter implements PaymentProcessor {
     *     private final PayPalSDK sdk;
     *     PayPalAdapter(PayPalSDK sdk) { ... }
     *
     *     @Override public String  charge(double amountUsd, String customerId) { ... }
     *     @Override public boolean refund(String transactionId)                { ... }
     * }
     */
}

package com.ramkumar.lld.designpatterns.structural.adapter.results;

// ─────────────────────────────────────────────────────────────────────────────
// Reference Solution — Adapter Pattern (Scenario B: Payment Gateway)
// ─────────────────────────────────────────────────────────────────────────────

public class AdapterReference {

    // ── Adaptees (pre-written, unchanged) ────────────────────────────────────

    static class StripeClient {
        public String createCharge(int cents, String stripeCustomer) {
            System.out.printf("  [StripeClient] createCharge(%d cents, %s)%n", cents, stripeCustomer);
            return "ch_" + Integer.toHexString(cents) + stripeCustomer.hashCode();
        }
        public void reverseCharge(String chargeId) {
            System.out.printf("  [StripeClient] reverseCharge(%s)%n", chargeId);
        }
    }

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

    static class PayPalSDK {
        public PayPalResponse submitPayment(double amount, String paypalEmail) {
            boolean approved = !paypalEmail.contains("fail");
            String code = approved
                    ? "PP-" + (int)(amount * 100) + "-" + paypalEmail.length()
                    : "PP-DECLINED";
            System.out.printf("  [PayPalSDK] submitPayment(%.2f, %s) → approved=%b%n",
                    amount, paypalEmail, approved);
            return new PayPalResponse(code, approved);
        }
        public boolean cancelTransaction(String confirmationCode) {
            boolean success = !confirmationCode.contains("FAIL");
            System.out.printf("  [PayPalSDK] cancelTransaction(%s) → %b%n", confirmationCode, success);
            return success;
        }
    }

    // ── Target interface ──────────────────────────────────────────────────────

    interface PaymentProcessor {
        String  charge(double amountUsd, String customerId);
        boolean refund(String transactionId);
    }

    // ── Object Adapter 1: Stripe ──────────────────────────────────────────────

    static class StripeAdapter implements PaymentProcessor {

        // [Composition] — hold the adaptee; never extend it
        private final StripeClient client;

        StripeAdapter(StripeClient client) {
            this.client = client;
        }

        @Override
        public String charge(double amountUsd, String customerId) {
            // [Translation] USD (double) → Stripe's integer-cents API
            int    cents          = (int) Math.round(amountUsd * 100); // Math.round avoids float truncation
            String stripeCustomer = "cus-" + customerId;               // Stripe-specific customer prefix

            // [Delegation] forward to adaptee — no business logic here
            String chargeId = client.createCharge(cents, stripeCustomer);

            System.out.printf("[Stripe] Charged $%.2f → %s%n", amountUsd, chargeId); // Unicode →, not ASCII ->
            return chargeId;
        }

        @Override
        public boolean refund(String transactionId) {
            // [Delegation] reverseCharge returns void; we translate that to boolean true
            client.reverseCharge(transactionId);
            System.out.printf("[Stripe] Refunded %s%n", transactionId);
            return true; // Stripe always succeeds or throws — void return means success
        }
    }

    // ── Object Adapter 2: PayPal ──────────────────────────────────────────────

    static class PayPalAdapter implements PaymentProcessor {

        // [Composition] — hold the adaptee; never extend it
        private final PayPalSDK sdk;

        PayPalAdapter(PayPalSDK sdk) {
            this.sdk = sdk;
        }

        @Override
        public String charge(double amountUsd, String customerId) {
            // [Delegation] PayPal uses email-as-customerId and returns a response object
            PayPalResponse response = sdk.submitPayment(amountUsd, customerId);

            // [Translation] PayPal signals failure via response object; our interface uses ISE
            // NOTE: exact message string is part of the contract — case matters
            if (!response.isApproved()) {
                throw new IllegalStateException("PayPal payment declined"); // "PayPal" — capital P
            }

            System.out.printf("[PayPal] Charged $%.2f → %s%n", amountUsd, response.getConfirmationCode());
            return response.getConfirmationCode();
        }

        @Override
        public boolean refund(String transactionId) {
            // [Delegation] cancelTransaction returns boolean — no translation needed
            boolean success = sdk.cancelTransaction(transactionId);
            System.out.printf("[PayPal] Refund %s: %s%n", transactionId, success ? "OK" : "FAILED"); // no space before ":"
            return success;
        }
    }

    // ── main() ────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        StripeClient stripe = new StripeClient();
        PayPalSDK    paypal = new PayPalSDK();

        StripeAdapter stripeAdapter = new StripeAdapter(stripe);
        PayPalAdapter paypalAdapter = new PayPalAdapter(paypal);

        int passed = 0, total = 9;

        // Test 1: StripeAdapter.charge returns non-blank ID
        System.out.println("\n[Test 1] StripeAdapter.charge($15.50, \"alice\")");
        String id1 = stripeAdapter.charge(15.50, "alice");
        check("non-blank charge ID", !id1.isBlank()); if (!id1.isBlank()) passed++;

        // Test 2: cents conversion — $15.50 → hex(1550) = "60e"
        System.out.println("\n[Test 2] Cents conversion: $15.50 → 1550 cents → hex 60e");
        String id2 = stripeAdapter.charge(15.50, "bob");
        check("charge ID starts with ch_60e", id2.startsWith("ch_60e")); if (id2.startsWith("ch_60e")) passed++;

        // Test 3: StripeAdapter.refund returns true
        System.out.println("\n[Test 3] StripeAdapter.refund");
        boolean r3 = stripeAdapter.refund("ch_abc123");
        check("refund returns true", r3); if (r3) passed++;

        // Test 4: PayPalAdapter.charge approved
        System.out.println("\n[Test 4] PayPalAdapter.charge approved");
        String id4 = paypalAdapter.charge(25.00, "user@example.com");
        check("confirmation code starts PP-", id4.startsWith("PP-")); if (id4.startsWith("PP-")) passed++;

        // Test 5: PayPalAdapter.charge declined → ISE with exact message
        System.out.println("\n[Test 5] PayPalAdapter.charge declined → ISE");
        boolean t5 = false;
        try {
            paypalAdapter.charge(10.00, "fail@example.com");
        } catch (IllegalStateException e) {
            t5 = "PayPal payment declined".equals(e.getMessage()); // exact case: "PayPal", not "Paypal"
        }
        check("ISE with exact message \"PayPal payment declined\"", t5); if (t5) passed++;

        // Test 6: PayPalAdapter.refund success
        System.out.println("\n[Test 6] PayPalAdapter.refund success");
        boolean r6 = paypalAdapter.refund("PP-2500-15");
        check("refund returns true", r6); if (r6) passed++;

        // Test 7: PayPalAdapter.refund fail
        System.out.println("\n[Test 7] PayPalAdapter.refund fail");
        boolean r7 = paypalAdapter.refund("PP-FAIL-001");
        check("refund returns false", !r7); if (!r7) passed++;

        // Test 8: Polymorphic use via PaymentProcessor[]
        System.out.println("\n[Test 8] Polymorphic PaymentProcessor[]");
        PaymentProcessor[] gateways  = { stripeAdapter, paypalAdapter };
        String[]           customers = { "carol", "dave@example.com" };
        boolean t8 = true;
        for (int i = 0; i < gateways.length; i++) {
            String txId = gateways[i].charge(5.00, customers[i]);
            if (txId == null || txId.isBlank()) t8 = false;
        }
        check("both gateways return non-blank ID via interface", t8); if (t8) passed++;

        // Test 9 (catches the most common mistake — validation in adapter)
        // The adapter must NOT throw when given a valid-looking negative amount;
        // that validation is the caller's job, not the adapter's.
        // Here we verify the adapter just forwards the call and lets the SDK decide.
        System.out.println("\n[Test 9] Adapter forwards negative amount without throwing (no adapter-level validation)");
        boolean t9 = false;
        try {
            // StripeClient will accept -100 cents — no ISE, no IAE from the adapter itself
            stripeAdapter.charge(-1.00, "test");
            t9 = true; // reached here → adapter did not validate; it forwarded
        } catch (IllegalArgumentException e) {
            // adapter added its own validation — this is the design mistake
            System.out.println("  Adapter threw IAE: " + e.getMessage() + " ← adapter should NOT validate");
        }
        check("adapter does not add its own input validation", t9); if (t9) passed++;

        System.out.printf("%n══════════════════════════════%n");
        System.out.printf("Results: %d / %d PASSED%n", passed, total);
        System.out.printf("══════════════════════════════%n");
    }

    private static void check(String label, boolean condition) {
        System.out.println("  " + label + ": " + (condition ? "PASSED" : "FAILED"));
    }
}

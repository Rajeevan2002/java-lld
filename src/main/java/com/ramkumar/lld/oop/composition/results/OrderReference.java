package com.ramkumar.lld.oop.composition.results;

/**
 * Reference solution for the E-Commerce Order Pricing System.
 *
 * Key fixes vs the practice submission:
 *  1. getSummary() uses shippingStrategy.getName() — not hardcoded "Express"
 *  2. FlatDiscount validates amount > 0 (not >= 0) — 0 is a no-op, reject it
 *  3. upgradeShipping() and applyNewDiscount() null-check their arguments
 *  4. Strategy getters removed — Law of Demeter; expose data, not objects
 *  5. CreditCardPayment uses "\\d{4}" — one regex that encodes both constraints
 *  6. Error message doesn't say "null" for a primitive double
 */
public class OrderReference {

    // =========================================================================
    // INTERFACE — DiscountStrategy
    // Stateless contract: given a price, return the discounted price.
    // =========================================================================

    interface DiscountStrategy {
        double apply(double price);     // never return < 0
        String getDescription();
    }

    // =========================================================================
    // DISCOUNT IMPLEMENTATIONS — all stateless (NoDiscount has no fields at all)
    // =========================================================================

    // Null Object pattern: a valid do-nothing implementation
    // Better than null: new NoDiscount() signals intent; null causes NPE
    static class NoDiscount implements DiscountStrategy {
        @Override public double apply(double price)  { return price; }
        @Override public String getDescription()     { return "No discount"; }
    }

    static class PercentageDiscount implements DiscountStrategy {
        private final double percent;

        public PercentageDiscount(double percent) {
            if (percent <= 0 || percent > 100)
                throw new IllegalArgumentException(
                        "Percent must be in (0, 100], got: " + percent);
            this.percent = percent;
        }

        @Override public double apply(double price) { return price * (1.0 - percent / 100.0); }
        @Override public String getDescription()    { return String.format("%.1f%% off", percent); }
    }

    static class FlatDiscount implements DiscountStrategy {
        private final double amount;

        public FlatDiscount(double amount) {
            if (amount <= 0)   // <= 0, not < 0 — a zero flat discount is a no-op, reject it
                throw new IllegalArgumentException(
                        "Flat discount amount must be > 0, got: " + amount);
            this.amount = amount;
        }

        @Override public double apply(double price) { return Math.max(0, price - amount); }
        @Override public String getDescription()    { return String.format("₹%.1f flat off", amount); }
    }

    // =========================================================================
    // INTERFACE — ShippingStrategy
    // =========================================================================

    interface ShippingStrategy {
        double calculateCost(double weightKg);
        int    getEstimatedDays();
        String getName();
    }

    // =========================================================================
    // SHIPPING IMPLEMENTATIONS — all stateless, no fields needed
    // =========================================================================

    static class StandardShipping implements ShippingStrategy {
        @Override public double calculateCost(double w) { return 50 + w * 10; }
        @Override public int    getEstimatedDays()      { return 5; }
        @Override public String getName()               { return "Standard"; }
    }

    static class ExpressShipping implements ShippingStrategy {
        @Override public double calculateCost(double w) { return 150 + w * 25; }
        @Override public int    getEstimatedDays()      { return 2; }
        @Override public String getName()               { return "Express"; }
    }

    static class FreeShipping implements ShippingStrategy {
        @Override public double calculateCost(double w) { return 0; }
        @Override public int    getEstimatedDays()      { return 7; }
        @Override public String getName()               { return "Free"; }
    }

    // =========================================================================
    // INTERFACE — PaymentBehavior
    // =========================================================================

    interface PaymentBehavior {
        boolean processPayment(double amount);
        String  getPaymentMethod();
    }

    // =========================================================================
    // PAYMENT IMPLEMENTATIONS
    // =========================================================================

    static class CreditCardPayment implements PaymentBehavior {
        private final String lastFourDigits;

        public CreditCardPayment(String lastFourDigits) {
            // "\\d{4}" encodes BOTH constraints in one regex: exactly 4 digits
            if (lastFourDigits == null || !lastFourDigits.matches("\\d{4}"))
                throw new IllegalArgumentException(
                        "Last four digits must be exactly 4 numeric characters");
            this.lastFourDigits = lastFourDigits;
        }

        @Override public boolean processPayment(double amount) { return true; }
        @Override public String  getPaymentMethod()            { return "CREDIT_CARD"; }
    }

    static class UPIPayment implements PaymentBehavior {
        private final String upiId;

        public UPIPayment(String upiId) {
            if (upiId == null || upiId.isBlank() || !upiId.contains("@"))
                throw new IllegalArgumentException(
                        "UPI ID must contain '@', got: " + upiId);
            this.upiId = upiId;
        }

        @Override public boolean processPayment(double amount) { return true; }
        @Override public String  getPaymentMethod()            { return "UPI"; }
    }

    static class CashPayment implements PaymentBehavior {
        @Override public boolean processPayment(double amount) { return true; }
        @Override public String  getPaymentMethod()            { return "CASH"; }
    }

    // =========================================================================
    // ORDER — composed, not extended
    //
    // HAS-A DiscountStrategy  (mutable — runtime swappable)
    // HAS-A ShippingStrategy  (mutable — runtime swappable)
    // HAS-A PaymentBehavior   (mutable — runtime swappable)
    //
    // Order contains ZERO pricing/shipping math.
    // Every calculation is delegated to the composed behavior object.
    // =========================================================================

    static class Order {

        // Immutable data — describes what is being ordered
        private final String orderId;
        private final String itemName;
        private final double basePrice;
        private final double weightKg;

        // Mutable behaviors — describe HOW the order is processed
        // Not final so they can be swapped at runtime
        private DiscountStrategy discountStrategy;
        private ShippingStrategy shippingStrategy;
        private PaymentBehavior  paymentBehavior;

        public Order(String orderId, String itemName,
                     double basePrice, double weightKg,
                     DiscountStrategy discount,
                     ShippingStrategy shipping,
                     PaymentBehavior  payment) {
            if (basePrice <= 0) throw new IllegalArgumentException("basePrice must be > 0");
            if (weightKg  <= 0) throw new IllegalArgumentException("weightKg must be > 0");
            // Note: error message says "must be > 0" not "cannot be null" — doubles can't be null
            this.orderId          = orderId;
            this.itemName         = itemName;
            this.basePrice        = basePrice;
            this.weightKg         = weightKg;
            this.discountStrategy = discount;
            this.shippingStrategy = shipping;
            this.paymentBehavior  = payment;
        }

        // DELEGATION — Order adds the two delegated values; does no math itself
        public double getFinalPrice() {
            return discountStrategy.apply(basePrice)
                 + shippingStrategy.calculateCost(weightKg);
        }

        // DELEGATION — single-line; Order doesn't know how payment works
        public boolean checkout() {
            return paymentBehavior.processPayment(getFinalPrice());
        }

        // DELEGATION — every field in the summary comes from the composed object
        // KEY FIX: shippingStrategy.getName() not hardcoded "Express"
        public String getSummary() {
            return String.format(
                    "[%s] %s | Base: ₹%.0f | Discount: %s | Shipping: %s (%dd) | Payment: %s | Final: ₹%.2f",
                    orderId, itemName, basePrice,
                    discountStrategy.getDescription(),   // from composed object
                    shippingStrategy.getName(),          // from composed object — NOT hardcoded
                    shippingStrategy.getEstimatedDays(), // from composed object
                    paymentBehavior.getPaymentMethod(),  // from composed object
                    getFinalPrice());
        }

        // RUNTIME SWAP — null guard required; every public mutation point is a trust boundary
        public void upgradeShipping(ShippingStrategy s) {
            if (s == null) throw new IllegalArgumentException("Shipping strategy cannot be null");
            this.shippingStrategy = s;
        }

        public void applyNewDiscount(DiscountStrategy d) {
            if (d == null) throw new IllegalArgumentException("Discount strategy cannot be null");
            this.discountStrategy = d;
        }

        // LAW OF DEMETER: expose data the caller needs, not the internal object
        // Do NOT expose getShippingStrategy() — that lets callers bypass Order
        public String getOrderId()         { return orderId; }
        public String getItemName()        { return itemName; }
        public double getBasePrice()       { return basePrice; }
        public double getWeightKg()        { return weightKg; }
        public String getShippingName()    { return shippingStrategy.getName(); }     // data, not object
        public int    getEstimatedDays()   { return shippingStrategy.getEstimatedDays(); }
        public String getDiscountInfo()    { return discountStrategy.getDescription(); }
        public String getPaymentMethod()   { return paymentBehavior.getPaymentMethod(); }
    }

    // =========================================================================
    // Main — same 9 test cases + 1 extra catching the most common mistake
    // =========================================================================

    public static void main(String[] args) {

        // Test 1
        Order order1 = new Order("O001", "Laptop", 50_000, 2.5,
                new NoDiscount(), new StandardShipping(), new UPIPayment("user@bank"));
        System.out.printf("Test 1: getFinalPrice = ₹%.2f (expected ₹50075.00)%n", order1.getFinalPrice());
        assert Math.abs(order1.getFinalPrice() - 50_075.0) < 0.01;
        System.out.println("Test 1 PASSED");

        // Test 2
        Order order2 = new Order("O002", "Phone", 50_000, 2.5,
                new PercentageDiscount(10), new ExpressShipping(), new CreditCardPayment("1234"));
        System.out.printf("Test 2: getFinalPrice = ₹%.2f (expected ₹45212.50)%n", order2.getFinalPrice());
        assert Math.abs(order2.getFinalPrice() - 45_212.5) < 0.01;
        System.out.println("Test 2 PASSED");

        // Test 3
        Order order3 = new Order("O003", "Book", 50_000, 0.5,
                new FlatDiscount(60_000), new FreeShipping(), new CashPayment());
        System.out.printf("Test 3: FlatDiscount floors at 0 → ₹%.2f (expected ₹0.00)%n", order3.getFinalPrice());
        assert order3.getFinalPrice() == 0.0;
        System.out.println("Test 3 PASSED");

        // Test 4
        assert order1.checkout();
        System.out.println("Test 4 PASSED: checkout() = true");

        // Test 5: upgradeShipping runtime swap
        double before = order1.getFinalPrice();
        order1.upgradeShipping(new ExpressShipping());
        double after = order1.getFinalPrice();
        System.out.printf("Test 5: Before=₹%.2f After=₹%.2f (Express > Standard)%n", before, after);
        assert after > before;
        System.out.println("Test 5 PASSED");

        // Test 6: applyNewDiscount runtime swap
        double beforeD = order1.getFinalPrice();
        order1.applyNewDiscount(new PercentageDiscount(20));
        double afterD = order1.getFinalPrice();
        System.out.printf("Test 6: Before=₹%.2f After=₹%.2f (20%% applied)%n", beforeD, afterD);
        assert afterD < beforeD;
        System.out.println("Test 6 PASSED");

        // Test 7: getSummary() — ALL fields from composed objects
        System.out.println("\n── Test 7: getSummary() ──────────────────────────────");
        System.out.println(order1.getSummary());
        System.out.println(order2.getSummary());
        System.out.println(order3.getSummary());

        // Test 8: Validation
        System.out.println("\n── Test 8: Validation ────────────────────────────────");
        try { new PercentageDiscount(0);   } catch (IllegalArgumentException e) { System.out.println("Test 8a PASSED: " + e.getMessage()); }
        try { new PercentageDiscount(101); } catch (IllegalArgumentException e) { System.out.println("Test 8b PASSED: " + e.getMessage()); }

        // Test 9: CreditCard validation
        try { new CreditCardPayment("12AB"); } catch (IllegalArgumentException e) { System.out.println("Test 9 PASSED: " + e.getMessage()); }

        // Test 10 (EXTRA): getSummary() must reflect runtime swap — catches hardcoded "Express"
        System.out.println("\n── Test 10 (extra): getSummary() updates after runtime swap ──");
        Order order4 = new Order("O004", "Tablet", 30_000, 1.0,
                new NoDiscount(), new ExpressShipping(), new CashPayment());
        System.out.println("Before swap: " + order4.getSummary());
        assert order4.getSummary().contains("Express") : "Should say Express before swap";

        order4.upgradeShipping(new FreeShipping());
        System.out.println("After swap:  " + order4.getSummary());
        assert order4.getSummary().contains("Free")    : "Should say Free after swap";
        assert !order4.getSummary().contains("Express"): "FAILED: hardcoded 'Express' detected!";
        System.out.println("Test 10 PASSED: getSummary() correctly delegates shipping name");

        System.out.println("\nAll tests completed.");
    }
}

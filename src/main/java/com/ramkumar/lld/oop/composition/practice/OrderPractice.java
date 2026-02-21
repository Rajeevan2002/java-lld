package com.ramkumar.lld.oop.composition.practice;

/**
 * ============================================================
 *  PRACTICE: E-Commerce Order Pricing System
 * ============================================================
 *
 * Problem Statement:
 * ------------------
 * Design an e-commerce order system where pricing, shipping, and
 * payment behaviours are COMPOSED into an Order — not inherited.
 *
 * The class explosion problem (inheritance approach — DO NOT use):
 *   Order
 *   ├── NoDiscountOrder
 *   │   ├── NoDiscountStandardShippingCreditCardOrder
 *   │   ├── NoDiscountExpressShippingUPIOrder  ...
 *   └── PercentageDiscountOrder
 *       └── ...  3 discounts × 3 shipping × 3 payments = 27 subclasses
 *
 * Your solution (composition approach):
 *   Order HAS-A DiscountStrategy + HAS-A ShippingStrategy + HAS-A PaymentBehavior
 *   3 + 3 + 3 = 9 behaviour classes. 0 Order subclasses.
 *
 * ── Interface 1: DiscountStrategy ─────────────────────────────
 * Methods:
 *   1. double apply(double price)    — return the discounted price; never return < 0
 *   2. String getDescription()       — human-readable e.g. "20% off", "₹500 flat off"
 *
 * Implementations:
 *   a. NoDiscount              → price unchanged,  "No discount"
 *   b. PercentageDiscount(double percent)
 *      → price * (1 - percent/100); validate 0 < percent <= 100
 *      → description: "percent% off"
 *   c. FlatDiscount(double amount)
 *      → Math.max(0, price - amount); validate amount > 0
 *      → description: "₹amount flat off"
 *
 * ── Interface 2: ShippingStrategy ─────────────────────────────
 * Methods:
 *   1. double calculateCost(double weightKg) — shipping cost; validate weight > 0
 *   2. int    getEstimatedDays()             — delivery window in days
 *   3. String getName()                      — "Standard", "Express", "Free"
 *
 * Implementations:
 *   a. StandardShipping → ₹50 base + ₹10/kg,   5 days
 *   b. ExpressShipping  → ₹150 base + ₹25/kg,  2 days
 *   c. FreeShipping     → ₹0,                  7 days
 *
 * ── Interface 3: PaymentBehavior ──────────────────────────────
 * Methods:
 *   1. boolean processPayment(double amount) — return true on success
 *   2. String  getPaymentMethod()            — "CREDIT_CARD", "UPI", "CASH"
 *
 * Implementations:
 *   a. CreditCardPayment(String lastFourDigits)
 *      → validate lastFourDigits is exactly 4 numeric chars
 *      → processPayment() always returns true
 *   b. UPIPayment(String upiId)
 *      → validate upiId contains "@"
 *      → processPayment() always returns true
 *   c. CashPayment → always returns true, no extra fields
 *
 * ── Class: Order (NOT abstract — composed, not extended) ──────
 * Fields (all final):
 *   1. orderId    (String)  — immutable
 *   2. itemName   (String)  — immutable
 *   3. basePrice  (double)  — validate > 0
 *   4. weightKg   (double)  — validate > 0
 *
 * Composed behaviour fields (mutable — can be swapped at runtime):
 *   5. discountStrategy  (DiscountStrategy)
 *   6. shippingStrategy  (ShippingStrategy)
 *   7. paymentBehavior   (PaymentBehavior)
 *
 * Constructor:
 *   Order(orderId, itemName, basePrice, weightKg,
 *         discountStrategy, shippingStrategy, paymentBehavior)
 *   → validate basePrice > 0 and weightKg > 0
 *
 * Methods:
 *   1. double getFinalPrice()
 *      → discountStrategy.apply(basePrice) + shippingStrategy.calculateCost(weightKg)
 *      → DELEGATE to composed objects — Order does no math itself
 *
 *   2. boolean checkout()
 *      → paymentBehavior.processPayment(getFinalPrice())
 *
 *   3. String getSummary()
 *      → one-line readable summary:
 *         "[O001] Laptop | Base: ₹50000 | Discount: 20% off | Shipping: Express (2d)
 *          | Payment: CREDIT_CARD | Final: ₹41150.00"
 *
 *   4. void upgradeShipping(ShippingStrategy s)   — RUNTIME SWAP
 *      → validate s != null; replace shippingStrategy
 *
 *   5. void applyNewDiscount(DiscountStrategy d)  — RUNTIME SWAP
 *      → validate d != null; replace discountStrategy
 *
 *   Getters: orderId, itemName, basePrice, weightKg
 *
 * Design Constraints:
 * -------------------
 *  - Order must NOT extend any class (other than Object)
 *  - Order must NOT contain any pricing/shipping math directly —
 *    all math must be delegated to the composed behaviour objects
 *  - No instanceof chains in Order or any service class
 *  - Behaviour classes (PercentageDiscount, ExpressShipping, etc.)
 *    must be stateless where possible — pure input → output
 *
 * ============================================================
 *  Write your solution below. Delete this comment block when done.
 * ============================================================
 */
public class OrderPractice {

    // =========================================================================
    // ── TODO 1: Declare interface DiscountStrategy ────────────────────────────
    //    Methods: apply(double price), getDescription()
    // =========================================================================
    interface DiscountStrategy {
        double apply(double price);
        String getDescription();

    }

    // =========================================================================
    // ── TODO 2: Implement NoDiscount ─────────────────────────────────────────
    //    apply() returns price unchanged
    //    getDescription() returns "No discount"
    // =========================================================================
    static class NoDiscount implements DiscountStrategy {
        @Override
        public double apply(double price){
            return price;
        }

        @Override
        public String getDescription(){
            return "No discount";
        }
    }


    // =========================================================================
    // ── TODO 3: Implement PercentageDiscount ──────────────────────────────────
    //    Field: percent (double, final) — validate 0 < percent <= 100
    //    apply() returns price * (1 - percent/100)
    //    getDescription() returns e.g. "20.0% off"
    // =========================================================================
    static class PercentageDiscount implements DiscountStrategy {
        private final double percent;
        public PercentageDiscount(double percent) {
            if(percent<=0 || percent >100){
                throw new IllegalArgumentException("Percentage Cannot be less than 0 or Greater than 100");
            }
            this.percent = percent;
        }

        @Override public double apply(double price) { return  price * ( 1.0 - percent / 100.0 ); }

        @Override public String getDescription(){ return String.format("%.1f%% off", percent); }
    }

    // =========================================================================
    // ── TODO 4: Implement FlatDiscount ───────────────────────────────────────
    //    Field: amount (double, final) — validate > 0
    //    apply() returns Math.max(0, price - amount)
    //    getDescription() returns e.g. "₹500.0 flat off"
    // =========================================================================
    static class FlatDiscount implements DiscountStrategy {
        private final double amount;
        public FlatDiscount(double amount){
            if(amount<=0){
                throw new IllegalArgumentException("Amount Cannot be Less than Zero");
            }
            this.amount = amount;
        }

        @Override public double apply(double price) { return Math.max(0, price - amount); }
        @Override public String getDescription(){ return String.format("₹%.1f flat off", amount); }
    }

    // =========================================================================
    // ── TODO 5: Declare interface ShippingStrategy ───────────────────────────
    //    Methods: calculateCost(double weightKg), getEstimatedDays(), getName()
    // =========================================================================
    interface ShippingStrategy {
        double calculateCost(double weightKg);
        int getEstimatedDays();
        String getName();
    }

    // =========================================================================
    // ── TODO 6: Implement StandardShipping ───────────────────────────────────
    //    calculateCost() → 50 + weight * 10
    //    getEstimatedDays() → 5
    //    getName() → "Standard"
    // =========================================================================
    static class StandardShipping implements ShippingStrategy{
        @Override
        public double calculateCost(double weightKg) {
            return 50 + weightKg * 10;
        }

        @Override public int getEstimatedDays() { return 5; }
        @Override public String getName() { return "Standard"; }
    }

    // =========================================================================
    // ── TODO 7: Implement ExpressShipping ────────────────────────────────────
    //    calculateCost() → 150 + weight * 25
    //    getEstimatedDays() → 2
    //    getName() → "Express"
    // =========================================================================
    static class ExpressShipping implements ShippingStrategy{
        @Override
        public double calculateCost(double weightKg) {
            return 150 + weightKg * 25;
        }

        @Override public int getEstimatedDays() { return 2; }
        @Override public String getName() { return "Express"; }
    }

    // =========================================================================
    // ── TODO 8: Implement FreeShipping ───────────────────────────────────────
    //    calculateCost() → 0
    //    getEstimatedDays() → 7
    //    getName() → "Free"
    // =========================================================================
    static class FreeShipping implements ShippingStrategy{
        @Override
        public double calculateCost(double weightKg) {
            return 0;
        }

        @Override public int getEstimatedDays() { return 7; }
        @Override public String getName() { return "Free"; }
    }

    // =========================================================================
    // ── TODO 9: Declare interface PaymentBehavior ─────────────────────────────
    //    Methods: processPayment(double amount), getPaymentMethod()
    // =========================================================================
    interface PaymentBehavior {
        boolean processPayment (double amount);
        String getPaymentMethod();
    }

    // =========================================================================
    // ── TODO 10: Implement CreditCardPayment ─────────────────────────────────
    //    Field: lastFourDigits (String, final) — validate exactly 4 numeric chars
    //    processPayment() → always true
    //    getPaymentMethod() → "CREDIT_CARD"
    // =========================================================================
    static class CreditCardPayment implements PaymentBehavior {
        private final String lastFourDigits;

        public CreditCardPayment(String lastFourDigits){
            if((lastFourDigits == null || lastFourDigits.isBlank()) ||
               (lastFourDigits.length() != 4 || !lastFourDigits.matches("\\d{4}"))) {
                throw  new IllegalArgumentException("Last Four Digits should only have numbers and lenfth should be 4");
            }
            this.lastFourDigits = lastFourDigits;
        }

        @Override
        public boolean processPayment(double amount) { return true; }

        @Override
        public String getPaymentMethod() { return "CREDIT_CARD"; }
    }

    // =========================================================================
    // ── TODO 11: Implement UPIPayment ────────────────────────────────────────
    //    Field: upiId (String, final) — validate contains "@"
    //    processPayment() → always true
    //    getPaymentMethod() → "UPI"
    // =========================================================================
    static class UPIPayment implements PaymentBehavior {
        private final String upiId;

        public  UPIPayment(String upiId){
            if(upiId == null || upiId.isBlank() || !upiId.contains("@")) {
                throw new IllegalArgumentException("UPI ID shouldn't be null or blank and should contain '@'");
            }
            this.upiId = upiId;
        }

        @Override
        public boolean processPayment(double amount) { return true; }

        @Override
        public String getPaymentMethod() { return "UPI"; }
    }

    // =========================================================================
    // ── TODO 12: Implement CashPayment ───────────────────────────────────────
    //    No extra fields
    //    processPayment() → always true
    //    getPaymentMethod() → "CASH"
    // =========================================================================
    static class CashPayment implements PaymentBehavior {
        @Override
        public boolean processPayment(double amount) { return true; }

        @Override
        public String getPaymentMethod() { return "CASH"; }
    }

    // =========================================================================
    // ── TODO 13: Implement Order ──────────────────────────────────────────────
    //    Fields: orderId, itemName, basePrice, weightKg (all final)
    //    Composed: discountStrategy, shippingStrategy, paymentBehavior (mutable)
    //    Constructor: validate basePrice > 0 and weightKg > 0
    //    getFinalPrice()     → delegate to discount + shipping (NO math in Order)
    //    checkout()          → delegate to paymentBehavior
    //    getSummary()        → formatted one-liner
    //    upgradeShipping()   → runtime swap, validate not null
    //    applyNewDiscount()  → runtime swap, validate not null
    //    Getters
    // =========================================================================
    static class Order{
        private final String orderId;
        private final String itemName;
        private final double basePrice;
        private final double weightKg;
        private DiscountStrategy discountStrategy;
        private ShippingStrategy shippingStrategy;
        private PaymentBehavior paymentBehavior;

        public Order(String orderId,
                     String itemName,
                     double basePrice,
                     double weightKg,
                     DiscountStrategy discountStrategy,
                     ShippingStrategy shippingStrategy,
                     PaymentBehavior paymentBehavior){
            if(basePrice <= 0 || weightKg <=0){
                throw new IllegalArgumentException("Base Price and weight in KG should not be < 0");
            }
            this.orderId = orderId;
            this.itemName = itemName;
            this.basePrice = basePrice;
            this.weightKg = weightKg;
            this.discountStrategy = discountStrategy;
            this.shippingStrategy = shippingStrategy;
            this.paymentBehavior = paymentBehavior;
        }

        public String getSummary(){
            return String.format("[%s] %s | Base ₹%.0f | Discount: %s | Shipping: %s (%dd) | Payment: %s | Final : ₹%.2f",
                    orderId,
                    itemName,
                    basePrice,
                    discountStrategy.getDescription(),
                    shippingStrategy.getName(),
                    shippingStrategy.getEstimatedDays(),
                    paymentBehavior.getPaymentMethod(),
                    getFinalPrice());


        }


        public void applyNewDiscount(DiscountStrategy discountStrategy){
            this.discountStrategy = discountStrategy;
        }

        public void upgradeShipping(ShippingStrategy shippingStrategy){
            if(shippingStrategy != null)
            {
                this.shippingStrategy = shippingStrategy;
            }
        }

        public double getFinalPrice() {
            return discountStrategy.apply(basePrice) + shippingStrategy.calculateCost(weightKg);
        }

        public boolean checkout() {
            return paymentBehavior.processPayment(getFinalPrice());
        }

        public String getOrderId() { return orderId; }
        public String getItemName() { return itemName; }
        public double getBasePrice() { return basePrice; }
        public double getWeightKg() { return weightKg; }
        public DiscountStrategy getDiscountStrategy(){ return discountStrategy; }
        public ShippingStrategy getShippingStrategy() { return shippingStrategy; }
        public PaymentBehavior getPaymentBehavior() { return paymentBehavior; }

    }

    // =========================================================================
    // Main — DO NOT MODIFY — implement the TODOs above to make these pass
    // =========================================================================

    public static void main(String[] args) {

        // Test 1: Basic order creation and getFinalPrice
        // basePrice=50000, NoDiscount → 50000, Standard(2.5kg) → 50+25=75, total=50075
        Order order1 = new Order("O001", "Laptop", 50_000, 2.5,
                new NoDiscount(), new StandardShipping(), new UPIPayment("user@bank"));
        System.out.printf("Test 1: getFinalPrice = ₹%.2f (expected ₹50075.00)%n",
                order1.getFinalPrice());
        assert Math.abs(order1.getFinalPrice() - 50_075.0) < 0.01 : "Test 1 FAILED";
        System.out.println("Test 1 PASSED");

        // Test 2: PercentageDiscount + ExpressShipping
        // 10% off 50000 = 45000, Express(2.5kg) = 150+62.5 = 212.5, total = 45212.5
        Order order2 = new Order("O002", "Phone", 50_000, 2.5,
                new PercentageDiscount(10), new ExpressShipping(),
                new CreditCardPayment("1234"));
        System.out.printf("Test 2: getFinalPrice = ₹%.2f (expected ₹45212.50)%n",
                order2.getFinalPrice());
        assert Math.abs(order2.getFinalPrice() - 45_212.5) < 0.01 : "Test 2 FAILED";
        System.out.println("Test 2 PASSED");

        // Test 3: FlatDiscount — price floors at 0
        // FlatDiscount(60000) on basePrice 50000 → max(0, 50000-60000) = 0, + Free = 0
        Order order3 = new Order("O003", "Book", 50_000, 0.5,
                new FlatDiscount(60_000), new FreeShipping(), new CashPayment());
        System.out.printf("Test 3: FlatDiscount floors at 0 → ₹%.2f (expected ₹0.00)%n",
                order3.getFinalPrice());
        assert order3.getFinalPrice() == 0.0 : "Test 3 FAILED";
        System.out.println("Test 3 PASSED");

        // Test 4: checkout() delegates to paymentBehavior
        boolean paid = order1.checkout();
        System.out.println("Test 4: checkout() = " + paid + " (expected true)");
        assert paid : "Test 4 FAILED";
        System.out.println("Test 4 PASSED");

        // Test 5: upgradeShipping() — RUNTIME SWAP
        double priceBefore = order1.getFinalPrice();
        order1.upgradeShipping(new ExpressShipping());
        double priceAfter = order1.getFinalPrice();
        System.out.printf("Test 5: Before=₹%.2f After=₹%.2f (Express > Standard)%n",
                priceBefore, priceAfter);
        assert priceAfter > priceBefore : "Test 5 FAILED: express should cost more";
        System.out.println("Test 5 PASSED");

        // Test 6: applyNewDiscount() — RUNTIME SWAP on same order object
        double beforeDiscount = order1.getFinalPrice();
        order1.applyNewDiscount(new PercentageDiscount(20));
        double afterDiscount = order1.getFinalPrice();
        System.out.printf("Test 6: Before=₹%.2f After=₹%.2f (20%% discount applied)%n",
                beforeDiscount, afterDiscount);
        assert afterDiscount < beforeDiscount : "Test 6 FAILED: discount should reduce price";
        System.out.println("Test 6 PASSED");

        // Test 7: getSummary() output
        System.out.println("\n── Test 7: getSummary() ──────────────────────────────");
        System.out.println(order1.getSummary());
        System.out.println(order2.getSummary());
        System.out.println(order3.getSummary());

        // Test 8: PercentageDiscount validates percent range
        System.out.println("\n── Test 8: Validation ────────────────────────────────");
        try {
            new PercentageDiscount(0);
            System.out.println("Test 8a FAILED: 0% should be invalid");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 8a PASSED: " + e.getMessage());
        }
        try {
            new PercentageDiscount(101);
            System.out.println("Test 8b FAILED: 101% should be invalid");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 8b PASSED: " + e.getMessage());
        }

        // Test 9: CreditCardPayment validates 4-digit numeric code
        try {
            new CreditCardPayment("12AB");
            System.out.println("Test 9 FAILED: non-numeric digits should be invalid");
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
     *  - How many subclasses of Order do you need? Zero.
     *    Order is a regular class — it delegates all variability to its behavior fields.
     *
     *  - Where does the price calculation math live?
     *    NOT in Order. Order calls discount.apply() and shipping.calculateCost() and
     *    adds them. The formulas (50 + weight*10) live inside the shipping class.
     *
     *  - How do upgradeShipping() and applyNewDiscount() work?
     *    They simply replace the mutable behavior field:
     *    this.shippingStrategy = newStrategy;
     *
     *  - What does the runtime swap prove?
     *    That the same Order object can change behavior without creating a new object
     *    or subclass — impossible with pure inheritance.
     */

    /*
     * ── HINT LEVEL 2 (Direct) ─────────────────────────────────────────────────
     *
     *  interface DiscountStrategy {
     *      double apply(double price);
     *      String getDescription();
     *  }
     *
     *  interface ShippingStrategy {
     *      double calculateCost(double weightKg);
     *      int    getEstimatedDays();
     *      String getName();
     *  }
     *
     *  interface PaymentBehavior {
     *      boolean processPayment(double amount);
     *      String  getPaymentMethod();
     *  }
     *
     *  Order.getFinalPrice():
     *      return discountStrategy.apply(basePrice)
     *           + shippingStrategy.calculateCost(weightKg);
     *
     *  FlatDiscount.apply():
     *      return Math.max(0, price - amount);   // never goes negative!
     *
     *  CreditCardPayment validation — lastFourDigits must be exactly 4 chars, all digits:
     *      lastFourDigits.length() == 4 && lastFourDigits.matches("\\d{4}")
     */

    /*
     * ── HINT LEVEL 3 (Near-Solution) ──────────────────────────────────────────
     *
     *  static class Order {
     *      private final String orderId, itemName;
     *      private final double basePrice, weightKg;
     *      private DiscountStrategy  discountStrategy;   // mutable — can be swapped
     *      private ShippingStrategy  shippingStrategy;   // mutable
     *      private PaymentBehavior   paymentBehavior;    // mutable
     *
     *      public Order(String orderId, String itemName, double basePrice, double weightKg,
     *                   DiscountStrategy d, ShippingStrategy s, PaymentBehavior p) {
     *          if (basePrice <= 0) throw new IllegalArgumentException("basePrice must be > 0");
     *          if (weightKg  <= 0) throw new IllegalArgumentException("weightKg must be > 0");
     *          this.orderId   = orderId;  this.itemName = itemName;
     *          this.basePrice = basePrice; this.weightKg = weightKg;
     *          this.discountStrategy = d;
     *          this.shippingStrategy = s;
     *          this.paymentBehavior  = p;
     *      }
     *
     *      public double  getFinalPrice() {
     *          return discountStrategy.apply(basePrice) + shippingStrategy.calculateCost(weightKg);
     *      }
     *      public boolean checkout() { return paymentBehavior.processPayment(getFinalPrice()); }
     *
     *      public void upgradeShipping(ShippingStrategy s) {
     *          if (s == null) throw new IllegalArgumentException("strategy cannot be null");
     *          this.shippingStrategy = s;
     *      }
     *      // applyNewDiscount() follows the same pattern
     *  }
     */
}

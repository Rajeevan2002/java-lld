# Composition over Inheritance

> "Favor object composition over class inheritance."
> — Gang of Four, Design Patterns (1994)
> This is not a rule against inheritance. It's a reminder to reach for composition first.

---

## 1. The Two Problems Inheritance Creates

### Problem 1: Class Explosion

Suppose you have a coffee drink with 3 sizes, 4 milk options, and 5 sweeteners.
Using inheritance:

```
CoffeeDrink
├── SmallCoffeeDrink
│   ├── SmallCoffeeDrinkWithOatMilk
│   │   ├── SmallCoffeeDrinkWithOatMilkAndSugar
│   │   ├── SmallCoffeeDrinkWithOatMilkAndHoney
│   │   └── ...  (5 variants)
│   └── ...  (4 milk × 5 sweetener = 20 subclasses for Small alone)
├── MediumCoffeeDrink  (another 20)
└── LargeCoffeeDrink   (another 20)

Total: 3 × 4 × 5 = 60 classes for what should be 3 + 4 + 5 = 12 concepts
```

Using composition:

```java
new CoffeeOrder(Size.LARGE, new OatMilk(), new HoneySweetener());
// One class. Infinite combinations.
```

---

### Problem 2: The Fragile Base Class

Changes to a superclass can silently break subclasses — even if the subclass didn't override
the changed method.

```java
class Base {
    public void doA() { doB(); }      // calls doB internally
    public void doB() { count++; }
}

class Child extends Base {
    @Override
    public void doA() { super.doA(); }   // fine
    @Override
    public void doB() { super.doB(); }   // fine
}

// Now Base changes doA() to call doB() twice:
class Base {
    public void doA() { doB(); doB(); } // ← breaking change
}
// Child now double-counts without changing a single line of Child code
```

With composition, `Child` holds a reference to `Base` and calls only the methods it chooses.
A change to `Base.doA()` doesn't affect `Child` unless `Child` calls `doA()`.

---

## 2. Has-A vs Is-A

| Relationship | Keyword | Use when |
|---|---|---|
| **Is-A** | `extends` | The subclass truly is a specialisation of the parent |
| **Has-A** | field reference | The class uses the behaviour of another object |

**Test:** Substitute the subclass everywhere the parent is used — does it still make sense?
- `Dog` is-an `Animal` ✅
- `Square` is-a `Rectangle` — fails the Liskov test (Phase 2) ❌ → use composition

**Composition signal phrases:**
- "has a payment method" → field `PaymentBehavior payment`
- "has a shipping strategy" → field `ShippingStrategy shipping`
- "can send alerts" → field `AlertBehavior alert`

---

## 3. Composition in Code

```
                INHERITANCE                    COMPOSITION
         ┌──────────────────┐          ┌──────────────────────┐
         │   SmartDevice    │          │     SmartDevice      │
         │   + connect()    │          │  - connectivity ─────────► «interface»
         │   + sendAlert()  │          │  - alertBehavior ────────► ConnectivityBehavior
         └────────┬─────────┘          │                      │    + connect()
                  │                    │  + connect() {        │    + disconnect()
         ┌────────┴─────────┐          │    connectivity       │
         │                  │          │      .connect(); }    │   «interface»
    WiFiLight         BtLight          └──────────────────────┘   AlertBehavior
    EmailAlert        PushAlert                                    + sendAlert()
    BtLight +
    NoAlert...
    ← class explosion
```

```java
// COMPOSITION — behavior as a field, injected at construction
class SmartDevice {
    private ConnectivityBehavior connectivity;  // HAS-A
    private AlertBehavior        alertBehavior; // HAS-A

    public SmartDevice(ConnectivityBehavior c, AlertBehavior a) {
        this.connectivity  = c;
        this.alertBehavior = a;
    }

    // Delegation — SmartDevice doesn't know HOW to connect; it delegates
    public void connect() { connectivity.connect(); }
    public void alert(String msg) { alertBehavior.sendAlert(msg); }

    // RUNTIME SWAP — change behavior without changing the class
    public void setAlertBehavior(AlertBehavior a) { this.alertBehavior = a; }
}
```

---

## 4. Delegation

**Delegation** is the mechanism behind composition. The owning class calls a method
on its composed object rather than implementing the logic itself.

```java
// Without delegation (logic inside the class — brittle)
class Order {
    public double calculateShipping(double weight) {
        return 50 + weight * 10;   // hardcoded logic — changes require editing Order
    }
}

// With delegation (logic in composed object — flexible)
class Order {
    private ShippingStrategy shipping;   // HAS-A

    public double calculateShipping(double weight) {
        return shipping.calculateCost(weight);  // delegate — Order doesn't care HOW
    }

    public void upgradeShipping(ShippingStrategy s) {
        this.shipping = s;   // runtime swap — no subclass needed
    }
}
```

---

## 5. Runtime Behavior Swapping — The Killer Feature

Inheritance fixes behavior at compile time (subclass hierarchy is static).
Composition changes behavior at runtime by swapping the composed object.

```java
Order order = new Order("O001", "Laptop", 50_000, 2.5,
        new NoDiscount(),
        new StandardShipping(),
        new UPIPayment("user@bank"));

System.out.println(order.getFinalPrice());  // base + standard shipping

// Customer upgrades shipping — no new class, no subclass
order.upgradeShipping(new ExpressShipping());
System.out.println(order.getFinalPrice());  // same order, new shipping cost

// Flash sale — swap discount
order.applyDiscount(new PercentageDiscount(20));
System.out.println(order.getFinalPrice());  // now with 20% off + express
```

**With inheritance:** You'd need to instantiate a different subclass. The old object is discarded.

---

## 6. The Strategy Pattern (Composition in Action)

Composition over inheritance is the principle. **Strategy** is the most direct pattern that embodies it.

```
   «interface»          «interface»          «interface»
  DiscountStrategy     ShippingStrategy    PaymentBehavior
  + apply(price)       + cost(weight)      + process(amount)
  + getDescription()   + getDays()         + getMethod()
        ▲                    ▲                    ▲
   ┌────┴────┐         ┌─────┴─────┐        ┌────┴────┐
   │         │         │     │     │        │    │    │
  None  Percentage    Std  Expr  Free      Card  UPI  Cash
  Flat
                             │
                         ┌───┴────────────────────┐
                         │          Order          │
                         │  - discountStrategy     │
                         │  - shippingStrategy     │
                         │  - paymentBehavior      │
                         │  + getFinalPrice()      │
                         │  + checkout()           │
                         │  + upgradeShipping()  ← runtime swap
                         │  + applyDiscount()    ← runtime swap
                         └─────────────────────────┘
```

---

## 7. When Inheritance IS the Right Choice

Composition is not a blanket replacement for inheritance. Use inheritance when:

| Condition | Example |
|-----------|---------|
| True IS-A relationship that won't fragment | `Dog extends Animal` |
| Want to share state AND behavior that is genuinely common | `abstract Payment` with shared transactionId |
| Liskov Substitution holds completely | Subclass can replace parent everywhere |
| The hierarchy is shallow (≤ 2 levels) and stable | No reason it will grow |

**Red flags that inheritance is wrong:**
- You're overriding methods just to throw `UnsupportedOperationException`
- The number of subclasses grows with every new combination
- Subclasses differ only in one small behavior that could be a field
- You need to add behavior to an existing class you can't modify

---

## 8. Interview Questions

**Q1: What is "composition over inheritance" and why is it recommended?**
> Composition means building a class by holding references to other objects (HAS-A) rather than extending them (IS-A). It's recommended because: (1) it avoids class explosion when behaviors combine multiplicatively, (2) it avoids the fragile base class problem, (3) behaviors can be swapped at runtime, and (4) each behavior class has a single responsibility.

**Q2: Can you give an example where inheritance causes a class explosion?**
> A vehicle with 3 engine types, 4 drive types, and 2 fuel types requires 3×4×2=24 subclasses. With composition, you inject `EngineType`, `DriveType`, and `FuelType` as fields — 3 interfaces + 9 implementation classes instead of 24 subclasses.

**Q3: What is the fragile base class problem?**
> When a superclass method is changed, subclasses that override or call it can break silently — even if the subclass itself wasn't changed. Composition avoids this because the composed object's changes only affect the owning class at the specific delegation points.

**Q4: What is delegation?**
> The owning class calls a method on a composed object rather than implementing the logic itself. The owning class doesn't know *how* the behavior works, just that it gets a result. This keeps each class focused on one responsibility.

**Q5: How does composition enable the Open/Closed Principle?**
> Because behavior is injected through interfaces, you can add new behavior (e.g., `CryptoPayment`) without modifying the `Order` class. `Order` is closed for modification but open for extension via new `PaymentBehavior` implementations.

**Q6: What is the Strategy Pattern?**
> A design pattern that defines a family of algorithms, encapsulates each one as an object, and makes them interchangeable. The class that uses the strategy holds a reference to the strategy interface, delegates to it, and can swap it at runtime. It's the most direct application of composition over inheritance.

**Q7: Is composition always better than inheritance?**
> No. Use inheritance for genuine IS-A relationships with stable, shallow hierarchies. Use composition when behaviors combine multiplicatively, when you need runtime flexibility, or when the base class is fragile. Most real systems use both: abstract classes with composed behavior fields.

---

## 9. Common Mistakes

```java
// MISTAKE 1: Using inheritance when only one method differs
class StandardOrder extends Order {
    @Override public double calculateShipping(double w) { return 50 + w * 10; }
}
class ExpressOrder extends Order {
    @Override public double calculateShipping(double w) { return 150 + w * 25; }
}
// ← The only difference is one method. Use a ShippingStrategy field instead.

// MISTAKE 2: Exposing the composed object directly (Law of Demeter violation)
public ShippingStrategy getShipping() { return shipping; }
// ← Callers then call order.getShipping().calculateCost(2.5)
// ← Better: order.calculateShipping(2.5) — Order delegates internally

// MISTAKE 3: Creating a composed object inside the constructor (tight coupling)
public Order(...) {
    this.shipping = new StandardShipping();  // ← hardcoded, can't swap, can't test
}
// Fix: inject via constructor or setter
public Order(..., ShippingStrategy shipping) {
    this.shipping = shipping;
}

// MISTAKE 4: Making behavior classes stateful when they should be stateless
class PercentageDiscount implements DiscountStrategy {
    private double lastAppliedPrice;  // ← why? This adds state to a behavior
    public double apply(double price) {
        lastAppliedPrice = price;  // pointless state
        return price * 0.8;
    }
}
// Behavior classes should be pure: input → output, no side effects

// MISTAKE 5: "Composition" that's really just aggregation with no delegation
class Order {
    public ShippingStrategy shipping;  // ← public field, no encapsulation
}
// The caller does: order.shipping.calculateCost(w)  ← delegation bypassed
// Fix: make it private, add Order.calculateShipping() that delegates
```

package com.ramkumar.lld.designpatterns.behavioral.observer.code;

import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// Observer Pattern — Scenario A: Stock Ticker
//
// Problem: A stock market broadcasts price changes to multiple consumers:
//   - PriceAlertObserver — fires when a stock crosses a threshold
//   - PortfolioTracker   — maintains running profit/loss
//   - AuditLogger        — records every price change for compliance
//
//   Without Observer: StockMarket would hold references to each concrete
//   consumer and call them directly. Every new consumer requires editing
//   StockMarket — violating OCP.
//
// Solution: Define a StockObserver interface. StockMarket holds a
//   List<StockObserver> and notifies all of them through the interface.
//   Consumers register/unregister at runtime; StockMarket never changes.
//
// Participants:
//   StockObserver      [Observer interface]      — the notification contract
//   PriceAlertObserver [ConcreteObserver 1]      — threshold-based alert
//   PortfolioTracker   [ConcreteObserver 2]      — P&L tracking
//   AuditLogger        [ConcreteObserver 3]      — compliance log
//   StockMarket        [Subject]                 — broadcasts price changes
// ─────────────────────────────────────────────────────────────────────────────

// ── [Observer interface] — the single notification contract ───────────────────
interface StockObserver {
    // [PushModel] Subject pushes ticker and price — observer gets exactly what it needs.
    void onPriceChange(String ticker, double newPrice);
}

// ── [ConcreteObserver 1] — alert when price crosses a threshold ───────────────
class PriceAlertObserver implements StockObserver {

    private final String ticker;
    // [ObserverState] Each observer carries its own configuration — the subject has no
    // knowledge of thresholds; it just calls onPriceChange.
    private final double alertPrice;

    PriceAlertObserver(String ticker, double alertPrice) {
        this.ticker     = ticker;
        this.alertPrice = alertPrice;
    }

    @Override
    public void onPriceChange(String ticker, double newPrice) {
        if (this.ticker.equals(ticker) && newPrice >= alertPrice) {
            System.out.printf("[ALERT] %s hit $%.2f (threshold $%.2f)%n",
                ticker, newPrice, alertPrice);
        }
    }
}

// ── [ConcreteObserver 2] — running P&L tracker ───────────────────────────────
class PortfolioTracker implements StockObserver {

    private final String ticker;
    private final int    shares;
    private final double costBasis;   // price per share paid at purchase
    // [MutableObserverState] lastPrice is updated each notification — observer has its
    // own internal state that evolves with each push.
    private double lastPrice;

    PortfolioTracker(String ticker, int shares, double costBasis) {
        this.ticker    = ticker;
        this.shares    = shares;
        this.costBasis = costBasis;
        this.lastPrice = costBasis;
    }

    @Override
    public void onPriceChange(String ticker, double newPrice) {
        if (!this.ticker.equals(ticker)) return;   // only track our own stock
        this.lastPrice = newPrice;
        double pnl = (newPrice - costBasis) * shares;
        System.out.printf("[Portfolio] %s: $%.2f/share × %d = P&L $%+.2f%n",
            ticker, newPrice, shares, pnl);
    }
}

// ── [ConcreteObserver 3] — compliance audit log ───────────────────────────────
class AuditLogger implements StockObserver {

    // [StatelessObserver] AuditLogger carries no observer-specific config — it logs
    // every single price change for every ticker. No filtering needed.
    @Override
    public void onPriceChange(String ticker, double newPrice) {
        System.out.printf("[Audit] %s → $%.2f%n", ticker, newPrice);
    }
}

// ── [Subject] ─────────────────────────────────────────────────────────────────
class StockMarket {

    // [InterfaceList] CRITICAL: typed as List<StockObserver> — never a concrete type.
    // This is what enables adding any new observer without touching StockMarket.
    private final List<StockObserver> observers = new ArrayList<>();

    // [Validate] Reject null at the boundary — fail fast rather than NPE at notification.
    void subscribe(StockObserver observer) {
        if (observer == null) throw new IllegalArgumentException("observer must not be null");
        observers.add(observer);
    }

    // [NoOp] remove() returns false if not present — safe to call with unknown refs.
    void unsubscribe(StockObserver observer) {
        observers.remove(observer);
    }

    // [Notification] Subject stores the latest price and broadcasts to all observers.
    void publishPrice(String ticker, double newPrice) {
        System.out.printf("%n── StockMarket: %s = $%.2f ──%n", ticker, newPrice);
        // [SnapshotCopy] Iterate a copy — an observer may call unsubscribe() during
        // notification; modifying the live list mid-loop causes ConcurrentModificationException.
        List<StockObserver> snapshot = new ArrayList<>(observers);
        for (StockObserver o : snapshot) {
            o.onPriceChange(ticker, newPrice);   // [Delegation] no instanceof anywhere
        }
    }

    int observerCount() { return observers.size(); }
}

// ── Demo ──────────────────────────────────────────────────────────────────────
public class StockTickerDemo {

    public static void main(String[] args) {

        StockMarket market = new StockMarket();

        // ── 1. Register three observers ─────────────────────────────────────
        System.out.println("=== Registering observers ===");
        PriceAlertObserver alert     = new PriceAlertObserver("AAPL", 180.00);
        PortfolioTracker   portfolio = new PortfolioTracker("AAPL", 50, 160.00);
        AuditLogger        audit     = new AuditLogger();

        market.subscribe(alert);
        market.subscribe(portfolio);
        market.subscribe(audit);
        System.out.println("Observers registered: " + market.observerCount());

        // ── 2. Publish prices — all three observers notified ────────────────
        System.out.println("\n=== Price updates ===");
        market.publishPrice("AAPL", 165.50);   // below alert threshold
        market.publishPrice("AAPL", 182.00);   // crosses alert threshold
        market.publishPrice("GOOGL", 140.00);  // different ticker — portfolio ignores

        // ── 3. Unsubscribe audit logger — only alert and portfolio notified ──
        System.out.println("\n=== After unsubscribing AuditLogger ===");
        market.unsubscribe(audit);
        System.out.println("Observers after unsubscribe: " + market.observerCount());
        market.publishPrice("AAPL", 175.00);

        // ── 4. Unsubscribe inside notification (safe because of snapshot copy) ─
        System.out.println("\n=== Self-unsubscribing observer ===");
        StockObserver oneShot = new StockObserver() {
            @Override
            public void onPriceChange(String ticker, double newPrice) {
                System.out.printf("[OneShot] triggered by %s $%.2f — unsubscribing%n",
                    ticker, newPrice);
                market.unsubscribe(this);   // [SafeUnsubscribe] snapshot copy prevents CME
            }
        };
        market.subscribe(oneShot);
        System.out.println("Observers before trigger: " + market.observerCount());
        market.publishPrice("AAPL", 178.00);
        System.out.println("Observers after trigger: " + market.observerCount());

        // ── 5. Null guard ────────────────────────────────────────────────────
        System.out.println("\n=== Null guard ===");
        try {
            market.subscribe(null);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}

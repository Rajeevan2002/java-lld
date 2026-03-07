package com.ramkumar.lld.designpatterns.behavioral.observer.results;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference solution — Observer Pattern: Weather Station
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>Snapshot copy in {@code recordReading()} is mandatory — iterating the live list
 *       throws {@code ConcurrentModificationException} when an observer unsubscribes mid-loop.
 *       Test 8 and Test 9 both verify this.</li>
 *   <li>{@code observers} is typed as {@code List<WeatherObserver>} (interface), not
 *       {@code ArrayList} — the backing collection is an implementation detail.</li>
 *   <li>Null guard in {@code subscribe()} fails fast at the boundary, not as a delayed
 *       NPE inside the notification loop.</li>
 *   <li>Inner-class members are package-private by default — no {@code public} needed.</li>
 *   <li>{@code double} fields default to {@code 0.0} — no explicit initialisation needed.</li>
 * </ul>
 */
public class ObserverReference {

    // ── [Observer interface] — the notification contract ─────────────────────
    interface WeatherObserver {
        // [PushModel] Subject pushes all three values — observer receives exactly what it needs.
        void onReadingUpdate(double tempC, double humidity, double pressureHpa);
    }

    // ── [ConcreteObserver 1] ──────────────────────────────────────────────────
    static class PhoneDisplay implements WeatherObserver {

        // [ObserverState] Config lives inside the observer; subject has zero knowledge of it.
        private final String owner;

        PhoneDisplay(String owner) {
            this.owner = owner;
        }

        @Override
        public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
            // pressureHpa is received but not displayed — that's fine; ignore unused parameters.
            System.out.printf("[Phone:%s] %.1f°C  %.0f%% humidity%n", owner, tempC, humidity);
        }
    }

    // ── [ConcreteObserver 2] ──────────────────────────────────────────────────
    static class WebDashboard implements WeatherObserver {

        // [StatelessObserver] No fields needed — all data arrives via method parameters.
        // Explicit no-arg constructor not required; the implicit one is identical.

        @Override
        public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
            System.out.printf("[WebDashboard] %.1f°C | %.0f%% RH | %.1f hPa%n",
                tempC, humidity, pressureHpa);
        }
    }

    // ── [ConcreteObserver 3] ──────────────────────────────────────────────────
    static class AlertService implements WeatherObserver {

        private final double heatThresholdC;

        AlertService(double heatThresholdC) {
            this.heatThresholdC = heatThresholdC;
        }

        @Override
        public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
            // [InclusiveThreshold] >= fires at exactly the threshold, not just above it.
            if (tempC >= heatThresholdC) {
                System.out.printf("[ALERT] Heat warning: %.1f°C exceeds threshold %.1f°C%n",
                    tempC, heatThresholdC);
            }
            // No else branch — silent below threshold is the correct behaviour.
        }
    }

    // ── [Subject] ─────────────────────────────────────────────────────────────
    static class WeatherStation {

        // [InterfaceType] List<WeatherObserver>, NOT ArrayList<WeatherObserver>.
        // The interface is the contract; ArrayList is the implementation detail.
        private final List<WeatherObserver> observers;
        // [DefaultZero] double fields default to 0.0 — no explicit initialisation needed.
        private double tempC;
        private double humidity;
        private double pressureHpa;

        WeatherStation() {
            // [InitList] ArrayList is the implementation choice — hidden behind the List type.
            observers = new ArrayList<>();
        }

        // [FailFast] Validate null at subscribe() — a clear IAE here beats a silent NPE
        // inside the notification loop where the root cause is impossible to trace.
        void subscribe(WeatherObserver o) {
            if (o == null) throw new IllegalArgumentException("observer must not be null");
            observers.add(o);
        }

        // [NoOp] List.remove() returns false silently when element is absent — no extra guard.
        void unsubscribe(WeatherObserver o) {
            observers.remove(o);
        }

        void recordReading(double tempC, double humidity, double pressureHpa) {
            this.tempC       = tempC;
            this.humidity    = humidity;
            this.pressureHpa = pressureHpa;
            // [SnapshotCopy] CRITICAL: copy before iterating.
            // If an observer calls unsubscribe() during onReadingUpdate(), the live list
            // would throw ConcurrentModificationException. The snapshot is not affected.
            List<WeatherObserver> snapshot = new ArrayList<>(observers);
            for (WeatherObserver o : snapshot) {
                o.onReadingUpdate(tempC, humidity, pressureHpa);  // [NoCasting] pure interface call
            }
        }

        int observerCount() {
            return observers.size();
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: PhoneDisplay print format ────────────────────────────────
        PhoneDisplay phone = new PhoneDisplay("Alice");
        phone.onReadingUpdate(28.5, 65.0, 1013.2);
        // expected: [Phone:Alice] 28.5°C  65% humidity

        // ── Test 2: WebDashboard print format ────────────────────────────────
        WebDashboard web = new WebDashboard();
        web.onReadingUpdate(28.5, 65.0, 1013.2);
        // expected: [WebDashboard] 28.5°C | 65% RH | 1013.2 hPa

        // ── Test 3: AlertService — below threshold, no output ─────────────────
        AlertService alert = new AlertService(35.0);
        alert.onReadingUpdate(28.5, 65.0, 1013.2);   // 28.5 < 35.0 → no output

        // ── Test 4: AlertService — at threshold fires alert ───────────────────
        alert.onReadingUpdate(35.0, 70.0, 1010.0);
        // expected: [ALERT] Heat warning: 35.0°C exceeds threshold 35.0°C

        // ── Test 5: WeatherStation notifies all subscribers ───────────────────
        WeatherStation station = new WeatherStation();
        PhoneDisplay p1 = new PhoneDisplay("Bob");
        WebDashboard w1 = new WebDashboard();
        AlertService a1 = new AlertService(35.0);
        station.subscribe(p1);
        station.subscribe(w1);
        station.subscribe(a1);
        System.out.println("Observer count: " + station.observerCount());  // expected: 3
        station.recordReading(22.0, 55.0, 1015.0);
        // expected: [Phone:Bob] 22.0°C  55% humidity
        //           [WebDashboard] 22.0°C | 55% RH | 1015.0 hPa

        // ── Test 6: Unsubscribe and re-notify ────────────────────────────────
        station.unsubscribe(w1);
        System.out.println("After unsubscribe: " + station.observerCount());  // expected: 2
        station.recordReading(36.0, 80.0, 1008.0);
        // expected: [Phone:Bob] 36.0°C  80% humidity
        //           [ALERT] Heat warning: 36.0°C exceeds threshold 35.0°C

        // ── Test 7: Null subscribe throws IAE ────────────────────────────────
        try {
            station.subscribe(null);
            System.out.println("Test 7 — FAILED (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 7 — null guard: "
                + ("observer must not be null".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
        }

        // ── Test 8: Self-unsubscribing observer during notification ───────────
        WeatherStation station2 = new WeatherStation();
        WeatherObserver oneShot = new WeatherObserver() {
            @Override
            public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
                System.out.printf("[OneShot] %.1f°C — unsubscribing%n", tempC);
                station2.unsubscribe(this);
            }
        };
        station2.subscribe(new PhoneDisplay("Carol"));
        station2.subscribe(oneShot);
        station2.subscribe(new PhoneDisplay("Dave"));
        System.out.println("Before trigger: " + station2.observerCount());  // expected: 3
        station2.recordReading(25.0, 60.0, 1012.0);
        System.out.println("After trigger: " + station2.observerCount());   // expected: 2
        System.out.println("Test 8 — self-unsubscribe no CME: PASSED");

        // ── Test 9 (catches the most common mistake: iterating live list) ─────
        // A student who forgets the snapshot copy and iterates `observers` directly
        // will pass Tests 1–7 but fail here when TWO observers unsubscribe mid-loop.
        // With two removals during iteration, ArrayList throws CME on the second pass.
        System.out.println("\n── Test 9: Two self-unsubscribing observers (catches missing snapshot) ──");
        WeatherStation station3 = new WeatherStation();
        // Create two observers that both unsubscribe themselves on first notification
        WeatherObserver selfRemover1 = new WeatherObserver() {
            @Override
            public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
                System.out.println("[SelfRemover1] notified, unsubscribing");
                station3.unsubscribe(this);
            }
        };
        WeatherObserver selfRemover2 = new WeatherObserver() {
            @Override
            public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
                System.out.println("[SelfRemover2] notified, unsubscribing");
                station3.unsubscribe(this);
            }
        };
        station3.subscribe(selfRemover1);
        station3.subscribe(new PhoneDisplay("Eve"));
        station3.subscribe(selfRemover2);
        System.out.println("Before: " + station3.observerCount());  // expected: 3
        try {
            station3.recordReading(20.0, 50.0, 1013.0);
            System.out.println("After: " + station3.observerCount());   // expected: 1
            System.out.println("Test 9 — two self-removers, no CME: PASSED");
        } catch (java.util.ConcurrentModificationException e) {
            // This catches the missing-snapshot mistake
            System.out.println("Test 9 — FAILED: ConcurrentModificationException (missing snapshot copy)");
        }
    }
}

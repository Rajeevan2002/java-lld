package com.ramkumar.lld.designpatterns.behavioral.observer.practice;

import java.util.ArrayList;
import java.util.List;

/**
 * Practice Exercise — Observer Pattern: Weather Station
 *
 * <p><b>Scenario B — One-to-many notification via interface list</b>
 *
 * <p>A weather station collects sensor readings and broadcasts them to multiple
 * display and alert systems. Any number of observers can subscribe or unsubscribe
 * at runtime; the station must never reference concrete observer types directly.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   WeatherObserver   [Observer interface]    ← TODO 1
 *   PhoneDisplay      [ConcreteObserver 1]    ← TODO 2
 *   WebDashboard      [ConcreteObserver 2]    ← TODO 3
 *   AlertService      [ConcreteObserver 3]    ← TODO 4
 *   WeatherStation    [Subject]               ← TODOs 5–8
 * </pre>
 *
 * <p><b>WeatherObserver (interface)</b> (TODO 1):
 * <ul>
 *   <li>{@code onReadingUpdate(double tempC, double humidity, double pressureHpa)}
 *       — called by the station on every new reading; no return value.</li>
 * </ul>
 *
 * <p><b>PhoneDisplay (ConcreteObserver 1)</b> (TODO 2):
 * <ul>
 *   <li>Field: {@code private final String owner} — set in constructor; immutable.</li>
 *   <li>Constructor: {@code PhoneDisplay(String owner)}.</li>
 *   <li>{@code onReadingUpdate}: prints
 *       {@code System.out.printf("[Phone:%s] %.1f°C  %.0f%% humidity%n", owner, tempC, humidity)}</li>
 * </ul>
 *
 * <p><b>WebDashboard (ConcreteObserver 2)</b> (TODO 3):
 * <ul>
 *   <li>No fields beyond what {@code onReadingUpdate} needs.</li>
 *   <li>Constructor: {@code WebDashboard()}.</li>
 *   <li>{@code onReadingUpdate}: prints
 *       {@code System.out.printf("[WebDashboard] %.1f°C | %.0f%% RH | %.1f hPa%n",
 *       tempC, humidity, pressureHpa)}</li>
 * </ul>
 *
 * <p><b>AlertService (ConcreteObserver 3)</b> (TODO 4):
 * <ul>
 *   <li>Field: {@code private final double heatThresholdC} — set in constructor; immutable.</li>
 *   <li>Constructor: {@code AlertService(double heatThresholdC)}.</li>
 *   <li>{@code onReadingUpdate}: if {@code tempC >= heatThresholdC} → print
 *       {@code System.out.printf("[ALERT] Heat warning: %.1f°C exceeds threshold %.1f°C%n",
 *       tempC, heatThresholdC)};
 *       otherwise print nothing.</li>
 * </ul>
 *
 * <p><b>WeatherStation (Subject)</b> (TODOs 5–8):
 * <ul>
 *   <li>Fields: {@code private final List<WeatherObserver> observers} — initialised to
 *       a new {@code ArrayList} in the constructor, never replaced;
 *       {@code private double tempC}, {@code private double humidity},
 *       {@code private double pressureHpa} — last recorded values, default {@code 0.0}.</li>
 *   <li>Constructor: {@code WeatherStation()} — initialises the observer list.</li>
 *   <li>{@code subscribe(WeatherObserver o)}: adds {@code o} to the list.
 *       Validate: {@code o == null} →
 *       throw {@code new IllegalArgumentException("observer must not be null")}.</li>
 *   <li>{@code unsubscribe(WeatherObserver o)}: removes {@code o} from the list;
 *       no-op if not present.</li>
 *   <li>{@code recordReading(double tempC, double humidity, double pressureHpa)}:
 *       stores the three values as fields, then notifies all current observers.
 *       MUST iterate over a <b>snapshot copy</b> of the observer list
 *       ({@code new ArrayList<>(observers)}) so that an observer unsubscribing
 *       during notification does not cause {@code ConcurrentModificationException}.</li>
 *   <li>{@code observerCount() → int}: returns {@code observers.size()}.</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code WeatherStation} must reference only {@code WeatherObserver} (interface)
 *       — never {@code PhoneDisplay}, {@code WebDashboard}, or {@code AlertService}.</li>
 *   <li>No {@code instanceof} or type-checking anywhere in {@code WeatherStation}.</li>
 *   <li>The snapshot-copy iteration rule is mandatory — using the live list is a design defect.</li>
 *   <li>The observer list must NOT be exposed directly via a getter.</li>
 * </ul>
 */
public class WeatherStationPractice {

    // ── Observer interface ─────────────────────────────────────────────────────

    interface WeatherObserver {
        void onReadingUpdate(double tempC, double humidity, double pressureHpa);
    }

    // ── ConcreteObserver 1 ─────────────────────────────────────────────────────

    static class PhoneDisplay implements WeatherObserver {
        private final String owner;
        public PhoneDisplay(String owner){
            this.owner = owner;
        }

        @Override
        public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
            System.out.printf("[Phone:%s] %.1f°C  %.0f%% humidity%n", owner, tempC, humidity);
        }
    }

    // ── ConcreteObserver 2 ─────────────────────────────────────────────────────


    static class WebDashboard implements WeatherObserver {
        @Override
        public void onReadingUpdate(double tempC, double humidity, double pressureHpa){
            System.out.printf("[WebDashboard] %.1f°C | %.0f%% RH | %.1f hPa%n", tempC, humidity, pressureHpa);
        }
    }

    // ── ConcreteObserver 3 ─────────────────────────────────────────────────────

    // ── TODO 4: Implement AlertService implements WeatherObserver
    //    Private final field: heatThresholdC (double) — set in constructor
    //    Constructor: AlertService(double heatThresholdC)
    //    onReadingUpdate(tempC, humidity, pressureHpa):
    //      If tempC >= heatThresholdC:
    //        Print: System.out.printf("[ALERT] Heat warning: %.1f°C exceeds threshold %.1f°C%n",
    //               tempC, heatThresholdC)
    //      Otherwise: print nothing

    static class AlertService implements WeatherObserver {
        private final double heatThresholdC;
        public AlertService(double heatThresholdC){
            this.heatThresholdC = heatThresholdC;
        }

        @Override
        public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
            if(tempC >=  heatThresholdC){
                System.out.printf("[ALERT] Heat warning: %.1f°C exceeds threshold %.1f°C%n", tempC, heatThresholdC);
            }
        }
    }

    // ── Subject ────────────────────────────────────────────────────────────────

    // ── TODO 5: Declare 4 fields in WeatherStation
    //    private final List<WeatherObserver> observers  — initialised in constructor; never replaced
    //    private double tempC                           — last recorded temperature; default 0.0
    //    private double humidity                        — last recorded humidity; default 0.0
    //    private double pressureHpa                     — last recorded pressure; default 0.0
    //    NOTE: the List must be typed as List<WeatherObserver> (interface), not ArrayList

    // ── TODO 6: Constructor WeatherStation()
    //    Initialise observers to new ArrayList<>()
    //    The three double fields default to 0.0 automatically — no explicit assignment needed

    // ── TODO 7: subscribe(WeatherObserver o) and unsubscribe(WeatherObserver o)
    //    subscribe:
    //      Validate: o == null → throw new IllegalArgumentException("observer must not be null")
    //      Add o to observers
    //    unsubscribe:
    //      Remove o from observers (no-op if not present; List.remove() handles this safely)

    // ── TODO 8: recordReading(double tempC, double humidity, double pressureHpa) and observerCount()
    //    recordReading:
    //      Store the three values into the corresponding fields (this.tempC, etc.)
    //      Take a snapshot: List<WeatherObserver> snapshot = new ArrayList<>(observers)
    //      Iterate snapshot (NOT observers) and call onReadingUpdate(tempC, humidity, pressureHpa)
    //      on each element — this prevents ConcurrentModificationException if an observer
    //      calls unsubscribe() during notification
    //    observerCount:
    //      Return observers.size()

    static class WeatherStation {
        private final List<WeatherObserver> observers;
        private double tempC;
        private double humidity;
        private double pressureHpa;

        public WeatherStation(){
            this.observers = new ArrayList<>();
            this.tempC = 0.0;
            this.humidity = 0.0;
            this.pressureHpa = 0.0;
        }

        public void recordReading(double tempC, double humidity, double pressureHpa){
            this.tempC = tempC;
            this.humidity = humidity;
            this.pressureHpa = pressureHpa;
            List<WeatherObserver> snapshot = new ArrayList<>(observers);
            for(WeatherObserver o : snapshot){
                o.onReadingUpdate(tempC, humidity, pressureHpa);
            }
        }

        public void subscribe(WeatherObserver o){
            if(o == null){
                throw new IllegalArgumentException("observer must not be null");
            }
            observers.add(o);
        }

        public void unsubscribe(WeatherObserver o){
            observers.remove(o);
        }

        public int observerCount() { return observers.size(); }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: PhoneDisplay prints correct format (uncomment after TODO 2) ──────────
         PhoneDisplay phone = new PhoneDisplay("Alice");
         phone.onReadingUpdate(28.5, 65.0, 1013.2);
         // expected: [Phone:Alice] 28.5°C  65% humidity

        // ── Test 2: WebDashboard prints all three readings (uncomment after TODO 3) ──────
         WebDashboard web = new WebDashboard();
         web.onReadingUpdate(28.5, 65.0, 1013.2);
        // // expected: [WebDashboard] 28.5°C | 65% RH | 1013.2 hPa

        // ── Test 3: AlertService — below threshold prints nothing (uncomment after TODO 4) ─
         AlertService alert = new AlertService(35.0);
         alert.onReadingUpdate(28.5, 65.0, 1013.2);   // 28.5 < 35.0 → no output

        // ── Test 4: AlertService — at threshold fires alert (uncomment after TODO 4) ──────
         alert.onReadingUpdate(35.0, 70.0, 1010.0);
         // expected: [ALERT] Heat warning: 35.0°C exceeds threshold 35.0°C

        // ── Test 5: WeatherStation notifies all subscribers (uncomment after TODOs 6–8) ───
         WeatherStation station = new WeatherStation();
         PhoneDisplay p1 = new PhoneDisplay("Bob");
         WebDashboard w1 = new WebDashboard();
         AlertService a1 = new AlertService(35.0);
         station.subscribe(p1);
         station.subscribe(w1);
         station.subscribe(a1);
         System.out.println("Observer count: " + station.observerCount());  // expected: 3
         station.recordReading(22.0, 55.0, 1015.0);   // below threshold — no alert
         // expected: [Phone:Bob] 22.0°C  55% humidity
         //           [WebDashboard] 22.0°C | 55% RH | 1015.0 hPa

        // ── Test 6: Unsubscribe and re-notify (uncomment after TODO 7) ────────────────────
         station.unsubscribe(w1);
         System.out.println("After unsubscribe: " + station.observerCount());  // expected: 2
         station.recordReading(36.0, 80.0, 1008.0);   // above threshold — alert fires
         // expected: [Phone:Bob] 36.0°C  80% humidity
         //           [ALERT] Heat warning: 36.0°C exceeds threshold 35.0°C

        // ── Test 7: Null subscribe throws IAE (uncomment after TODO 7) ───────────────────
         try {
             station.subscribe(null);
             System.out.println("Test 7 — FAILED (no exception)");
         } catch (IllegalArgumentException e) {
             System.out.println("Test 7 — null guard: "
                 + ("observer must not be null".equals(e.getMessage()) ? "PASSED" : "FAILED (msg: " + e.getMessage() + ")"));
         }

        // ── Test 8: Self-unsubscribing observer during notification (no CME) (uncomment after TODO 8) ──
         WeatherStation station2 = new WeatherStation();
         WeatherObserver oneShot = new WeatherObserver() {
             @Override
             public void onReadingUpdate(double tempC, double humidity, double pressureHpa) {
                 System.out.printf("[OneShot] %.1f°C — unsubscribing%n", tempC);
                 station2.unsubscribe(this);  // safe only if snapshot copy used in recordReading
             }
         };
         station2.subscribe(new PhoneDisplay("Carol"));
         station2.subscribe(oneShot);
         station2.subscribe(new PhoneDisplay("Dave"));
         System.out.println("Before trigger: " + station2.observerCount());  // expected: 3
         station2.recordReading(25.0, 60.0, 1012.0);  // oneShot fires and unsubscribes itself
         System.out.println("After trigger: " + station2.observerCount());   // expected: 2
         System.out.println("Test 8 — self-unsubscribe no CME: PASSED");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   The weather station needs to tell multiple systems about new readings without
    //   knowing their concrete types. Think about how the station could maintain a
    //   list of "things that want to be notified" and call a single agreed-upon method
    //   on each one. The station should be able to handle new system types without any
    //   change to its own code.

    // HINT 2 (Direct):
    //   Use the Observer pattern.
    //   WeatherObserver is an interface with one method: onReadingUpdate(tempC, humidity, pressureHpa).
    //   WeatherStation holds a List<WeatherObserver> (typed as the interface).
    //   subscribe() adds to the list; unsubscribe() removes.
    //   recordReading() stores the values and notifies via a snapshot copy of the list
    //   to avoid ConcurrentModificationException when an observer unsubscribes mid-loop.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface WeatherObserver {
    //       void onReadingUpdate(double tempC, double humidity, double pressureHpa);
    //   }
    //
    //   static class PhoneDisplay implements WeatherObserver {
    //       private final String owner;
    //       PhoneDisplay(String owner) { ... }
    //       @Override public void onReadingUpdate(double tempC, double humidity, double pressureHpa) { ... }
    //   }
    //
    //   static class WebDashboard implements WeatherObserver {
    //       WebDashboard() { }
    //       @Override public void onReadingUpdate(double tempC, double humidity, double pressureHpa) { ... }
    //   }
    //
    //   static class AlertService implements WeatherObserver {
    //       private final double heatThresholdC;
    //       AlertService(double heatThresholdC) { ... }
    //       @Override public void onReadingUpdate(double tempC, double humidity, double pressureHpa) { ... }
    //   }
    //
    //   static class WeatherStation {
    //       private final List<WeatherObserver> observers;
    //       private double tempC, humidity, pressureHpa;
    //       WeatherStation() { ... }
    //       void subscribe(WeatherObserver o) { ... }      // null check + add
    //       void unsubscribe(WeatherObserver o) { ... }    // remove; no-op if absent
    //       void recordReading(double tempC, double humidity, double pressureHpa) { ... }  // snapshot copy!
    //       int  observerCount() { ... }
    //   }
}

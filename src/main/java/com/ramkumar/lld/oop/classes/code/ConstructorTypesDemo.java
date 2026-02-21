package com.ramkumar.lld.oop.classes.code;

/**
 * Explores every constructor-related concept in Java:
 *
 *  1. Default (compiler-generated)     ← shown by absence
 *  2. No-arg (explicit)
 *  3. Parameterized
 *  4. Copy constructor
 *  5. Private constructor (Singleton / Utility)
 *  6. Constructor chaining with this()
 *  7. Execution order (static block → instance block → constructor)
 */
public class ConstructorTypesDemo {

    // =========================================================================
    // Example A: Full constructor chain
    // =========================================================================

    static class Vehicle {

        private static int count = 0;

        private final int id;
        private String brand;
        private String model;
        private int year;

        // Static initializer — runs ONCE when class is first loaded
        static {
            System.out.println("[static block] Vehicle class loaded");
        }

        // Instance initializer — runs BEFORE every constructor body
        {
            id = ++count;
            System.out.println("[instance block] new Vehicle #" + id + " being created");
        }

        // ── No-arg constructor ────────────────────────────────────────────────
        public Vehicle() {
            this("Generic", "Unknown", 2000);   // delegates →
            System.out.println("[no-arg constructor] done");
        }

        // ── 1-param constructor ───────────────────────────────────────────────
        public Vehicle(String brand) {
            this(brand, "Base", 2024);           // delegates →
            System.out.println("[1-param constructor] brand=" + brand);
        }

        // ── Full parameterized constructor ────────────────────────────────────
        public Vehicle(String brand, String model, int year) {
            this.brand = brand;
            this.model = model;
            this.year  = year;
            System.out.println("[full constructor] " + brand + " " + model + " " + year);
        }

        // ── Copy constructor ──────────────────────────────────────────────────
        public Vehicle(Vehicle other) {
            this(other.brand, other.model, other.year);   // reuse full constructor
            System.out.println("[copy constructor] copied from Vehicle #" + other.id);
        }

        @Override
        public String toString() {
            return "Vehicle#" + id + "{" + brand + " " + model + " " + year + "}";
        }
    }

    // =========================================================================
    // Example B: Private constructor — Singleton pattern (sneak peek)
    // =========================================================================

    static class AppConfig {

        private static AppConfig instance;  // single instance stored here

        private String env;

        // Private constructor — no one outside can call new AppConfig()
        private AppConfig() {
            this.env = System.getenv("APP_ENV") != null
                        ? System.getenv("APP_ENV") : "development";
        }

        // Public factory method — the only way to get the instance
        public static AppConfig getInstance() {
            if (instance == null) {
                instance = new AppConfig();     // internally calls the private constructor
            }
            return instance;
        }

        public String getEnv() { return env; }
    }

    // =========================================================================
    // Example C: Utility class — private constructor to prevent instantiation
    // =========================================================================

    static class MathUtils {

        private MathUtils() {
            // Nothing to instantiate — all methods are static
            throw new UnsupportedOperationException("Utility class");
        }

        public static int square(int n) { return n * n; }
        public static boolean isEven(int n) { return n % 2 == 0; }
    }

    // =========================================================================
    // Main — run all examples
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("=== Constructor chaining order ===");
        // Chain: no-arg → 3-param (instance block runs first every time)
        Vehicle v1 = new Vehicle();
        System.out.println("Created: " + v1);

        System.out.println("\n--- 1-param chain ---");
        Vehicle v2 = new Vehicle("Toyota");
        System.out.println("Created: " + v2);

        System.out.println("\n--- Copy constructor ---");
        Vehicle v3 = new Vehicle(v2);
        System.out.println("Created: " + v3);

        // Mutating v3 does NOT affect v2 (it's a separate object)
        System.out.println("v2 == v3: " + (v2 == v3));  // false

        System.out.println("\n=== Singleton (private constructor) ===");
        AppConfig cfg1 = AppConfig.getInstance();
        AppConfig cfg2 = AppConfig.getInstance();
        System.out.println("Same instance? " + (cfg1 == cfg2));  // true
        System.out.println("Env: " + cfg1.getEnv());

        System.out.println("\n=== Utility class ===");
        System.out.println("square(5) = " + MathUtils.square(5));
        System.out.println("isEven(4) = " + MathUtils.isEven(4));
        // new MathUtils() ← would throw UnsupportedOperationException
    }
}

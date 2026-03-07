package com.ramkumar.lld.designpatterns.creational.singleton.code;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Scenario A: Application Configuration Manager
 *
 * Demonstrates all five Singleton implementation variants using a shared
 * application configuration manager as the domain model.
 *
 * Why config is a classic Singleton:
 *   - One authoritative copy of settings for the JVM process
 *   - Expensive to reload from disk on every access
 *   - All modules must see the same configuration at the same time
 *
 * Variants demonstrated (in order of increasing sophistication):
 *   1. EagerConfig           — eager initialization (simplest, always safe)
 *   2. SynchronizedConfig    — synchronized lazy init (safe, but slow)
 *   3. DCLConfig             — double-checked locking + volatile (fast + safe)
 *   4. HolderConfig          — static inner class / Bill Pugh (cleanest lazy)
 *   5. EnumConfig            — enum singleton (reflection + serialization proof)
 */
public class ApplicationConfigDemo {

    // =========================================================================
    // Variant 1 — Eager Initialization
    // Thread-safe: JVM class loading guarantees single creation
    // Trade-off: instance is created even if getInstance() is never called
    // =========================================================================

    static class EagerConfig {

        // SINGLETON PATTERN: static final field — created once when class loads
        // Thread-safety: guaranteed by JVM class-loading mechanism
        private static final EagerConfig INSTANCE = new EagerConfig();

        private final Map<String, String> config = new HashMap<>();

        // SINGLETON PATTERN: private constructor prevents external instantiation
        private EagerConfig() {
            config.put("app.name", "EagerApp");
            config.put("server.port", "8080");
            System.out.println("[EagerConfig] Instance created at class load time");
        }

        // SINGLETON PATTERN: global access point — always returns the same object
        public static EagerConfig getInstance() { return INSTANCE; }

        public String get(String key)                        { return config.get(key); }
        public String get(String key, String defaultValue)   { return config.getOrDefault(key, defaultValue); }
        public void   set(String key, String value)          { config.put(key, value); }
        public Map<String, String> getAll()                  { return Collections.unmodifiableMap(config); }
    }

    // =========================================================================
    // Variant 2 — Synchronized Method (lazy, thread-safe, but slow)
    // Every call to getInstance() acquires a lock — unnecessary after first use
    // =========================================================================

    static class SynchronizedConfig {

        // null until first call — lazy
        private static SynchronizedConfig instance;

        private final Map<String, String> config = new HashMap<>();

        private SynchronizedConfig() {
            config.put("app.name", "SynchronizedApp");
            config.put("db.url", "jdbc:mysql://localhost/db");
        }

        // SINGLETON PATTERN: synchronized on the class object
        // ⚠️ Performance: acquires lock on EVERY call, even after initialization
        public static synchronized SynchronizedConfig getInstance() {
            if (instance == null) {
                instance = new SynchronizedConfig();
            }
            return instance;
        }

        public String get(String key) { return config.get(key); }
    }

    // =========================================================================
    // Variant 3 — Double-Checked Locking (DCL) with volatile
    // The most commonly used pattern in production Java code
    // =========================================================================

    static class DCLConfig {

        // CRITICAL: volatile is mandatory.
        // Without it, the JVM may make the reference visible to other threads
        // before the constructor has finished running (instruction reordering).
        // volatile ensures a happens-before guarantee on the reference assignment.
        private static volatile DCLConfig instance;

        private final Map<String, String> config;
        private final long loadedAt;

        private DCLConfig() {
            this.config   = new HashMap<>();
            this.loadedAt = System.currentTimeMillis();
            config.put("app.name", "DCLApp");
            config.put("app.version", "2.1.0");
            config.put("db.pool.size", "10");
            config.put("server.port", "9090");
            config.put("feature.darkMode", "false");
            System.out.println("[DCLConfig] Instance created (lazy, first call)");
        }

        public static DCLConfig getInstance() {
            // FIRST CHECK: fast path — no lock if already initialized
            if (instance == null) {
                // LOCK: only one thread can initialize
                synchronized (DCLConfig.class) {
                    // SECOND CHECK: another thread may have initialized while we waited
                    if (instance == null) {
                        instance = new DCLConfig();
                    }
                }
            }
            return instance;
        }

        public String get(String key)                       { return config.get(key); }
        public String get(String key, String defaultValue)  { return config.getOrDefault(key, defaultValue); }
        public void   set(String key, String value)         { config.put(key, value); }
        public Map<String, String> getAll()                 { return Collections.unmodifiableMap(config); }
        public long   getLoadedAt()                         { return loadedAt; }
    }

    // =========================================================================
    // Variant 4 — Static Inner Class (Bill Pugh / Holder Pattern)
    // Best of both worlds: lazy + thread-safe + zero synchronization overhead
    // =========================================================================

    static class HolderConfig {

        private final Map<String, String> config = new HashMap<>();

        private HolderConfig() {
            config.put("app.name", "HolderApp");
            config.put("cache.ttl.seconds", "3600");
            System.out.println("[HolderConfig] Instance created (lazy via Holder)");
        }

        // SINGLETON PATTERN: inner class is not loaded until getInstance() is called.
        // JVM class loading initializes the inner class exactly once and is thread-safe.
        // No synchronized or volatile needed — the JVM handles it.
        private static class Holder {
            private static final HolderConfig INSTANCE = new HolderConfig();
        }

        public static HolderConfig getInstance() {
            // Triggers Holder class loading → runs static initializer → creates INSTANCE
            return Holder.INSTANCE;
        }

        public String get(String key) { return config.get(key); }
        public void   set(String key, String value) { config.put(key, value); }
    }

    // =========================================================================
    // Variant 5 — Enum Singleton (Joshua Bloch's Effective Java recommendation)
    // Immune to: reflection attacks, serialization attacks
    // Trade-off: cannot extend another class
    // =========================================================================

    enum EnumConfig {
        INSTANCE;   // SINGLETON PATTERN: only one enum constant = only one instance

        private final Map<String, String> config = new HashMap<>();

        // Enum constructor is called once by the JVM when the enum class is loaded
        EnumConfig() {
            config.put("app.name", "EnumApp");
            config.put("email.from", "noreply@example.com");
            config.put("email.smtp", "smtp.example.com");
            System.out.println("[EnumConfig] Instance created via enum initializer");
        }

        public String get(String key)                       { return config.get(key); }
        public String get(String key, String defaultValue)  { return config.getOrDefault(key, defaultValue); }
        public void   set(String key, String value)         { config.put(key, value); }

        // Enum Singleton is reflection-proof:
        // Constructor.newInstance() throws IllegalArgumentException for enums.
        // Enum Singleton is serialization-proof:
        // JVM's enum deserialization always returns the existing constant.
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" Singleton Demo: Application Configuration Manager");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ── Variant 1: Eager ──────────────────────────────────────────────────
        System.out.println("── Variant 1: Eager Initialization ────────────────────");
        EagerConfig a1 = EagerConfig.getInstance();
        EagerConfig a2 = EagerConfig.getInstance();
        System.out.println("Same instance: " + (a1 == a2));   // true
        System.out.println("app.name: " + a1.get("app.name"));

        // Modify via a1, observe via a2 — same object
        a1.set("theme", "dark");
        System.out.println("theme via a2: " + a2.get("theme"));   // dark — same object

        // ── Variant 2: Synchronized ───────────────────────────────────────────
        System.out.println("\n── Variant 2: Synchronized Method ─────────────────────");
        SynchronizedConfig b1 = SynchronizedConfig.getInstance();
        SynchronizedConfig b2 = SynchronizedConfig.getInstance();
        System.out.println("Same instance: " + (b1 == b2));
        System.out.println("db.url: " + b1.get("db.url"));

        // ── Variant 3: DCL ────────────────────────────────────────────────────
        System.out.println("\n── Variant 3: Double-Checked Locking (volatile) ────────");
        DCLConfig c1 = DCLConfig.getInstance();
        DCLConfig c2 = DCLConfig.getInstance();
        DCLConfig c3 = DCLConfig.getInstance();   // all return same instance
        System.out.println("c1 == c2: " + (c1 == c2));
        System.out.println("c2 == c3: " + (c2 == c3));
        System.out.println("app.version: " + c1.get("app.version"));
        System.out.println("Unknown key with default: " + c1.get("missing.key", "N/A"));

        // Demonstrate shared mutable state
        c1.set("feature.darkMode", "true");
        System.out.println("darkMode via c3: " + c3.get("feature.darkMode")); // true

        System.out.println("Config keys: " + c1.getAll().keySet());

        // ── Variant 4: Holder ─────────────────────────────────────────────────
        System.out.println("\n── Variant 4: Static Inner Class (Holder) ──────────────");
        HolderConfig d1 = HolderConfig.getInstance();
        HolderConfig d2 = HolderConfig.getInstance();
        System.out.println("Same instance: " + (d1 == d2));
        d1.set("max.retries", "3");
        System.out.println("max.retries via d2: " + d2.get("max.retries"));  // 3

        // ── Variant 5: Enum ───────────────────────────────────────────────────
        System.out.println("\n── Variant 5: Enum Singleton ───────────────────────────");
        EnumConfig e1 = EnumConfig.INSTANCE;
        EnumConfig e2 = EnumConfig.INSTANCE;
        System.out.println("Same instance: " + (e1 == e2));
        System.out.println("email.from: " + EnumConfig.INSTANCE.get("email.from"));

        // Reflection attack FAILS for enum — the JVM protects us:
        System.out.println("\n── Reflection attack on Enum Singleton ─────────────────");
        try {
            java.lang.reflect.Constructor<EnumConfig> c =
                    EnumConfig.class.getDeclaredConstructor(String.class, int.class);
            c.setAccessible(true);
            c.newInstance("FAKE", 0);
            System.out.println("❌ Second enum instance created (should NOT happen)");
        } catch (Exception e) {
            System.out.println("✅ Reflection blocked: " + e.getClass().getSimpleName()
                    + " — " + e.getMessage());
        }

        // ── Summary ───────────────────────────────────────────────────────────
        System.out.println("\n── Summary: All five singletons verified ───────────────");
        System.out.println("Eager      : c1 == c2? " + (EagerConfig.getInstance() == EagerConfig.getInstance()));
        System.out.println("Synchronized: c1 == c2? " + (SynchronizedConfig.getInstance() == SynchronizedConfig.getInstance()));
        System.out.println("DCL        : c1 == c2? " + (DCLConfig.getInstance() == DCLConfig.getInstance()));
        System.out.println("Holder     : c1 == c2? " + (HolderConfig.getInstance() == HolderConfig.getInstance()));
        System.out.println("Enum       : c1 == c2? " + (EnumConfig.INSTANCE == EnumConfig.INSTANCE));
    }
}

package com.ramkumar.lld.designpatterns.creational.builder.practice;

/**
 * Practice Exercise — Builder Pattern (Creational)
 * Phase 3, Topic 3.4 | Scenario B: Server Configuration Builder
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building a server configuration library. Servers have two required
 * settings (host and port) and several optional settings (connection limits,
 * timeouts, SSL). Callers must be able to set only what they need and always
 * get a validated, immutable ServerConfig in return.
 *
 * Implement using the Builder pattern with a static nested Builder class.
 *
 * ── PRODUCT: ServerConfig ───────────────────────────────────────────────
 *
 * All fields are private final (no setters — the object is immutable):
 *
 *   String  host                  (required)
 *   int     port                  (required)
 *   int     maxConnections        (optional, default = 100)
 *   long    connectionTimeoutMs   (optional, default = 5_000)
 *   long    readTimeoutMs         (optional, default = 30_000)
 *   boolean keepAlive             (optional, default = false)
 *   boolean sslEnabled            (optional, default = false)
 *   String  certPath              (optional, default = null)
 *   String  keyPath               (optional, default = null)
 *
 * The private constructor takes a Builder object and copies all fields.
 *
 * Provide a getter for every field (naming: getHost(), getPort(),
 * getMaxConnections(), getConnectionTimeoutMs(), getReadTimeoutMs(),
 * isKeepAlive(), isSslEnabled(), getCertPath(), getKeyPath()).
 *
 * toString() must produce (exact format):
 *   ServerConfig{host='<host>', port=<port>, maxConnections=<n>,
 *   connectionTimeoutMs=<ms>, readTimeoutMs=<ms>, keepAlive=<b>, ssl=<b>}
 *   (Note: certPath and keyPath are NOT included in toString for security)
 *
 * ── BUILDER: ServerConfig.Builder ────────────────────────────────────────
 *
 * Required fields in Builder constructor:
 *   Builder(String host, int port)
 *
 * Fluent setter methods (each returns the Builder):
 *   maxConnections(int val)
 *   connectionTimeoutMs(long val)
 *   readTimeoutMs(long val)
 *   keepAlive(boolean val)
 *   ssl(boolean enabled, String certPath, String keyPath)
 *     — sets sslEnabled=true, certPath, and keyPath in one call
 *
 * build() must call a private validate() then return new ServerConfig(this).
 *
 * ── VALIDATION (all in validate(), called by build()) ────────────────────
 *
 *   host null or blank   → throw new IllegalArgumentException("host must not be blank")
 *   port < 1 or > 65535 → throw new IllegalArgumentException("port must be between 1 and 65535")
 *   maxConnections < 1  → throw new IllegalArgumentException("maxConnections must be >= 1")
 *   connectionTimeoutMs < 1 → throw new IllegalArgumentException("connectionTimeoutMs must be >= 1")
 *   readTimeoutMs < 1   → throw new IllegalArgumentException("readTimeoutMs must be >= 1")
 *   sslEnabled && (certPath null or blank) → throw new IllegalStateException("ssl requires certPath")
 *   sslEnabled && (keyPath  null or blank) → throw new IllegalStateException("ssl requires keyPath")
 *
 * ── DESIGN CONSTRAINTS ────────────────────────────────────────────────────
 *   - ServerConfig constructor must be private (no public constructor)
 *   - No setters on ServerConfig — immutable after build()
 *   - Builder is a static nested class inside ServerConfig
 *   - All validation in one private validate() method (not in individual setters)
 *   - Fluent setters (except ssl()) return `this` — the same Builder instance
 *
 * ═══════════════════════════════════════════════════════════════════════
 */
public class ServerConfigPractice {

    // =========================================================================
    // PRODUCT — immutable; all fields private final; private constructor
    // =========================================================================

    static final class ServerConfig {

        // ── TODO 1: Declare all product fields — ALL must be private final
        private final String host;
        private final int port;
        private final int maxConnections;
        private final long connectionTimeoutMs;
        private final long readTimeoutMs;
        private final boolean keepAlive;
        private final boolean sslEnabled;
        private final String certPath;
        private final String keyPath;



        // ── TODO 2: private constructor — takes a Builder, copies every field
        private ServerConfig(Builder b) {
            this.host                = b.host;
            this.port                = b.port;
            this.maxConnections      = b.maxConnections;
            this.connectionTimeoutMs = b.connectionTimeoutMs;
            this.readTimeoutMs       = b.readTimeoutMs;
            this.keepAlive           = b.keepAlive;
            this.sslEnabled          = b.sslEnabled;
            this.certPath            = b.certPath;
            this.keyPath             = b.keyPath;
        }


        // ── TODO 3: Getters — no setters (product is immutable)
        public String getHost() { return this.host; }
        public int getPort() { return this.port; }
        public int getMaxConnections() { return this.maxConnections; }
        public long getConnectionTimeoutMs() { return this.connectionTimeoutMs; }
        public long getReadTimeoutMs() { return this.readTimeoutMs; }
        public boolean isKeepAlive() { return this.keepAlive; }
        public boolean isSslEnabled() { return this.sslEnabled; }
        public String getCertPath() { return this.certPath; }
        public String getKeyPath() { return this.keyPath; }


        // ── TODO 4: toString() — exact format required for Test 1 and Test 4
        public String toString() {
            return String.format("ServerConfig{host='%s', port=%d, maxConnections=%d, " +
                    "connectionTimeoutMs=%d, readTimeoutMs=%d, keepAlive=%b, ssl=%b}", this.host,
                            this.port, this.maxConnections, this.connectionTimeoutMs, this.readTimeoutMs, this.keepAlive,this.sslEnabled);
        }
        // =====================================================================
        // BUILDER — static nested class
        // =====================================================================

        static final class Builder {

            // ── TODO 5: Required fields — private final; set only in Builder constructor
            private final String host;
            private final int port;

            // ── TODO 6: Optional fields — non-final; MUST have these exact default values
            private int maxConnections = 100;
            private long connectionTimeoutMs = 5_000L;
            private long readTimeoutMs = 30_000L;
            private boolean keepAlive = false;
            private boolean sslEnabled = false;
            private String certPath;
            private String keyPath;

            // ── TODO 7: Builder constructor — accepts required fields only
            public Builder(String host, int port) {
                    this.host = host;
                    this.port = port;
            }

            // ── TODO 8: Fluent setter — maxConnections(int val)
            //    Constraint: just stores val, returns this (do NOT validate here)
            //    Return type: Builder
            public Builder maxConnections(int val) {
                // your code here
                this.maxConnections = val;
                return this;
            }

            // ── TODO 9: Fluent setter — connectionTimeoutMs(long val)
            //    Constraint: just stores val, returns this (do NOT validate here)
            //    Return type: Builder
            public Builder connectionTimeoutMs(long val) {
                // your code here
                this.connectionTimeoutMs = val;
                return this;
            }

            // ── TODO 10: Fluent setter — readTimeoutMs(long val)
            //    Constraint: just stores val, returns this (do NOT validate here)
            //    Return type: Builder
            public Builder readTimeoutMs(long val) {
                // your code here
                this.readTimeoutMs = val;
                return this;
            }

            // ── TODO 11: Fluent setter — keepAlive(boolean val)
            //    Constraint: just stores val, returns this (do NOT validate here)
            //    Return type: Builder
            public Builder keepAlive(boolean val) {
                // your code here
                this.keepAlive = val;
                return this;
            }

            // ── TODO 12: Fluent setter — ssl(boolean enabled, String certPath, String keyPath)
            //    Sets: this.sslEnabled = enabled, this.certPath = certPath, this.keyPath = keyPath
            //    Constraint: just stores values, returns this (do NOT validate here)
            //    Return type: Builder
            public Builder ssl(boolean enabled, String certPath, String keyPath) {
                // your code here
                this.sslEnabled = enabled;
                this.certPath = certPath;
                this.keyPath = keyPath;
                return this;
            }

            // ── TODO 13: private validate() — all validation in this one method
            //    Check in this order:
            //    1. host == null || host.isBlank()
            //       → throw new IllegalArgumentException("host must not be blank")
            //    2. port < 1 || port > 65535
            //       → throw new IllegalArgumentException("port must be between 1 and 65535")
            //    3. maxConnections < 1
            //       → throw new IllegalArgumentException("maxConnections must be >= 1")
            //    4. connectionTimeoutMs < 1
            //       → throw new IllegalArgumentException("connectionTimeoutMs must be >= 1")
            //    5. readTimeoutMs < 1
            //       → throw new IllegalArgumentException("readTimeoutMs must be >= 1")
            //    6. sslEnabled && (certPath == null || certPath.isBlank())
            //       → throw new IllegalStateException("ssl requires certPath")
            //       (ISE not IAE — it is the COMBINATION of fields that is invalid, not one arg)
            //    7. sslEnabled && (keyPath == null || keyPath.isBlank())
            //       → throw new IllegalStateException("ssl requires keyPath")
            private void validate() {
                // your code here
                if(host == null || host.isBlank()) {
                    throw new IllegalArgumentException("host must not be blank");
                }
                if(port < 1 || port > 65535){
                    throw new IllegalArgumentException("port must be between 1 and 65535");
                }
                if(maxConnections <  1){
                    throw new IllegalArgumentException("maxConnections must be >= 1");
                }
                if(connectionTimeoutMs < 1){
                    throw new IllegalArgumentException("connectionTimeoutMs must be >= 1");
                }
                if(readTimeoutMs <  1) {
                    throw new IllegalArgumentException("readTimeoutMs must be >= 1");
                }
                if(sslEnabled && (certPath == null || certPath.isBlank())) {
                    throw new IllegalStateException("ssl requires certPath");
                }
                if(sslEnabled && (keyPath == null || keyPath.isBlank())) {
                    throw new IllegalStateException("ssl requires keyPath");
                }
            }

            // ── TODO 14: build() — calls validate(), then returns new ServerConfig(this)
            //    Return type: ServerConfig
            public ServerConfig build() {
                // your code here
                validate();
                return new ServerConfig(this); // replace with: validate(); return new ServerConfig(this);
            }
        }
    }

    // =========================================================================
    // DO NOT MODIFY — test cases; fill in the TODOs above to make them pass
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: Minimal config — only required fields ═══════════");
        ServerConfig minimal = new ServerConfig.Builder("localhost", 8080)
            .build();
        System.out.println(minimal);
        boolean t1 = "localhost".equals(minimal.getHost())
                  && minimal.getPort() == 8080
                  && minimal.getMaxConnections() == 100
                  && minimal.getConnectionTimeoutMs() == 5_000L
                  && minimal.getReadTimeoutMs() == 30_000L
                  && !minimal.isKeepAlive()
                  && !minimal.isSslEnabled()
                  && minimal.getCertPath() == null
                  && minimal.getKeyPath() == null;
        System.out.println("Test 1 " + (t1 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 2: Full config — all optional fields set ═══════════");
        ServerConfig full = new ServerConfig.Builder("prod-db.internal", 5432)
            .maxConnections(500)
            .connectionTimeoutMs(3_000L)
            .readTimeoutMs(15_000L)
            .keepAlive(true)
            .ssl(true, "/etc/ssl/cert.pem", "/etc/ssl/key.pem")
            .build();
        System.out.println(full);
        boolean t2 = "prod-db.internal".equals(full.getHost())
                  && full.getPort() == 5432
                  && full.getMaxConnections() == 500
                  && full.getConnectionTimeoutMs() == 3_000L
                  && full.getReadTimeoutMs() == 15_000L
                  && full.isKeepAlive()
                  && full.isSslEnabled()
                  && "/etc/ssl/cert.pem".equals(full.getCertPath())
                  && "/etc/ssl/key.pem".equals(full.getKeyPath());
        System.out.println("Test 2 " + (t2 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 3: Immutability — no setters on ServerConfig ═══════");
        // Verified structurally: if the code compiles without setters, this passes.
        // Try calling minimal.setHost("hacked") — it should NOT compile.
        System.out.println("Test 3 PASSED — ServerConfig has no setters (compile-time check)");

        System.out.println("\n═══ Test 4: toString() format ═══════════════════════════════");
        ServerConfig cfg4 = new ServerConfig.Builder("api.example.com", 443)
            .maxConnections(200)
            .connectionTimeoutMs(2_000L)
            .readTimeoutMs(10_000L)
            .keepAlive(true)
            .ssl(true, "/certs/cert.pem", "/certs/key.pem")
            .build();
        String expected4 = "ServerConfig{host='api.example.com', port=443, maxConnections=200, " +
                           "connectionTimeoutMs=2000, readTimeoutMs=10000, keepAlive=true, ssl=true}";
        boolean t4 = expected4.equals(cfg4.toString());
        System.out.println("Expected: " + expected4);
        System.out.println("Actual:   " + cfg4);
        System.out.println("Test 4 " + (t4 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: Blank host → IAE ════════════════════════════════");
        try {
            new ServerConfig.Builder("  ", 8080).build();
            System.out.println("Test 5 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t5 = "host must not be blank".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 5 " + (t5 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 6: Port 0 → IAE ════════════════════════════════════");
        try {
            new ServerConfig.Builder("localhost", 0).build();
            System.out.println("Test 6 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t6 = "port must be between 1 and 65535".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 6 " + (t6 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 7: Port 65536 → IAE ════════════════════════════════");
        try {
            new ServerConfig.Builder("localhost", 65536).build();
            System.out.println("Test 7 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t7 = "port must be between 1 and 65535".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 7 " + (t7 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 8: maxConnections 0 → IAE ══════════════════════════");
        try {
            new ServerConfig.Builder("localhost", 8080)
                .maxConnections(0)
                .build();
            System.out.println("Test 8 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t8 = "maxConnections must be >= 1".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 8 " + (t8 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 9: Negative connectionTimeoutMs → IAE ══════════════");
        try {
            new ServerConfig.Builder("localhost", 8080)
                .connectionTimeoutMs(-1L)
                .build();
            System.out.println("Test 9 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t9 = "connectionTimeoutMs must be >= 1".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 9 " + (t9 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 10: SSL without certPath → ISE ═════════════════════");
        try {
            new ServerConfig.Builder("localhost", 443)
                .ssl(true, null, "/etc/ssl/key.pem")
                .build();
            System.out.println("Test 10 FAILED — should have thrown");
        } catch (IllegalStateException e) {
            boolean t10 = "ssl requires certPath".equals(e.getMessage());
            System.out.println("Caught ISE: " + e.getMessage());
            System.out.println("Test 10 " + (t10 ? "PASSED" : "FAILED — wrong message or wrong exception type"));
        } catch (IllegalArgumentException e) {
            System.out.println("Test 10 FAILED — threw IAE but should be ISE: " + e.getMessage());
        }

        System.out.println("\n═══ Test 11: SSL without keyPath → ISE ══════════════════════");
        try {
            new ServerConfig.Builder("localhost", 443)
                .ssl(true, "/etc/ssl/cert.pem", "")
                .build();
            System.out.println("Test 11 FAILED — should have thrown");
        } catch (IllegalStateException e) {
            boolean t11 = "ssl requires keyPath".equals(e.getMessage());
            System.out.println("Caught ISE: " + e.getMessage());
            System.out.println("Test 11 " + (t11 ? "PASSED" : "FAILED — wrong message or wrong exception type"));
        } catch (IllegalArgumentException e) {
            System.out.println("Test 11 FAILED — threw IAE but should be ISE: " + e.getMessage());
        }

        System.out.println("\n═══ Test 12: Fluent chain — same Builder returned each time ═");
        ServerConfig.Builder builder12 = new ServerConfig.Builder("localhost", 9090);
        ServerConfig.Builder after1 = builder12.maxConnections(50);
        ServerConfig.Builder after2 = after1.keepAlive(true);
        boolean t12 = (builder12 == after1) && (after1 == after2);
        System.out.println("Same builder instance returned: " + t12);
        System.out.println("Test 12 " + (t12 ? "PASSED" : "FAILED — fluent setters must return `this`"));

        System.out.println("\n═══ Test 13: SSL with both paths → success ══════════════════");
        try {
            ServerConfig sslCfg = new ServerConfig.Builder("secure.example.com", 443)
                .ssl(true, "/certs/server.crt", "/certs/server.key")
                .build();
            boolean t13 = sslCfg.isSslEnabled()
                       && "/certs/server.crt".equals(sslCfg.getCertPath())
                       && "/certs/server.key".equals(sslCfg.getKeyPath());
            System.out.println("Test 13 " + (t13 ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test 13 FAILED — unexpected exception: " + e.getMessage());
        }

        System.out.println("\n═══ Test 14: certPath/keyPath absent from toString ═══════════");
        ServerConfig cfg14 = new ServerConfig.Builder("vault.internal", 8200)
            .ssl(true, "/secret/cert.pem", "/secret/key.pem")
            .build();
        String str14 = cfg14.toString();
        boolean t14 = !str14.contains("cert.pem") && !str14.contains("key.pem");
        System.out.println("toString: " + str14);
        System.out.println("Test 14 " + (t14 ? "PASSED — cert/key paths hidden" : "FAILED — cert/key paths leaked in toString"));
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * HINTS — read only if stuck; try for at least 20 minutes first
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle):
 *   The product class (ServerConfig) should have only getters — no setters.
 *   The inner class (Builder) holds mutable state while building.
 *   Think about which fields must always be provided vs which ones are optional.
 *   Consider: where should validation happen so cross-field rules (ssl + certPath)
 *   can be checked after ALL values are set?
 *
 * HINT 2 (Direct):
 *   - Use the Static Nested Builder pattern: `static class Builder` inside `ServerConfig`.
 *   - Required fields (host, port) → `private final` in Builder, set in Builder constructor.
 *   - Optional fields → non-final with defaults in Builder.
 *   - Fluent setters: `public Builder maxConnections(int val) { this.maxConnections = val; return this; }`
 *   - Validation in: `private void validate()` called from `build()`.
 *   - Cross-field rules (ssl) → IllegalStateException (not IAE) because the state
 *     of the builder is inconsistent, not a single argument.
 *   - `build()`: call validate(), then `return new ServerConfig(this)`.
 *   - ServerConfig constructor: `private ServerConfig(Builder b)` copies each field.
 *
 * HINT 3 (Near-solution — class skeleton):
 *
 *   static final class ServerConfig {
 *       private final String  host;
 *       private final int     port;
 *       private final int     maxConnections;
 *       private final long    connectionTimeoutMs;
 *       private final long    readTimeoutMs;
 *       private final boolean keepAlive;
 *       private final boolean sslEnabled;
 *       private final String  certPath;
 *       private final String  keyPath;
 *
 *       private ServerConfig(Builder b) {
 *           this.host                = b.host;
 *           this.port                = b.port;
 *           this.maxConnections      = b.maxConnections;
 *           this.connectionTimeoutMs = b.connectionTimeoutMs;
 *           this.readTimeoutMs       = b.readTimeoutMs;
 *           this.keepAlive           = b.keepAlive;
 *           this.sslEnabled          = b.sslEnabled;
 *           this.certPath            = b.certPath;
 *           this.keyPath             = b.keyPath;
 *       }
 *
 *       public String  getHost()                { return host; }
 *       // ... remaining getters ...
 *
 *       @Override
 *       public String toString() {
 *           return String.format(
 *               "ServerConfig{host='%s', port=%d, maxConnections=%d, " +
 *               "connectionTimeoutMs=%d, readTimeoutMs=%d, keepAlive=%b, ssl=%b}",
 *               host, port, maxConnections, connectionTimeoutMs, readTimeoutMs,
 *               keepAlive, sslEnabled);
 *       }
 *
 *       static final class Builder {
 *           private final String  host;
 *           private final int     port;
 *           private int     maxConnections      = 100;
 *           private long    connectionTimeoutMs = 5_000L;
 *           private long    readTimeoutMs       = 30_000L;
 *           private boolean keepAlive           = false;
 *           private boolean sslEnabled          = false;
 *           private String  certPath            = null;
 *           private String  keyPath             = null;
 *
 *           public Builder(String host, int port) { this.host = host; this.port = port; }
 *
 *           public Builder maxConnections(int val)        { this.maxConnections = val;      return this; }
 *           public Builder connectionTimeoutMs(long val)  { this.connectionTimeoutMs = val; return this; }
 *           public Builder readTimeoutMs(long val)        { this.readTimeoutMs = val;       return this; }
 *           public Builder keepAlive(boolean val)         { this.keepAlive = val;           return this; }
 *           public Builder ssl(boolean en, String cert, String key) {
 *               this.sslEnabled = en; this.certPath = cert; this.keyPath = key; return this;
 *           }
 *
 *           private void validate() { ... }  // see TODO 13 for full rules
 *           public ServerConfig build() { validate(); return new ServerConfig(this); }
 *       }
 *   }
 */

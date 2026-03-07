package com.ramkumar.lld.designpatterns.creational.builder.results;

/**
 * Reference Solution — Builder Pattern (Creational)
 * Phase 3, Topic 3.4 | Scenario B: Server Configuration Builder
 *
 * Key fixes over the practice submission:
 *   1. Explicit = null on certPath and keyPath in Builder (documents intent)
 *   2. @Override on toString()
 *   3. No stale scaffold comments
 *   Extra Test 15: validates that setters do NOT throw — deferred to build() only
 */
public class BuilderReference {

    // =========================================================================
    // PRODUCT — immutable; private constructor; no setters
    // =========================================================================

    // [Product] — all fields private final; constructed only via Builder.build()
    static final class ServerConfig {

        // [Encapsulation] All fields private final — immutable after construction
        private final String  host;
        private final int     port;
        private final int     maxConnections;
        private final long    connectionTimeoutMs;
        private final long    readTimeoutMs;
        private final boolean keepAlive;
        private final boolean sslEnabled;
        private final String  certPath;
        private final String  keyPath;

        // [Private constructor] — only Builder.build() may call this
        // Takes the whole Builder so the parameter list never grows
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

        // [Getters only — no setters] product is immutable after build()
        public String  getHost()                { return host; }
        public int     getPort()                { return port; }
        public int     getMaxConnections()      { return maxConnections; }
        public long    getConnectionTimeoutMs() { return connectionTimeoutMs; }
        public long    getReadTimeoutMs()       { return readTimeoutMs; }
        public boolean isKeepAlive()            { return keepAlive; }
        public boolean isSslEnabled()           { return sslEnabled; }
        public String  getCertPath()            { return certPath; }
        public String  getKeyPath()             { return keyPath; }

        // [Fix 2] @Override ensures the compiler checks we're overriding Object.toString()
        @Override
        public String toString() {
            // certPath and keyPath intentionally excluded — security-sensitive paths
            return String.format(
                "ServerConfig{host='%s', port=%d, maxConnections=%d, " +
                "connectionTimeoutMs=%d, readTimeoutMs=%d, keepAlive=%b, ssl=%b}",
                host, port, maxConnections, connectionTimeoutMs, readTimeoutMs,
                keepAlive, sslEnabled);
        }

        // =====================================================================
        // BUILDER — static nested so it can access ServerConfig's private constructor
        // =====================================================================

        // [Builder] static nested class — lives inside ServerConfig for encapsulation
        static final class Builder {

            // [Required fields] private final — must be set in constructor; never change
            private final String host;
            private final int    port;

            // [Optional fields] non-final; have sensible defaults
            // [Fix 1] explicit = null documents that null is the valid "not configured" state
            private int     maxConnections      = 100;
            private long    connectionTimeoutMs = 5_000L;
            private long    readTimeoutMs       = 30_000L;
            private boolean keepAlive           = false;
            private boolean sslEnabled          = false;
            private String  certPath            = null;
            private String  keyPath             = null;

            // [Builder constructor] required fields only — signals what is mandatory
            public Builder(String host, int port) {
                this.host = host;
                this.port = port;
            }

            // [Fluent setters] each returns `this` — enables chaining
            // None validates — deferred validation belongs in build() only
            public Builder maxConnections(int val)       { this.maxConnections = val;      return this; }
            public Builder connectionTimeoutMs(long val) { this.connectionTimeoutMs = val; return this; }
            public Builder readTimeoutMs(long val)       { this.readTimeoutMs = val;       return this; }
            public Builder keepAlive(boolean val)        { this.keepAlive = val;           return this; }

            // ssl() sets three related fields in one call — keeps the cross-field invariant visible
            public Builder ssl(boolean enabled, String certPath, String keyPath) {
                this.sslEnabled = enabled;
                this.certPath   = certPath;
                this.keyPath    = keyPath;
                return this;
            }

            // [Validation] all rules in one place — cross-field rules require all fields set first
            private void validate() {
                // Single-field rules → IllegalArgumentException (one bad argument)
                if (host == null || host.isBlank())
                    throw new IllegalArgumentException("host must not be blank");
                if (port < 1 || port > 65535)
                    throw new IllegalArgumentException("port must be between 1 and 65535");
                if (maxConnections < 1)
                    throw new IllegalArgumentException("maxConnections must be >= 1");
                if (connectionTimeoutMs < 1)
                    throw new IllegalArgumentException("connectionTimeoutMs must be >= 1");
                if (readTimeoutMs < 1)
                    throw new IllegalArgumentException("readTimeoutMs must be >= 1");

                // Cross-field rules → IllegalStateException (the builder's state is inconsistent)
                // Cannot be expressed in a setter — both sslEnabled and certPath must already be set
                if (sslEnabled && (certPath == null || certPath.isBlank()))
                    throw new IllegalStateException("ssl requires certPath");
                if (sslEnabled && (keyPath == null || keyPath.isBlank()))
                    throw new IllegalStateException("ssl requires keyPath");
            }

            // [build()] the only entry point to create a ServerConfig
            public ServerConfig build() {
                validate();                    // throws before constructing an invalid product
                return new ServerConfig(this); // private constructor — only reachable from here
            }
        }
    }

    // =========================================================================
    // Tests — same 14 as practice + Test 15 (catches validation-in-setter mistake)
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
            new ServerConfig.Builder("localhost", 8080).maxConnections(0).build();
            System.out.println("Test 8 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t8 = "maxConnections must be >= 1".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 8 " + (t8 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 9: Negative connectionTimeoutMs → IAE ══════════════");
        try {
            new ServerConfig.Builder("localhost", 8080).connectionTimeoutMs(-1L).build();
            System.out.println("Test 9 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t9 = "connectionTimeoutMs must be >= 1".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 9 " + (t9 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 10: SSL without certPath → ISE ═════════════════════");
        try {
            new ServerConfig.Builder("localhost", 443).ssl(true, null, "/key.pem").build();
            System.out.println("Test 10 FAILED — should have thrown");
        } catch (IllegalStateException e) {
            boolean t10 = "ssl requires certPath".equals(e.getMessage());
            System.out.println("Caught ISE: " + e.getMessage());
            System.out.println("Test 10 " + (t10 ? "PASSED" : "FAILED — wrong message or type"));
        } catch (IllegalArgumentException e) {
            System.out.println("Test 10 FAILED — threw IAE but should be ISE: " + e.getMessage());
        }

        System.out.println("\n═══ Test 11: SSL without keyPath → ISE ══════════════════════");
        try {
            new ServerConfig.Builder("localhost", 443).ssl(true, "/cert.pem", "").build();
            System.out.println("Test 11 FAILED — should have thrown");
        } catch (IllegalStateException e) {
            boolean t11 = "ssl requires keyPath".equals(e.getMessage());
            System.out.println("Caught ISE: " + e.getMessage());
            System.out.println("Test 11 " + (t11 ? "PASSED" : "FAILED — wrong message or type"));
        } catch (IllegalArgumentException e) {
            System.out.println("Test 11 FAILED — threw IAE but should be ISE: " + e.getMessage());
        }

        System.out.println("\n═══ Test 12: Fluent chain — same Builder returned each time ═");
        ServerConfig.Builder b12 = new ServerConfig.Builder("localhost", 9090);
        ServerConfig.Builder a1  = b12.maxConnections(50);
        ServerConfig.Builder a2  = a1.keepAlive(true);
        boolean t12 = (b12 == a1) && (a1 == a2);
        System.out.println("Test 12 " + (t12 ? "PASSED" : "FAILED — fluent setters must return `this`"));

        System.out.println("\n═══ Test 13: SSL with both paths → success ══════════════════");
        try {
            ServerConfig ssl = new ServerConfig.Builder("secure.example.com", 443)
                .ssl(true, "/certs/server.crt", "/certs/server.key").build();
            boolean t13 = ssl.isSslEnabled()
                       && "/certs/server.crt".equals(ssl.getCertPath())
                       && "/certs/server.key".equals(ssl.getKeyPath());
            System.out.println("Test 13 " + (t13 ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test 13 FAILED — unexpected: " + e.getMessage());
        }

        System.out.println("\n═══ Test 14: certPath/keyPath absent from toString ═══════════");
        ServerConfig cfg14 = new ServerConfig.Builder("vault.internal", 8200)
            .ssl(true, "/secret/cert.pem", "/secret/key.pem").build();
        String str14 = cfg14.toString();
        boolean t14 = !str14.contains("cert.pem") && !str14.contains("key.pem");
        System.out.println("toString: " + str14);
        System.out.println("Test 14 " + (t14 ? "PASSED — paths hidden" : "FAILED — paths leaked"));

        // ── Test 15: Deferred validation — setter must NOT throw; only build() throws ──
        // Most common mistake: putting validation inside individual setters.
        // If maxConnections(0) validates immediately, it throws here instead of at build().
        // Tests 1-14 never call a setter with an invalid value, so they can't catch this.
        System.out.println("\n═══ Test 15: Invalid setter value must NOT throw until build() ═");
        ServerConfig.Builder b15 = new ServerConfig.Builder("localhost", 8080);
        boolean setterThrew = false;
        try {
            b15.maxConnections(0); // MUST NOT throw — deferred validation
        } catch (Exception e) {
            setterThrew = true;
            System.out.println("Test 15 FAILED — setter threw early: " + e.getMessage());
        }
        if (!setterThrew) {
            try {
                b15.build(); // MUST throw here
                System.out.println("Test 15 FAILED — build() should have thrown");
            } catch (IllegalArgumentException e) {
                System.out.println("Caught IAE at build(): " + e.getMessage());
                System.out.println("Test 15 PASSED — validation deferred correctly to build()");
            }
        }
    }
}

package com.ramkumar.lld.designpatterns.creational.builder.code;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Scenario A — HTTP Request Builder
 *
 * Demonstrates the Builder pattern using an immutable HTTP request object.
 * Shows how to separate construction (step-by-step, fluent API) from the
 * final immutable product, with all validation deferred to build().
 *
 * ── Participants ────────────────────────────────────────────────────────────
 *   Product         →  HttpRequest          (immutable, private constructor)
 *   Builder         →  HttpRequest.Builder  (fluent setters, validates at build())
 *   Client          →  main()               (uses only the Builder fluent API)
 * ────────────────────────────────────────────────────────────────────────────
 */
public class HttpRequestDemo {

    // =========================================================================
    // PRODUCT — immutable; no setters; private constructor
    // =========================================================================

    // [Product]
    static final class HttpRequest {

        // [Encapsulation] All fields private final — set once at construction
        private final String              method;          // required
        private final String              url;             // required
        private final Map<String, String> headers;         // optional, default = {}
        private final String              body;            // optional, default = null
        private final int                 timeoutMs;       // optional, default = 5000
        private final boolean             followRedirects; // optional, default = true

        // [Private constructor] — only Builder.build() may call this
        // Takes the entire Builder so it doesn't need N parameters
        private HttpRequest(Builder b) {
            this.method          = b.method;
            this.url             = b.url;
            // [Defensive copy] — caller's Map cannot mutate our internal state
            this.headers         = Collections.unmodifiableMap(new HashMap<>(b.headers));
            this.body            = b.body;
            this.timeoutMs       = b.timeoutMs;
            this.followRedirects = b.followRedirects;
        }

        // [Getters only — no setters] — product is immutable after build()
        public String              getMethod()          { return method; }
        public String              getUrl()             { return url; }
        public Map<String, String> getHeaders()         { return headers; } // already unmodifiable
        public String              getBody()            { return body; }
        public int                 getTimeoutMs()       { return timeoutMs; }
        public boolean             isFollowRedirects()  { return followRedirects; }

        @Override
        public String toString() {
            return String.format(
                "HttpRequest{method='%s', url='%s', headers=%s, body=%s, timeoutMs=%d, followRedirects=%b}",
                method, url, headers, body == null ? "null" : "'" + body + "'",
                timeoutMs, followRedirects);
        }

        // =====================================================================
        // BUILDER — static nested class for encapsulation
        // =====================================================================

        // [Builder] — static nested so it can access HttpRequest's private constructor
        static final class Builder {

            // [Required fields] — final; set in Builder constructor; never change
            private final String method;
            private final String url;

            // [Optional fields] — have sensible defaults; may be overridden by fluent setters
            private Map<String, String> headers         = new HashMap<>();
            private String              body            = null;
            private int                 timeoutMs       = 5_000;
            private boolean             followRedirects = true;

            // [Builder constructor] — only required fields; caller must supply them
            Builder(String method, String url) {
                this.method = method;
                this.url    = url;
            }

            // ── Fluent setters — each returns `this` to enable chaining ─────

            // [Fluent API] — caller writes .header("k","v").body("...").timeoutMs(10_000)
            Builder header(String name, String value) {
                this.headers.put(name, value);
                return this;   // ← returning `this` is what makes it "fluent"
            }

            Builder body(String body) {
                this.body = body;
                return this;
            }

            Builder timeoutMs(int ms) {
                this.timeoutMs = ms;
                return this;
            }

            Builder followRedirects(boolean follow) {
                this.followRedirects = follow;
                return this;
            }

            // [Validation] — deferred to build(); cross-field rules expressible here
            private void validate() {
                // Single-field rules
                if (method == null || method.isBlank())
                    throw new IllegalArgumentException("method must not be blank");

                Set<String> VALID_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD");
                if (!VALID_METHODS.contains(method.toUpperCase()))
                    throw new IllegalArgumentException("unsupported method: " + method);

                if (url == null || url.isBlank())
                    throw new IllegalArgumentException("url must not be blank");

                if (!url.startsWith("http://") && !url.startsWith("https://"))
                    throw new IllegalArgumentException("url must start with http:// or https://");

                if (timeoutMs < 1)
                    throw new IllegalArgumentException("timeoutMs must be >= 1");

                // Cross-field rule — only checkable when all fields are known
                if ((method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD"))
                        && body != null && !body.isBlank())
                    throw new IllegalStateException("GET/HEAD requests must not have a body");
            }

            // [build()] — the only entry point to create an HttpRequest
            HttpRequest build() {
                validate();
                return new HttpRequest(this);
            }
        }
    }

    // =========================================================================
    // DEMO
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Builder — HTTP Request Demo ════════════════════════════");

        // [Builder usage — only required args in constructor]
        System.out.println("\n── Case 1: Minimal GET request (only required fields) ───────");
        HttpRequest getReq = new HttpRequest.Builder("GET", "https://api.example.com/users")
            .build();   // ← no fluent setters used — all defaults apply
        System.out.println(getReq);
        System.out.println("  timeout default: " + getReq.getTimeoutMs());       // 5000
        System.out.println("  followRedirects: " + getReq.isFollowRedirects());  // true

        // [Fluent API] — every setter returns `this`, so chaining works
        System.out.println("\n── Case 2: Full POST request (all optional fields set) ──────");
        HttpRequest postReq = new HttpRequest.Builder("POST", "https://api.example.com/users")
            .header("Authorization", "Bearer abc123")
            .header("Content-Type", "application/json")
            .body("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}")
            .timeoutMs(15_000)
            .followRedirects(false)
            .build();
        System.out.println(postReq);
        System.out.println("  body present: " + (postReq.getBody() != null));

        // [Immutability] — headers map cannot be modified after build()
        System.out.println("\n── Case 3: Immutability check ───────────────────────────────");
        try {
            postReq.getHeaders().put("X-Injected", "evil");
            System.out.println("  FAIL — should have thrown");
        } catch (UnsupportedOperationException e) {
            System.out.println("  PASSED — headers map is unmodifiable after build()");
        }

        // [Validation — invalid method]
        System.out.println("\n── Case 4: Invalid HTTP method ──────────────────────────────");
        try {
            new HttpRequest.Builder("BREW", "https://api.example.com").build();
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught IAE: " + e.getMessage());
        }

        // [Validation — bad URL]
        System.out.println("\n── Case 5: Bad URL (no scheme) ──────────────────────────────");
        try {
            new HttpRequest.Builder("GET", "api.example.com/users").build();
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught IAE: " + e.getMessage());
        }

        // [Cross-field validation — GET with body]
        System.out.println("\n── Case 6: Cross-field — GET with body ──────────────────────");
        try {
            new HttpRequest.Builder("GET", "https://api.example.com")
                .body("{\"invalid\":true}")
                .build();
        } catch (IllegalStateException e) {
            // [ISE not IAE] — cross-field rule; no single argument is wrong
            System.out.println("  Caught ISE: " + e.getMessage());
        }

        // [OCP] — new methods (PUT, DELETE) require zero changes to HttpRequest
        System.out.println("\n── Case 7: PUT and DELETE requests ──────────────────────────");
        HttpRequest put = new HttpRequest.Builder("PUT", "https://api.example.com/users/42")
            .body("{\"name\":\"Bob\"}")
            .build();
        HttpRequest del = new HttpRequest.Builder("DELETE", "https://api.example.com/users/42")
            .build();
        System.out.println("  PUT:    " + put.getMethod() + " " + put.getUrl());
        System.out.println("  DELETE: " + del.getMethod() + " " + del.getUrl());

        System.out.println("\n── Builder Pattern Summary ──────────────────────────────────");
        System.out.println("  Required fields set in Builder constructor (method, url)");
        System.out.println("  Optional fields have defaults (timeoutMs=5000, followRedirects=true)");
        System.out.println("  All validation deferred to build() — cross-field rules possible");
        System.out.println("  Product is immutable — no setters, defensive copy of headers");
        System.out.println("  Private constructor — only Builder.build() can create HttpRequest");
    }
}

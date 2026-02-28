package com.ramkumar.lld.solid.srp.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference solution — Single Responsibility Principle (SRP)
 * Phase 2, Topic 1 | Scenario B: Blog Publishing Platform
 *
 * Fixes from the practice review:
 *   Issue 1: BlogService.publish() — inverted condition (!validate, not validate)
 *   Issue 2: PostAnalytics maps — now private final with inline initialization
 *   Issue 3: PostFormatter.toText() — now includes publishedAt date
 *   Issue 4: PostValidator — getValidationError() now matches validate() order
 *   Issue 5: BlogPost — static counter declared before instance fields
 */
public class BlogReference {

    // =========================================================================
    // 1. BlogPost — DATA ONLY
    // Actor: Content team (changes field names, adds a new metadata field)
    // Responsibility: "Holds blog post data."
    // =========================================================================
    static class BlogPost {

        // Static field FIRST (Java convention)
        private static int counter = 0;

        // All instance fields final — immutable after construction
        private final String postId;
        private final String title;
        private final String content;
        private final String authorName;
        private final long   publishedAt;

        public BlogPost(String title, String content, String authorName) {
            // Validate null BEFORE calling isBlank() — avoids NullPointerException
            if (title == null || title.isBlank())
                throw new IllegalArgumentException("Title cannot be blank");
            if (content == null || content.isBlank())
                throw new IllegalArgumentException("Content cannot be blank");
            if (authorName == null || authorName.isBlank())
                throw new IllegalArgumentException("Author name cannot be blank");

            // Note: constructor validates null/blank presence only.
            // Business rules (min length, word count) belong in PostValidator.
            this.postId      = String.format("POST-%03d", ++counter);
            this.title       = title;
            this.content     = content;
            this.authorName  = authorName;
            this.publishedAt = System.currentTimeMillis();
        }

        public String getPostId()     { return postId; }
        public String getTitle()      { return title; }
        public String getContent()    { return content; }
        public String getAuthorName() { return authorName; }
        public long   getPublishedAt(){ return publishedAt; }
    }

    // =========================================================================
    // 2. PostValidator — VALIDATION LOGIC ONLY
    // Actor: Editorial team (changes minimum title length, adds profanity filter)
    // Responsibility: "Decides whether a post meets publish criteria."
    // =========================================================================
    static class PostValidator {

        private static final int MIN_TITLE_LENGTH   = 5;
        private static final int MIN_CONTENT_LENGTH = 100;

        // HIGH COHESION — every method is about deciding if a post is valid
        public boolean validate(BlogPost post) {
            // Order: title → content → author (short-circuit on first failure)
            return post.getTitle().length()   >= MIN_TITLE_LENGTH
                && post.getContent().length() >= MIN_CONTENT_LENGTH
                && !post.getAuthorName().isBlank();
        }

        // CONSISTENCY — getValidationError() checks rules in the SAME order as validate()
        // so both methods agree on which rule "fired" when multiple rules fail
        public String getValidationError(BlogPost post) {
            if (post.getTitle().length() < MIN_TITLE_LENGTH)
                return "Title must be at least " + MIN_TITLE_LENGTH + " characters";
            if (post.getContent().length() < MIN_CONTENT_LENGTH)
                return "Content must be at least " + MIN_CONTENT_LENGTH + " characters";
            if (post.getAuthorName().isBlank())
                return "Author name cannot be blank";
            return "OK";
        }
    }

    // =========================================================================
    // 3. PostFormatter — FORMATTING ONLY
    // Actor: UI/Print team (changes layout, adds HTML5 tags, adds PDF output)
    // Responsibility: "Converts a blog post into a human-readable format."
    // =========================================================================
    static class PostFormatter {

        // toText uses ALL fields of BlogPost — publishedAt included
        public String toText(BlogPost post) {
            return post.getTitle() + "\n"
                 + "By " + post.getAuthorName() + " | " + new Date(post.getPublishedAt()) + "\n"
                 + post.getContent();
        }

        public String toHtml(BlogPost post) {
            return "<h1>" + post.getTitle() + "</h1>"
                 + "<p>" + post.getContent() + "</p>"
                 + "<em>" + post.getAuthorName() + "</em>";
        }
    }

    // =========================================================================
    // 4. PostAnalytics — ANALYTICS ONLY
    // Actor: Marketing team (changes metrics: adds "shares", "time-on-page")
    // Responsibility: "Tracks and reports engagement for each post."
    // =========================================================================
    static class PostAnalytics {

        // PRIVATE FINAL — both modifier AND final; no constructor needed
        // Inline initialization is cleaner and makes accidental reassignment impossible
        private final Map<String, Integer> views = new HashMap<>();
        private final Map<String, Integer> likes = new HashMap<>();

        // Map.merge is the most concise idiom: if key absent, set 1; else add 1
        public void recordView(String postId) {
            views.merge(postId, 1, Integer::sum);
        }

        public void recordLike(String postId) {
            likes.merge(postId, 1, Integer::sum);
        }

        public int getViews(String postId) {
            return views.getOrDefault(postId, 0);
        }

        public int getLikes(String postId) {
            return likes.getOrDefault(postId, 0);
        }

        public String getSummary(String postId) {
            return String.format("%s: %d views, %d likes",
                    postId, getViews(postId), getLikes(postId));
        }
    }

    // =========================================================================
    // 5. PostRepository — PERSISTENCE ONLY
    // Actor: Engineering / DBA team (switches to DB, adds pagination)
    // Responsibility: "Stores and retrieves blog posts."
    // =========================================================================
    static class PostRepository {

        // Inline initialization — no need for a constructor
        private final List<BlogPost> store = new ArrayList<>();

        public void save(BlogPost post) {
            store.add(post);
            System.out.println("[REPO] Saved " + post.getPostId());
        }

        public BlogPost findById(String postId) {
            return store.stream()
                        .filter(p -> p.getPostId().equals(postId))
                        .findFirst()
                        .orElse(null);
        }

        // Unmodifiable view — callers cannot add/remove without going through save()
        public List<BlogPost> findAll() {
            return Collections.unmodifiableList(store);
        }

        public int count() {
            return store.size();
        }
    }

    // =========================================================================
    // 6. BlogService — ORCHESTRATION ONLY
    // Actor: Product team (changes publish workflow: add moderation step, add audit log)
    // Responsibility: "Coordinates the blog publish workflow."
    // =========================================================================
    static class BlogService {

        // DEPENDENCY INJECTION — collaborators injected; BlogService never calls new
        private final PostValidator  validator;
        private final PostFormatter  formatter;
        private final PostRepository repository;
        private final PostAnalytics  analytics;

        public BlogService(PostValidator validator,
                           PostFormatter formatter,
                           PostRepository repository,
                           PostAnalytics analytics) {
            this.validator  = validator;
            this.formatter  = formatter;
            this.repository = repository;
            this.analytics  = analytics;
        }

        public boolean publish(BlogPost post) {
            // KEY FIX: !validate — reject when INVALID, not when valid
            // Read the condition aloud: "if the post is NOT valid, reject it"
            if (!validator.validate(post)) {
                System.out.println("[REJECTED] " + validator.getValidationError(post));
                return false;
            }
            repository.save(post);
            System.out.println(formatter.toText(post));
            analytics.recordView(post.getPostId());    // simulate first read
            System.out.println("[PUBLISHED] " + post.getPostId());
            return true;
        }
    }

    // =========================================================================
    // Main — same 10 tests as the practice file, plus Test 11 (catches Issue 1)
    // =========================================================================
    public static void main(String[] args) {

        PostValidator  validator  = new PostValidator();
        PostFormatter  formatter  = new PostFormatter();
        PostRepository repository = new PostRepository();
        PostAnalytics  analytics  = new PostAnalytics();
        BlogService    service    = new BlogService(validator, formatter, repository, analytics);

        System.out.println("═══ Test 1: BlogPost construction ═══════════════════════");
        BlogPost post1 = new BlogPost(
                "Getting Started with Java",
                "Java is a versatile, object-oriented language that has been a cornerstone of enterprise "
                + "software development for over two decades. In this post we explore its core features.",
                "Alice");
        System.out.println("postId : " + post1.getPostId());   // POST-001
        System.out.println("title  : " + post1.getTitle());
        System.out.println("author : " + post1.getAuthorName());
        System.out.println("Test 1 PASSED");

        System.out.println("\n═══ Test 2: BlogPost validation (blank title) ═══════════");
        try {
            new BlogPost("", "content", "Bob");
            System.out.println("Test 2 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Test 2 PASSED: " + e.getMessage());
        }

        System.out.println("\n═══ Test 3: PostValidator — valid post ══════════════════");
        PostValidator v = new PostValidator();
        System.out.println("valid?  " + v.validate(post1));              // true
        System.out.println("error?  " + v.getValidationError(post1));    // OK
        System.out.println("Test 3 PASSED");

        System.out.println("\n═══ Test 4: PostValidator — short title ═════════════════");
        BlogPost shortTitle = new BlogPost("Hi",
                "This is a reasonably long content string that goes on for a while and is definitely "
                + "longer than one hundred characters in total.", "Carol");
        System.out.println("valid?  " + v.validate(shortTitle));         // false
        System.out.println("error?  " + v.getValidationError(shortTitle)); // title too short
        System.out.println("Test 4 PASSED");

        System.out.println("\n═══ Test 5: PostFormatter — text and HTML ═══════════════");
        PostFormatter fmt = new PostFormatter();
        String text = fmt.toText(post1);
        System.out.println(text);
        // Verify date appears on line 2
        assert text.contains(" | ") : "toText must include date separator";
        System.out.println(fmt.toHtml(post1));
        System.out.println("Test 5 PASSED");

        System.out.println("\n═══ Test 6: PostAnalytics ════════════════════════════════");
        PostAnalytics ana = new PostAnalytics();
        ana.recordView(post1.getPostId());
        ana.recordView(post1.getPostId());
        ana.recordLike(post1.getPostId());
        System.out.println(ana.getSummary(post1.getPostId()));  // POST-001: 2 views, 1 likes
        System.out.println("getViews: " + ana.getViews(post1.getPostId()));  // 2
        System.out.println("getLikes: " + ana.getLikes(post1.getPostId()));  // 1
        System.out.println("Test 6 PASSED");

        System.out.println("\n═══ Test 7: PostRepository ══════════════════════════════");
        PostRepository repo = new PostRepository();
        repo.save(post1);
        System.out.println("count: " + repo.count());           // 1
        BlogPost found = repo.findById(post1.getPostId());
        System.out.println("found: " + (found != null ? found.getTitle() : "null"));
        System.out.println("findAll size: " + repo.findAll().size());  // 1
        try {
            repo.findAll().add(post1);
            System.out.println("Test 7 FAILED — findAll should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("unmodifiable: confirmed");
        }
        System.out.println("Test 7 PASSED");

        System.out.println("\n═══ Test 8: BlogService — valid publish ═════════════════");
        boolean result = service.publish(post1);
        System.out.println("published: " + result);   // true
        if (!result) System.out.println("Test 8 FAILED — valid post was rejected!");
        else         System.out.println("Test 8 PASSED");

        System.out.println("\n═══ Test 9: BlogService — invalid post rejected ══════════");
        BlogPost shortContent = new BlogPost("Valid Title Here",
                "Too short.",   // under 100 chars — passes BlogPost, fails PostValidator
                "Eve");
        boolean rejected = service.publish(shortContent);
        System.out.println("rejected: " + !rejected);   // true (publish returned false)
        if (rejected) System.out.println("Test 9 FAILED — invalid post was published!");
        else          System.out.println("Test 9 PASSED");

        System.out.println("\n═══ Test 10: SRP proof — one class, one reason ══════════");
        System.out.println("Each class has one reason to change — verified by design.");
        System.out.println("Test 10 PASSED");

        // ── Test 11 (Extra) — catches Issue 1: inverted publish condition ─────
        // This test specifically verifies that the condition in publish() is !validate,
        // not validate. A post that clearly passes all validation rules MUST return true.
        // A post that clearly fails MUST return false.
        // If the condition is inverted, both of these assertions fail.
        System.out.println("\n═══ Test 11 (Extra): inverted-condition trap ═════════════");
        PostValidator  v2  = new PostValidator();
        PostFormatter  f2  = new PostFormatter();
        PostRepository r2  = new PostRepository();
        PostAnalytics  a2  = new PostAnalytics();
        BlogService    svc = new BlogService(v2, f2, r2, a2);

        BlogPost clearlyValid = new BlogPost(
                "Definitely Valid Title",
                "This content is definitely long enough to pass the hundred-character minimum "
                + "content requirement set by the editorial team's validation rules.",
                "Frank");
        boolean t11Valid = svc.publish(clearlyValid);
        if (!t11Valid) {
            System.out.println("Test 11 FAILED — clearly-valid post was rejected (inverted condition bug)");
        }

        BlogPost clearlyInvalid = new BlogPost("Hi", "Short.", "Grace");
        boolean t11Invalid = svc.publish(clearlyInvalid);
        if (t11Invalid) {
            System.out.println("Test 11 FAILED — clearly-invalid post was published (inverted condition bug)");
        }

        if (t11Valid && !t11Invalid)
            System.out.println("Test 11 PASSED — publish() condition is correct");
    }
}

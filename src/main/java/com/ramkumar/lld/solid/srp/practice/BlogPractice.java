package com.ramkumar.lld.solid.srp.practice;

import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PRACTICE — Single Responsibility Principle (SRP)
 * Phase 2, Topic 1 | Scenario B: Blog Publishing Platform
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PROBLEM STATEMENT
 * ─────────────────
 * A startup built a blog platform with a single god class, BlogManagerGod, that
 * handles everything. Your job is to refactor it into focused, SRP-compliant
 * classes — each with exactly one reason to change.
 *
 * ─── CURRENT VIOLATION (shown below) ────────────────────────────────────────
 * BlogManagerGod does too much:
 *   • stores post data               ← Reason 1: Content team
 *   • validates post content         ← Reason 2: Editorial team
 *   • formats output (text / HTML)   ← Reason 3: UI / Print team
 *   • tracks analytics (views, likes)← Reason 4: Marketing team
 *   • persists posts (in-memory)     ← Reason 5: Engineering / DBA team
 *   • orchestrates publish workflow  ← Reason 6: Product team
 *
 * ─── YOUR TASK ──────────────────────────────────────────────────────────────
 * Create these six SRP-compliant classes:
 *
 *  1. BlogPost         — data holder (fields + getters; no logic)
 *  2. PostValidator    — validates whether a post is ready to publish
 *  3. PostFormatter    — formats a post as plain text or HTML
 *  4. PostAnalytics    — tracks and reports views and likes
 *  5. PostRepository   — in-memory store: save, findById, findAll
 *  6. BlogService      — orchestrates the publish workflow
 *
 * ─── FIELD REQUIREMENTS ─────────────────────────────────────────────────────
 * BlogPost (immutable data — all fields final):
 *   • postId        : String  — auto-generated "POST-001" format (private static counter)
 *   • title         : String  — provided at construction, cannot be blank
 *   • content       : String  — provided at construction, cannot be blank
 *   • authorName    : String  — provided at construction, cannot be blank
 *   • publishedAt   : long    — set to System.currentTimeMillis() at construction
 *
 * PostValidator (no fields — pure logic):
 *   • validate(BlogPost post) : boolean
 *       Rules: title ≥ 5 chars, content ≥ 100 chars, author ≠ blank
 *   • getValidationError(BlogPost post) : String
 *       Returns first violated rule as a human-readable message, or "OK" if valid
 *
 * PostFormatter (no fields):
 *   • toText(BlogPost post) : String
 *       Plain text: title on first line, author and date on second, then content
 *   • toHtml(BlogPost post) : String
 *       Wraps title in <h1>, content in <p>, author in <em>
 *
 * PostAnalytics (state: views and likes maps):
 *   • recordView(String postId) : void  — increments view count for this post
 *   • recordLike(String postId) : void  — increments like count for this post
 *   • getViews(String postId)  : int   — returns view count (0 if never recorded)
 *   • getLikes(String postId)  : int   — returns like count (0 if never recorded)
 *   • getSummary(String postId): String — e.g. "POST-001: 5 views, 2 likes"
 *
 * PostRepository (state: internal list of posts):
 *   • save(BlogPost post) : void     — adds post to internal store
 *   • findById(String postId) : BlogPost  — returns matching post or null
 *   • findAll() : List<BlogPost>     — returns unmodifiable list
 *   • count() : int                  — returns total number of stored posts
 *
 * BlogService (orchestrator — no state beyond collaborators):
 *   • Constructor: takes PostValidator, PostFormatter, PostRepository, PostAnalytics
 *   • publish(BlogPost post) : boolean
 *       1. validate — if invalid, print "[REJECTED] <error>" and return false
 *       2. save to repository
 *       3. print the text-formatted post to System.out
 *       4. record one view (simulates first read after publish)
 *       5. print "[PUBLISHED] <postId>"
 *       6. return true
 *
 * ─── DESIGN CONSTRAINTS ─────────────────────────────────────────────────────
 *  • BlogPost constructor must validate all three fields (null/blank) and throw
 *    IllegalArgumentException with a descriptive message if any fails.
 *  • PostRepository.findAll() must return an unmodifiable list.
 *  • BlogService must NOT instantiate its collaborators internally — use the constructor.
 *  • No instanceof chains anywhere.
 *  • The private static counter in BlogPost must be private.
 *  • PostAnalytics must use a Map internally (HashMap is fine).
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class BlogPractice {

    // =========================================================================
    // GOD CLASS — read this to understand what you are refactoring AWAY from.
    // DO NOT ADD CODE TO THIS CLASS. DO NOT CALL THIS CLASS FROM MAIN.
    // =========================================================================

    @SuppressWarnings("unused")
    static class BlogManagerGod {
        // Reason 1: data
        private String postId, title, content, authorName;
        private long   publishedAt;

        // Reason 2: validation
        public boolean isValid() {
            return title != null && title.length() >= 5
                    && content != null && content.length() >= 100
                    && authorName != null && !authorName.isBlank();
        }

        // Reason 3: formatting
        public String toText() {
            return title + "\nBy " + authorName + "\n" + content;
        }
        public String toHtml() {
            return "<h1>" + title + "</h1><p>" + content + "</p><em>" + authorName + "</em>";
        }

        // Reason 4: analytics
        private int views = 0, likes = 0;
        public void recordView() { views++; }
        public void recordLike() { likes++; }
        public String getStats() { return postId + ": " + views + " views, " + likes + " likes"; }

        // Reason 5: persistence
        private static final java.util.List<BlogManagerGod> store = new java.util.ArrayList<>();
        public void save() { store.add(this); System.out.println("[DB] Saved " + postId); }

        // Reason 6: orchestration
        public boolean publish() {
            if (!isValid()) { System.out.println("[REJECTED]"); return false; }
            save();
            System.out.println(toText());
            recordView();
            System.out.println("[PUBLISHED] " + postId);
            return true;
        }
    }

    // =========================================================================
    // ── TODO 1: BlogPost ─────────────────────────────────────────────────────
    // Data holder. All fields private and final.
    // Auto-generates postId using a private static counter: "POST-001" format.
    // Constructor: BlogPost(String title, String content, String authorName)
    //   • Validate each field — throw IllegalArgumentException if null or blank.
    //   • Set publishedAt = System.currentTimeMillis()
    // Expose only getters. No business logic here.
    // =========================================================================

    // TODO 1a: declare private static int counter = 0;
    // TODO 1b: declare all five private final fields
    // TODO 1c: write the constructor with validation + auto-generated postId
    // TODO 1d: write getters for all five fields
    static class BlogPost {
        private final String postId, title, content, authorName;
        private final long publishedAt;
        private static int counter = 0;

        public BlogPost(String title, String content, String authorName){
            if(title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title Cannot be Blank!!");
            }
            if(content == null || content.isBlank()){
                throw new IllegalArgumentException("Content Cannot be Blank!!");
            }
            if(authorName == null || authorName.isBlank()){
                throw new IllegalArgumentException("Author Name Cannot be Blank!!");
            }
            ++counter;
            this.postId = String.format("POST-%03d", counter);
            this.title = title;
            this.content = content;
            this.authorName = authorName;
            this.publishedAt = System.currentTimeMillis();
        }

        public String getPostId() { return postId; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getAuthorName() { return authorName; }
        public long getPublishedAt() { return publishedAt; }
    }


    // =========================================================================
    // ── TODO 2: PostValidator ─────────────────────────────────────────────────
    // Pure logic — no fields, no constructor needed (default is fine).
    // validate(BlogPost post) : boolean
    //   Rules: title.length() >= 5, content.length() >= 100, authorName not blank
    // getValidationError(BlogPost post) : String
    //   Returns first violated rule as message, or "OK" if all pass.
    // =========================================================================
    // TODO 2a: validate(BlogPost) — returns true only if ALL three rules pass
    // TODO 2b: getValidationError(BlogPost) — first failing rule message or "OK"

    static class PostValidator {
        public boolean validate(BlogPost post){
            return (post.getTitle().length() >= 5 && post.getContent().length() >= 100  && !post.getAuthorName().isBlank());
        }

        public String getValidationError(BlogPost post){
            if(post.getAuthorName().isBlank()) {
                return "Post AuthorName cannot be Blank";
            }
            if(post.getTitle().length() < 5) {
                return "Post Title should contain at least 5 characters";
            }
            if(post.getContent().length() < 100) {
                return "Post Content should contain at least 100 characters";
            }
            return "OK";
        }
    }

    // =========================================================================
    // ── TODO 3: PostFormatter ────────────────────────────────────────────────
    // No fields. Formats a BlogPost as text or HTML.
    // toText(BlogPost) — title, then "By <author>" on second line, then content
    // toHtml(BlogPost) — <h1>title</h1><p>content</p><em>author</em>
    // =========================================================================

    // TODO 3a: toText(BlogPost post) : String
    // TODO 3b: toHtml(BlogPost post) : String

    static class PostFormatter {
        public String toText(BlogPost post) {
            return post.getTitle() + "\n" + "By " + post.getAuthorName() + "\n" + post.getContent();
        }

        public String toHtml(BlogPost post) {
            return "<h1>" + post.getTitle()
                    + "</h1><p>" + post.getContent()
                    + "</p><em>" + post.getAuthorName() + "</em>";
        }
    }

    // =========================================================================
    // ── TODO 4: PostAnalytics ─────────────────────────────────────────────────
    // Tracks views and likes PER postId using two Maps (use HashMap).
    // recordView(String postId) — increments view count
    // recordLike(String postId) — increments like count
    // getViews(String postId)   — returns count (0 if never recorded)
    // getLikes(String postId)   — returns count (0 if never recorded)
    // getSummary(String postId) — "POST-001: 5 views, 2 likes"
    // =========================================================================

    // TODO 4a: declare two private Map<String, Integer> fields (views, likes)
    // TODO 4b: recordView — Map.merge or getOrDefault + put
    // TODO 4c: recordLike
    // TODO 4d: getViews   — Map.getOrDefault(postId, 0)
    // TODO 4e: getLikes
    // TODO 4f: getSummary — formatted string
    static class PostAnalytics {
        private final Map<String, Integer> views;
        private final Map<String, Integer> likes;

        public PostAnalytics(){
            this.views = new HashMap<>();
            this.likes = new HashMap<>();
        }

        public void recordView(String postId) {
            int currentViews = views.getOrDefault(postId, 0);
            views.put(postId, currentViews + 1);
        }

        public void recordLike(String postId) {
            int currentLikes = likes.getOrDefault(postId, 0);
            likes.put(postId, currentLikes + 1);
        }

        public int getViews(String postId) {
            return views.getOrDefault(postId, 0);
        }

        public int getLikes(String postId) {
            return likes.getOrDefault(postId, 0);
        }

        public String getSummary(String postId) {
            return String.format("%s: %d views, %d likes", postId,
                                        views.getOrDefault(postId, 0),
                                        likes.getOrDefault(postId, 0));
        }
    }

    // =========================================================================
    // ── TODO 5: PostRepository ───────────────────────────────────────────────
    // In-memory store using a private List<BlogPost>.
    // save(BlogPost)                — adds to list, prints "[REPO] Saved <postId>"
    // findById(String postId)       — stream filter, returns null if not found
    // findAll()                     — returns Collections.unmodifiableList(...)
    // count()                       — returns list size
    // =========================================================================

    // TODO 5a: declare private List<BlogPost> store = new ArrayList<>()
    // TODO 5b: save(BlogPost) — add + print
    // TODO 5c: findById(String) — stream filter
    // TODO 5d: findAll() — unmodifiable list
    // TODO 5e: count()

    static class PostRepository {
        private final List<BlogPost> postList;

        public PostRepository(){
            this.postList = new ArrayList<>();
        }

        public void save(BlogPost post){
            postList.add(post);
            System.out.println("[REPO] Saved " + post.getPostId());
        }

        public BlogPost findById(String postId){
            return postList.stream()
                    .filter(post -> post.getPostId().equals(postId))
                    .findFirst()
                    .orElse(null);
        }
        public List<BlogPost> findAll() {
            return Collections.unmodifiableList(postList);
        }

        public int count() {
            return postList.size();
        }
    }

    // =========================================================================
    // ── TODO 6: BlogService ──────────────────────────────────────────────────
    // Orchestrator. Holds references to all four collaborators.
    // Constructor: BlogService(PostValidator, PostFormatter, PostRepository, PostAnalytics)
    // publish(BlogPost post) : boolean
    //   1. validate — if invalid, print "[REJECTED] <getValidationError()>" → return false
    //   2. save to repository
    //   3. System.out.println(formatter.toText(post))
    //   4. analytics.recordView(post.getPostId())
    //   5. System.out.println("[PUBLISHED] " + post.getPostId())
    //   6. return true
    // =========================================================================

    // TODO 6a: declare four private final fields for the collaborators
    // TODO 6b: constructor accepting all four collaborators
    // TODO 6c: publish(BlogPost) implementing the 6-step workflow
    static class BlogService {
        private final PostValidator postValidator;
        private final PostFormatter postFormatter;
        private final PostRepository postRepository;
        private final PostAnalytics postAnalytics;

        public BlogService(PostValidator postValidator,
                           PostFormatter postFormatter,
                           PostRepository postRepository,
                           PostAnalytics postAnalytics) {
            this.postValidator = postValidator;
            this.postFormatter = postFormatter;
            this.postRepository = postRepository;
            this.postAnalytics = postAnalytics;
        }

        public boolean publish(BlogPost post) {
            if(postValidator.validate(post)) {
                System.out.println("[REJECTED] " + postValidator.getValidationError(post));
                return false;
            }
            postRepository.save(post);
            System.out.println(postFormatter.toText(post));
            postAnalytics.recordView(post.getPostId());
            System.out.println("[PUBLISHED] " + post.getPostId());
            return true;
        }
    }

    // =========================================================================
    // DO NOT MODIFY — pre-written tests; fill in the TODOs above to make them pass
    // =========================================================================

    public static void main(String[] args) {

        // ── Shared collaborators ──────────────────────────────────────────────
        // Uncomment and use once your classes are implemented:
        //
         PostValidator  validator  = new PostValidator();
         PostFormatter  formatter  = new PostFormatter();
         PostRepository repository = new PostRepository();
         PostAnalytics  analytics  = new PostAnalytics();
         BlogService    service    = new BlogService(validator, formatter, repository, analytics);

        System.out.println("═══ Test 1: BlogPost construction ═══════════════════════");
         BlogPost post1 = new BlogPost(
                 "Getting Started with Java",
                 "Java is a versatile, object-oriented language that has been a cornerstone of enterprise " +
                 "software development for over two decades. In this post we explore its core features.",
                 "Alice");
         System.out.println("postId   : " + post1.getPostId());          // POST-001
         System.out.println("title    : " + post1.getTitle());
         System.out.println("author   : " + post1.getAuthorName());
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
         System.out.println("valid?  " + v.validate(post1));             // true
         System.out.println("error?  " + v.getValidationError(post1));   // OK
         System.out.println("Test 3 PASSED");

        System.out.println("\n═══ Test 4: PostValidator — short title ═════════════════");
         BlogPost shortTitle = new BlogPost("Hi",
                 "This is a reasonably long content string that goes on for a while and is definitely " +
                 "longer than one hundred characters in total.", "Carol");
         System.out.println("valid?  " + v.validate(shortTitle));        // false
         System.out.println("error?  " + v.getValidationError(shortTitle)); // title too short
         System.out.println("Test 4 PASSED");

        System.out.println("\n═══ Test 5: PostFormatter — text and HTML ═══════════════");
         PostFormatter fmt = new PostFormatter();
         System.out.println(fmt.toText(post1));
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
         System.out.println("found: " + (found != null ? found.getTitle() : "null")); // title
         System.out.println("findAll size: " + repo.findAll().size());  // 1
         System.out.println("Test 7 PASSED");

        System.out.println("\n═══ Test 8: BlogService — valid publish ═════════════════");
         boolean result = service.publish(post1);
         System.out.println("published: " + result);             // true
         System.out.println("Test 8 PASSED");

        System.out.println("\n═══ Test 9: BlogService — invalid post rejected ══════════");
         BlogPost tooShort = new BlogPost("Hi", "Short.", "Dave");  // wait — constructor validates!
         // correct approach: build an almost-valid post that passes BlogPost but fails PostValidator
         BlogPost shortContent = new BlogPost("Valid Title Here",
                 "Too short.",   // well under 100 chars — but constructor doesn't check this
                 "Eve");
         boolean rejected = service.publish(shortContent);
         System.out.println("rejected: " + !rejected);           // true (publish returned false)
         System.out.println("Test 9 PASSED");

        System.out.println("\n═══ Test 10: SRP proof — one class, one reason ══════════");
        // Verify that changing tax rules (here: word count rule) ONLY touches PostValidator
        // Verify that changing output format ONLY touches PostFormatter
        // Verify that changing storage ONLY touches PostRepository
         System.out.println("Each class has one reason to change — verified by design.");
         System.out.println("Test 10 PASSED");

        System.out.println("\n[Uncomment the test code above after implementing your classes]");
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * HINTS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle) — Think about which ACTOR would demand each change.
 *   Ask: "If the editorial team changes the minimum word count, which class breaks?"
 *   It should be exactly one class. Apply that question to every field and method.
 *   For tracking views/likes per post, think about a data structure that maps
 *   a post ID to a count — and what happens when the key doesn't exist yet.
 *
 * HINT 2 (Direct) — Implementation pointers:
 *   • BlogPost.postId: use a private static int counter and String.format("POST-%03d", ++counter)
 *   • PostValidator: use String.length() >= threshold, not word count
 *   • PostAnalytics: Map<String, Integer> — use map.merge(key, 1, Integer::sum)
 *     or map.put(key, map.getOrDefault(key, 0) + 1)
 *   • PostRepository.findAll(): wrap with Collections.unmodifiableList(store)
 *   • BlogService: receive all collaborators in constructor; do not call new inside
 *
 * HINT 3 (Near-solution) — Class skeletons without method bodies:
 *
 *   static class BlogPost {
 *       private static int counter = 0;
 *       private final String postId, title, content, authorName;
 *       private final long publishedAt;
 *       BlogPost(String title, String content, String authorName) { ... }
 *       // getters only
 *   }
 *
 *   static class PostValidator {
 *       boolean validate(BlogPost post) { ... }
 *       String  getValidationError(BlogPost post) { ... }
 *   }
 *
 *   static class PostFormatter {
 *       String toText(BlogPost post) { ... }
 *       String toHtml(BlogPost post) { ... }
 *   }
 *
 *   static class PostAnalytics {
 *       private final Map<String, Integer> views = new HashMap<>();
 *       private final Map<String, Integer> likes = new HashMap<>();
 *       void   recordView(String postId) { ... }
 *       void   recordLike(String postId) { ... }
 *       int    getViews(String postId)   { ... }
 *       int    getLikes(String postId)   { ... }
 *       String getSummary(String postId) { ... }
 *   }
 *
 *   static class PostRepository {
 *       private final List<BlogPost> store = new ArrayList<>();
 *       void           save(BlogPost post)        { ... }
 *       BlogPost       findById(String postId)    { ... }
 *       List<BlogPost> findAll()                  { ... }
 *       int            count()                    { ... }
 *   }
 *
 *   static class BlogService {
 *       private final PostValidator validator;
 *       private final PostFormatter formatter;
 *       private final PostRepository repository;
 *       private final PostAnalytics  analytics;
 *       BlogService(PostValidator v, PostFormatter f, PostRepository r, PostAnalytics a) { ... }
 *       boolean publish(BlogPost post) { ... }
 *   }
 */

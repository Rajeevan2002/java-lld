# Review — Single Responsibility Principle (SRP)
Phase 2, Topic 1 | Scenario B: Blog Publishing Platform

---

## What You Got Right

- **BlogPost data holder**: All five fields correctly `private final`, proper null/blank validation in the constructor, `String.format("POST-%03d", ++counter)` auto-ID, `publishedAt = System.currentTimeMillis()`, five clean getters — exactly what an immutable data class should look like.
- **PostValidator as pure logic**: No fields, no state — purely behaviour. Both `validate()` and `getValidationError()` are correctly separated (data class doesn't know its own rules, validator does). This is the central SRP lesson and the class boundaries are correct.
- **PostFormatter** — correct `toText()` and `toHtml()` structure. No state, no coupling to persistence or validation.
- **PostRepository** — correct `Collections.unmodifiableList(postList)` in `findAll()`, stream + filter for `findById()`, `[REPO] Saved` print in `save()`. Encapsulation of the internal list done right.
- **BlogService constructor DI** — all four collaborators injected, no `new` inside the class body. This is the correct dependency injection pattern.
- **PostAnalytics getSummary / getViews / getLikes** — `getOrDefault(postId, 0)` is the clean idiom; `String.format` for the summary is the right choice.
- **Six-class decomposition** — all six actors correctly identified, each given one class. The mental model is sound.

---

## Issues Found

### Issue 1 — Bug: `BlogService.publish()` condition is inverted

**Severity**: Bug (Critical)

`validate()` returns `true` when the post is valid. The `if` block rejects on `true` — so valid posts are rejected and invalid posts are published.

**Your code:**
```java
if(postValidator.validate(post)) {   // ← fires when post IS valid
    System.out.println("[REJECTED] " + postValidator.getValidationError(post));
    return false;
}
```

**Fix:**
```java
if (!postValidator.validate(post)) {  // fire when post is NOT valid
    System.out.println("[REJECTED] " + postValidator.getValidationError(post));
    return false;
}
```

**Why it matters**: Test 8 silently returns `false` — a valid post gets rejected — but the test still prints "Test 8 PASSED" because it only checks the boolean, not whether `[PUBLISHED]` appeared. The bug is invisible in the current test run, which makes it more dangerous.

---

### Issue 2 — Bug: `PostAnalytics` maps are not `private` or `final`

**Severity**: Bug (Encapsulation)

Both maps are package-private (no access modifier) and not `final`. Any class in the same package can reach in and mutate the maps directly.

**Your code:**
```java
Map<String, Integer> views;   // ← package-private, mutable reference
Map<String, Integer> likes;   // ← same
```

**Fix:**
```java
private final Map<String, Integer> views = new HashMap<>();
private final Map<String, Integer> likes = new HashMap<>();
// constructor can be removed — initialization is inline
```

**Why it matters**: A collaborator could do `analytics.views.clear()` and wipe all view counts without going through `recordView()`. Encapsulation exists to prevent exactly this.

---

### Issue 3 — Missing: `toText()` omits the date

**Severity**: Missing Feature

The spec says second line should be "author and date". The implementation only includes the author.

**Your code:**
```java
return post.getTitle() + "\n" + "By " + post.getAuthorName() + "\n" + post.getContent();
```

**Fix:**
```java
return post.getTitle() + "\n"
    + "By " + post.getAuthorName()
    + " | " + new java.util.Date(post.getPublishedAt()) + "\n"
    + post.getContent();
```

**Why it matters**: A date field that exists on the data object but is never displayed is dead state. If a field has no consumer, question whether it belongs there at all.

---

### Issue 4 — Design: `getValidationError()` checks in a different order than `validate()`

**Severity**: Design

`validate()` evaluates title → content → author. `getValidationError()` checks author → title → content. When a post fails multiple rules, the two methods disagree on which rule failed "first".

**Your code:**
```java
// validate() order:              title → content → author
return post.getTitle().length() >= 5 && post.getContent().length() >= 100 && !post.getAuthorName().isBlank();

// getValidationError() order:    author → title → content
if(post.getAuthorName().isBlank())  { return "..."; }
if(post.getTitle().length() < 5)    { return "..."; }
if(post.getContent().length() < 100){ return "..."; }
```

**Fix** — match the order in both methods:
```java
public String getValidationError(BlogPost post) {
    if (post.getTitle().length() < 5)     return "Title must be at least 5 characters";
    if (post.getContent().length() < 100) return "Content must be at least 100 characters";
    if (post.getAuthorName().isBlank())   return "Author name cannot be blank";
    return "OK";
}
```

**Why it matters**: In production, the UI displays `getValidationError()` to the user. If validation rule logic and error message logic disagree on which rule fires, users see misleading errors.

---

### Issue 5 — Minor: `static` field declared after instance fields

**Severity**: Minor (Style)

`counter` is declared after the instance fields. Java convention is static fields first, then instance fields.

**Your code:**
```java
private final String postId, title, content, authorName;
private final long publishedAt;
private static int counter = 0;   // ← static field after instance fields
```

**Fix:**
```java
private static int counter = 0;   // static first
private final String postId, title, content, authorName;
private final long publishedAt;
```

---

## Score Card

| Requirement | Result | Notes |
|---|---|---|
| BlogPost: 5 fields, all `private final` | ✅ | Correct |
| BlogPost: auto-ID `POST-001` format | ✅ | Correct |
| BlogPost: null/blank validation, throws IAE | ✅ | Correct |
| BlogPost: `publishedAt` from `currentTimeMillis()` | ✅ | Correct |
| BlogPost: `counter` is `private` | ✅ | Correct |
| PostValidator: pure logic, no fields | ✅ | Correct |
| PostValidator: `validate()` checks title/content/author | ✅ | Correct |
| PostValidator: `getValidationError()` consistent order | ❌ | Different order than `validate()` |
| PostFormatter: `toText()` includes title + author + date | ⚠️ | Date missing |
| PostFormatter: `toHtml()` correct tags | ✅ | Correct |
| PostAnalytics: views/likes maps `private final` | ❌ | Package-private, not final |
| PostAnalytics: `getOrDefault` for 0-count | ✅ | Correct |
| PostAnalytics: `getSummary()` formatted string | ✅ | Correct |
| PostRepository: `findAll()` unmodifiable | ✅ | Correct |
| PostRepository: `findById()` via stream filter | ✅ | Correct |
| BlogService: DI constructor (no `new` inside) | ✅ | Correct |
| BlogService: `publish()` rejects invalid, publishes valid | ❌ | Logic inverted |
| BlogService: 6-step publish workflow order | ✅ | Order correct (except inverted gate) |
| No `instanceof` chains | ✅ | Correct |

---

## Key Takeaways — Do Not Miss These

**TK-1: Name your condition so it reads like English, then check if you need `!`**
Before writing any boolean condition, say it aloud: "if the post is valid, reject it" — that's wrong. The condition should be `!validate(post)`, i.e., "if the post is NOT valid, reject it". Inverted boolean conditions are one of the most common silent bugs in Java — no exception, no crash, just wrong output.
*Interview relevance*: Inverted booleans are a known code-review red flag; reviewers look for them specifically.

**TK-2: Access modifiers are not optional — every field must be `private`**
`Map<String, Integer> views` with no modifier is package-private. In a real codebase any class in the same package can break your invariants. The habit of always writing `private` before every field must be automatic.
*Interview relevance*: Interviewers scan field declarations for missing `private` as the first encapsulation check.

**TK-3: If you store a value, display it somewhere**
`publishedAt` is stored but `toText()` never uses it. Every field on a data class should be read by at least one collaborator — otherwise it is dead state. If a field has no consumer, question whether it belongs there at all.
*Interview relevance*: "Why is this field here? Where is it used?" is a standard design-review question.

**TK-4: Validation order must be consistent across all related methods**
`validate()` and `getValidationError()` are a pair — they describe the same rules. If they evaluate rules in different orders, you get contradictory behaviour when multiple rules fail simultaneously. Write `getValidationError()` by literally copying the condition order from `validate()` and inverting each check.
*Interview relevance*: Inconsistency between a validator and its error-reporter is a common bug reported in production code reviews.

**TK-5: `private final` fields initialised inline need no constructor**
```java
// Instead of constructor + assignment:
private Map<String, Integer> views;
PostAnalytics() { this.views = new HashMap<>(); }

// Prefer inline initialisation:
private final Map<String, Integer> views = new HashMap<>();
```
Fewer lines, guaranteed initialisation at field declaration time, and `final` prevents accidental reassignment.
*Interview relevance*: This pattern signals fluency with Java field initialisation semantics.

**TK-6: Six classes, six actors — name each actor before naming each class**
When unsure how to split a god class, write down the list of humans/teams who demand changes: "content team, editorial team, UI team, marketing team, DBA, product". Then create one class per team. If you cannot name the actor, that is a sign the class boundary is not SRP.
*Interview relevance*: Interviewers ask "who owns this class?" — being able to name the actor is the proof you understand SRP.

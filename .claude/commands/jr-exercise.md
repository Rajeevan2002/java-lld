Generate a Java Readiness practice exercise for the following topic: $ARGUMENTS

This is a Phase 4 — Java Readiness for LLD exercise. The focus is on Java API mastery,
correct collection/idiom selection, and idiomatic Java — not OOP/design patterns.

IMPORTANT RULE — Two different scenarios, always:
- The `code/` directory already contains a worked example with Scenario A.
- The `practice/` exercise MUST use a completely different real-world Scenario B.
- The student must not be able to copy-paste from the worked example.
- Choose Scenario B so it tests the same APIs and idioms in a fresh, realistic context.
  Good pairings (Scenario A → Scenario B):
    E-commerce inventory (HashMap)  → Library catalogue / Flight seat map / Hospital records
    Task scheduler (PriorityQueue)  → Ride dispatch / Print queue / Customer support tickets
    LRU cache (LinkedHashMap)       → Browser history / Recently played tracks / Session store
    Word frequency (TreeMap)        → Leaderboard / Grade report / Stock price history

---

Follow this exact structure in your response:

## Context
State the phase (4) and topic from the javareadiness/README.md roadmap.
State which Java API(s)/collection(s)/idioms this exercise specifically practices.
State Scenario A (what the code/ worked example covers) — one line.
State Scenario B (what this practice exercise covers) — one line.
Explain in one sentence why Scenario B tests the same APIs differently.

## Scenario B — Problem Statement
Write a realistic, interview-style problem statement for Scenario B. Include:
- What collection(s) or Java API(s) must be used and why (what property makes it the right choice)
- A numbered list of method requirements with explicit validation rules
- Explicit output/return format requirements where relevant
- Design constraints (e.g., "must use TreeMap for sorted iteration", "must use Stream API for filtering", "must use Map.merge for aggregation")

## Step 1 — Create the docs/ README
If a docs/README.md does not already exist for this topic, create one covering:
- What each collection/API is for — internal mechanism, time complexity, key methods
- When to use this collection/API in LLD scenarios — a decision guide
- Code snippets for each key operation
- Comparison table (e.g., HashMap vs TreeMap vs LinkedHashMap)
- 5–7 interview Q&A focused on the API itself
- Common mistakes and misconceptions section

## Step 2 — Create the code/ worked example (Scenario A)
Create a fully commented Java file in `code/` for Scenario A.
Package: `com.ramkumar.lld.javareadiness.<topic>.code`
- Comment every non-obvious API choice (why this collection, not another)
- Label which lines demonstrate which concept (e.g., insertion-order guarantee, O(log n) sort, merge semantics)
- Include a main() that prints labelled output showing the behaviour clearly

## Step 3 — Create the practice/ skeleton (Scenario B)
Create the skeleton Java file in `practice/` for Scenario B.
Package: `com.ramkumar.lld.javareadiness.<topic>.practice`
- Javadoc block at the top with the full problem statement (file is self-contained)
- `// ── TODO N: <what to implement>` markers for every field and method body
- Comment the required collection type and why (leave the implementation blank)
- A main() with at least 8 pre-written test cases — student fills TODOs to make them pass
- DO NOT MODIFY comment on main()
- 3-level hints block at the bottom:
    HINT 1 (Gentle)        — points to the right concept without naming the specific API
    HINT 2 (Direct)        — names the specific collection or method to use
    HINT 3 (Near-solution) — shows field declarations and method signatures with the key API call revealed

## Step 4 — Create results/.gitkeep
Ensure `results/.gitkeep` exists as an empty placeholder.

## What This Exercise Tests
Table: Java API / concept → exactly where in the skeleton it is tested (TODO number).

## Suggested file paths
List all files created.

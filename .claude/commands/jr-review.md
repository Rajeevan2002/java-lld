Review the user's solution for the current Java Readiness practice exercise.

The user has just completed a Phase 4 — Java Readiness practice problem. To perform the review:
1. Read the practice file in the `practice/` directory of the current topic
2. Read the topic's `docs/README.md` for the APIs, collections, and idioms being tested
3. Evaluate the solution against: correct API selection, time complexity, edge case handling, and idiomatic Java usage

Then produce a structured review in this exact format:

---

## What You Got Right
List everything done correctly. Be specific — name the API, method, or idiom used and explain
why it is the right choice here. Do not be vague ("good job on the map") — say which map method,
which collection, why it was the right pick, and what it buys you.

## Issues Found
For each issue:
- **Severity**: Bug / Wrong API / Suboptimal / Missing Validation / Minor
- **What the issue is** — one clear sentence
- **Your code** — show the problematic snippet
- **Fix** — show the corrected version
- **Why it matters** — one sentence on the real-world consequence or the performance/correctness implication

Number the issues in priority order (bugs and wrong API choices first, then suboptimal, then minor).

## Score Card
A markdown table with each requirement from the problem statement as a row, and ✅ / ❌ / ⚠️ as the result.

## Key Takeaways — Do Not Miss These
3 to 7 numbered takeaways. Each must:
- Start with a bold label (e.g., **TK-1: TreeMap when you need sorted keys**)
- State a rule or principle in one sentence
- Give a one-line "why it matters in interviews" note

## Reference Solution
Write the complete, optimal Java solution with:
- Correct package: `com.ramkumar.lld.javareadiness.<topic>.results`
- A class name `<Topic>Reference`
- Inline comments explaining every non-obvious API choice (why this collection, not that one)
- A `main()` that runs the same test cases as the practice file, with one extra test case
  that catches the most common API misuse seen in this exercise

Save the reference solution to:
`src/main/java/com/ramkumar/lld/javareadiness/<topic>/results/<ClassName>Reference.java`

Also save the full review (everything above, excluding the Java file) to:
`src/main/java/com/ramkumar/lld/javareadiness/<topic>/results/REVIEW-<topic-slug>.md`

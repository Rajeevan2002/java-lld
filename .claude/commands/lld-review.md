Review the user's solution for the current LLD practice exercise.

The user has just completed a practice problem. To perform the review:
1. Read the practice file in the `practice/` directory of the current topic
2. Read the output/results the user has shared (if any)
3. Read the topic's `docs/README.md` for the concepts being tested

Then produce a structured review in this exact format:

---

## What You Got Right
List everything done correctly. Be specific — name the construct, pattern, or design decision. Do not be vague ("good job on constructors") — say what specifically was right and why it matters.

## Issues Found
For each issue:
- **Severity**: Bug / Design / Missing Validation / Minor
- **What the issue is** — one clear sentence
- **Your code** — show the problematic snippet
- **Fix** — show the corrected version
- **Why it matters** — one sentence on the real-world consequence

Number the issues in priority order (bugs first, then design, then minor).

## Score Card
A markdown table with each requirement from the problem statement as a row, and ✅ / ❌ / ⚠️ as the result.

## Key Takeaways — Do Not Miss These
3 to 7 numbered takeaways. Each must:
- Start with a bold label (e.g., **TK-1: One master constructor, always**)
- State a rule or principle in one sentence
- Give a one-line "why it matters in interviews" note

## Reference Solution
Write the complete, optimal Java solution with:
- Correct package: `com.ramkumar.lld.<phase>.<topic>.results`
- A class name `<Topic>Reference`
- Inline comments explaining every non-obvious decision
- A `main()` that runs the same test cases as the practice file, with one extra test case that catches the most common mistake made in this exercise

Save the reference solution to:
`src/main/java/com/ramkumar/lld/<phase>/<topic>/results/<ClassName>Reference.java`

Also save the full review (everything above, excluding the Java file) to:
`src/main/java/com/ramkumar/lld/<phase>/<topic>/results/REVIEW-<topic-slug>.md`

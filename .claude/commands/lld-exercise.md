Generate a practice exercise for the following LLD / Java topic: $ARGUMENTS

IMPORTANT RULE — Two different scenarios, always:
- The `code/` directory already contains a worked example with Scenario A.
- The `practice/` exercise MUST use a completely different real-world Scenario B.
- The student must not be able to copy-paste from the worked example.
- Choose Scenario B so it tests the same concepts but in a fresh context.
  Good pairings (Scenario A → Scenario B):
    Employee hierarchy     → Vehicle rental / Media subscription / Insurance policy
    Shape/geometry         → Tax calculator / Shipping carrier / Notification system
    Bank account           → Library book / E-commerce cart / Loyalty rewards
    Animal kingdom         → Electronic device / Food menu / Course catalog

---

Follow this exact structure in your response:

## Context
State the phase and topic from the README.md roadmap.
State Scenario A (what the code/ worked example covers) — one line.
State Scenario B (what this practice exercise will cover) — one line.
Explain in one sentence why Scenario B tests the same concepts differently.

## Scenario B — Problem Statement
Write a realistic, interview-style problem statement for Scenario B. Include:
- A clear description of what the class or system should model
- A numbered list of field requirements per class (mark which are immutable/final)
- A numbered list of constructor requirements (state which should chain and how)
- A numbered list of method requirements with explicit validation rules
- Design constraints (e.g., "no instanceof chains", "list must be unmodifiable", etc.)

## Step 1 — Create the docs/ README
If a docs/README.md does not already exist for this topic, create one covering:
- Core concepts with ASCII diagrams
- Code snippets for each concept
- Comparison tables where relevant
- 5–7 interview Q&A
- Common mistakes section

## Step 2 — Create the code/ worked example (Scenario A)
Create a fully commented Java file in `code/` for Scenario A.
Package: `com.ramkumar.lld.<phase>.<topic>.code`
- Comment every non-obvious decision
- Label which lines demonstrate which concept (Encapsulation, Polymorphism, etc.)
- Include a main() that prints labelled output

## Step 3 — Create the practice/ skeleton (Scenario B)
Create the skeleton Java file in `practice/` for Scenario B.
Package: `com.ramkumar.lld.<phase>.<topic>.practice`
- Javadoc block at the top with the full problem statement (file is self-contained)
- `// ── TODO N: <what to implement>` markers for every field, constructor, and method
- A main() with at least 7 pre-written test cases — student fills TODOs to make them pass
- DO NOT MODIFY comment on main()
- 3-level hints block at the bottom:
    HINT 1 (Gentle)      — points to the right concept without naming the solution
    HINT 2 (Direct)      — names the API / pattern to use
    HINT 3 (Near-solution) — shows the class skeleton without the method bodies

## Step 4 — Create results/.gitkeep
Ensure `results/.gitkeep` exists as an empty placeholder.

## What This Exercise Tests
Table: concept → exactly where in the skeleton it is tested.

## Suggested file paths
List all files created.

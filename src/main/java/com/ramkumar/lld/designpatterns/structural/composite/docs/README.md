# 3.2.5 Composite Pattern

## What Problem Does It Solve?

You have a tree-shaped hierarchy of objects — files inside directories, employees inside
departments, menu items inside menus, UI widgets inside panels. You want to apply the
same operation (compute total size, draw, calculate salary) uniformly without asking
"is this a leaf or a container?"

The naive approach is to check types at every call site:

```java
if (node instanceof File f)       total += f.getSizeBytes();
else if (node instanceof Dir d)   total += d.calculateTotal();  // separate method!
```

This breaks every time you add a new node type and scatters conditional logic everywhere.

The **Composite** pattern defines a **single Component interface** that both leaves and
composites implement. A composite holds a `List<Component>` and delegates to each
child. The client calls the same method regardless of whether it holds a leaf or a
composite.

---

## Core Structure

```
Client ──► Component (interface)
                ▲              ▲
                │              │
              Leaf          Composite
           (no children)   - children: List<Component>  ← stores Components, not Leaves
                           + add(Component c)
                           + operation()  ← delegates to each child's operation()
```

**The two key relationships in Composite:**
1. `Composite implements Component` — is-a Component, substitutable for a Leaf
2. `Composite has-a List<Component>` — stores *interface* references, enabling arbitrary depth

---

## ASCII Example — File System

```
[Dir]  project/ (150 KB)
  [Dir]  src/ (100 KB)
    [File] Main.java   (60 KB)
    [File] Utils.java  (40 KB)
  [Dir]  docs/ (50 KB)
    [File] README.md   (50 KB)
```

`project.getSizeBytes()` → calls `src.getSizeBytes()` + `docs.getSizeBytes()`
`src.getSizeBytes()`     → calls `Main.getSizeBytes()` + `Utils.getSizeBytes()`
All through the same `Component.getSizeBytes()` call.

---

## Code Skeleton

```java
// ── [Component] — the uniform interface ──────────────────────────────────────
interface FileSystemComponent {
    String getName();
    long getSizeBytes();
    void display(String indent);
}

// ── [Leaf] — no children; implements operations directly ─────────────────────
class File implements FileSystemComponent {
    private final String name;
    private final long sizeBytes;

    File(String name, long sizeBytes) { this.name = name; this.sizeBytes = sizeBytes; }

    @Override public String getName()        { return name; }
    @Override public long   getSizeBytes()   { return sizeBytes; }
    @Override public void   display(String indent) {
        System.out.printf("%s[File] %s (%,d bytes)%n", indent, name, sizeBytes);
    }
}

// ── [Composite] — has children; delegates operations ─────────────────────────
class Directory implements FileSystemComponent {
    private final String name;
    private final List<FileSystemComponent> children = new ArrayList<>();  // [Interface type!]

    Directory(String name) { this.name = name; }

    void add(FileSystemComponent c) { children.add(c); }   // [Accepts any Component]

    @Override public String getName() { return name; }

    @Override
    public long getSizeBytes() {
        // [Delegation] ask each child; children handle their own recursion
        long total = 0;
        for (FileSystemComponent child : children) total += child.getSizeBytes();
        return total;
    }

    @Override
    public void display(String indent) {
        System.out.printf("%s[Dir] %s (%,d bytes)%n", indent, name, getSizeBytes());
        for (FileSystemComponent child : children) child.display(indent + "  "); // [Recursion]
    }
}
```

---

## Critical Rules

| Rule | Why it matters |
|---|---|
| `children` is `List<Component>`, not `List<Leaf>` | Allows nesting composites inside composites |
| `add()` accepts `Component`, not `Leaf` | Without this, you cannot add a sub-directory to a directory |
| Never use `instanceof` inside composite operations | Violates OCP — adding a new node type breaks every switch |
| `display()` passes `indent + "  "` to children | Each level adds its own indentation — no global counter needed |
| Composite delegates to children; children handle their own recursion | The pattern is naturally recursive; no loop depth limit |

---

## Comparison: Composite vs Related Patterns

| Dimension | Composite | Decorator | Iterator |
|---|---|---|---|
| **Structure** | Tree (parent ↔ children) | Chain (outer ↔ inner) | Linear sequence |
| **Children count** | 0..N per node | Exactly 1 wrapped object | Elements in collection |
| **Purpose** | Uniform part-whole hierarchy | Add behaviour at runtime | Traverse without exposing internals |
| **Recursion** | Inherent — composite calls children | Inherent — decorator calls wrapped | Provided by iterator |
| **Leaf type** | Separate class that can't have children | Same class wrapped N times | Element in collection |

---

## When to Use (and When Not To)

**Use Composite when:**
- You have a part-whole hierarchy (file systems, org charts, UI trees, expression trees).
- Clients should treat leaf and container objects identically.
- You want to add operations uniformly across an entire tree without `instanceof`.

**Do NOT use Composite when:**
- The hierarchy is flat — a simple `List` is clearer.
- Leaves and composites have fundamentally different operations — forcing a shared interface produces either empty stub methods or meaningless defaults.
- You need to restrict which component types can be children of which composites — Composite's uniform interface gives you no compile-time enforcement of that.

---

## Interview Q&A

**Q1. What is the Composite pattern? State the three participants.**
Composite lets you compose objects into tree structures to represent part-whole
hierarchies, treating leaf and container objects uniformly. The three participants are:
Component (shared interface), Leaf (no children, implements operations directly),
and Composite (holds a `List<Component>`, delegates operations to each child).

**Q2. Why must the `children` list be `List<Component>` and not `List<Leaf>`?**
Because composites need to hold other composites, not just leaves. If the list were
`List<Leaf>`, you could not add sub-directories to a directory — the whole recursive
tree structure would collapse to a flat list of leaves.

**Q3. Why is `instanceof` in a Composite operation a design smell?**
It violates OCP. Every time you add a new node type (e.g., `Symlink`) you must find
every `instanceof` check and add another branch. With the Composite pattern, the new
type simply implements the Component interface; all existing composite operations
delegate to it automatically with no changes.

**Q4. What operation do composites delegate, and why does it "just work" for arbitrary depth?**
They delegate every Component interface method to each child. Because a Composite child
also implements Component, it will in turn delegate to *its* children — and so on
recursively. The recursion terminates at Leaf nodes, which implement the method directly
without any delegation. No manual depth tracking is needed.

**Q5. Where does the Composite pattern appear in real-world Java / frameworks?**
- `javax.swing` widget hierarchy: `JPanel` (Composite) contains `JButton` (Leaf) or
  other `JPanel`s; both are `Component`.
- XML/HTML DOM: an `Element` node (Composite) contains child `Node`s; a `Text` node
  is a Leaf.
- Gradle / Maven build tasks: a composite task delegates to sub-tasks.
- Expression trees in compilers: `BinaryExpression` (Composite with left + right
  children), `Literal` (Leaf).

**Q6. What is the difference between Composite's `add()` method and Decorator's constructor?**
Both accept a Component. Decorator stores exactly one wrapped object and is designed
to stack. Composite stores *zero or more* children in a list, forming a tree — a
Composite has siblings, parents, and multiple children. A Decorator has no siblings
and a single chain below it.

---

## Common Mistakes

1. **Typing `children` as `List<Leaf>` or `List<Concrete>`.**
   This breaks the ability to nest composites. Always type the list as
   `List<Component>` — the interface.

2. **Using `instanceof` inside the composite's operation.**
   If `getSizeBytes()` in `Directory` checks `if (child instanceof File)`, you lose
   the ability to add sub-directories without changing the method. Delegate uniformly.

3. **Forgetting to pass increased indentation to children in `display()`.**
   `child.display(indent + "  ")` — each level adds its own prefix. Without it, the
   entire tree prints at depth 0.

4. **Putting `add()` on the Component interface.**
   Leaf nodes have no children; `add()` on the interface forces `File` to implement
   `add()` with either an `UnsupportedOperationException` or a silent no-op — both
   mislead callers. Put `add()` only on the Composite class.

5. **Implementing `getSalary()` / `getSize()` in Composite with a hard-coded loop
   over only one level.**
   The point is that you do NOT need to know the depth. Call `child.getSize()` in a
   loop — recursion handles the rest automatically.

package com.ramkumar.lld.designpatterns.structural.decorator.code;

// ─────────────────────────────────────────────────────────────────────────────
// Decorator Pattern — Scenario A: Text Transformation Pipeline
//
// Problem: A logging system needs to apply optional transformations to text —
//          trim whitespace, convert to upper-case, prepend a tag, append a
//          timestamp — in any combination and order.
//
//          Subclassing: 4 features → up to 15 subclasses.
//          Decorator:   4 Decorator classes, unlimited runtime combinations.
//
// Participants:
//   TextTransformer        [Component interface]
//   PassThroughTransformer [ConcreteComponent]
//   TextDecorator          [BaseDecorator — abstract]
//   TrimDecorator          [ConcreteDecorator — no extra state]
//   UpperCaseDecorator     [ConcreteDecorator — no extra state]
//   PrefixDecorator        [ConcreteDecorator — has own field: prefix]
//   SuffixDecorator        [ConcreteDecorator — has own field: suffix]
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// [Component] — the interface all objects (concrete and decorated) must share.
//               Every decorator implements this, enabling infinite stacking.
// ─────────────────────────────────────────────────────────────────────────────
interface TextTransformer {
    /** Apply this transformer's logic to text and return the result. */
    String apply(String text);
}

// ─────────────────────────────────────────────────────────────────────────────
// [ConcreteComponent] — the base object being decorated.
//                       Returns text unchanged; the starting point of any chain.
// ─────────────────────────────────────────────────────────────────────────────
class PassThroughTransformer implements TextTransformer {
    @Override
    public String apply(String text) {
        return text; // [BaseCase] no transformation; pure identity function
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [BaseDecorator] — abstract; implements Component AND holds a Component.
//
//   implements TextTransformer   → is substitutable everywhere a component is expected
//   private final TextTransformer wrapped → delegates to the next in the chain
//
//   The default apply() purely delegates — concrete decorators override to add behavior.
// ─────────────────────────────────────────────────────────────────────────────
abstract class TextDecorator implements TextTransformer {        // [implements Component]

    private final TextTransformer wrapped;                       // [has-a Component — never null, never replaced]

    TextDecorator(TextTransformer wrapped) {                     // [ConstructorInjection]
        this.wrapped = wrapped;
    }

    @Override
    public String apply(String text) {
        return wrapped.apply(text);                              // [Delegation — pure pass-through in base]
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [ConcreteDecorator 1] — no extra state; trims leading and trailing whitespace
// ─────────────────────────────────────────────────────────────────────────────
class TrimDecorator extends TextDecorator {

    TrimDecorator(TextTransformer wrapped) {
        super(wrapped);                                          // [MustCallSuper — stores wrapped]
    }

    @Override
    public String apply(String text) {
        return super.apply(text).trim();                         // [AddBehavior after delegation]
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [ConcreteDecorator 2] — no extra state; uppercases the result
// ─────────────────────────────────────────────────────────────────────────────
class UpperCaseDecorator extends TextDecorator {

    UpperCaseDecorator(TextTransformer wrapped) {
        super(wrapped);
    }

    @Override
    public String apply(String text) {
        return super.apply(text).toUpperCase();                  // [AddBehavior after delegation]
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [ConcreteDecorator 3] — HAS OWN STATE: prefix string.
//                         Demonstrates that decorators can carry independent fields.
// ─────────────────────────────────────────────────────────────────────────────
class PrefixDecorator extends TextDecorator {

    private final String prefix;                                 // [OwnState — independent of wrapped component]

    PrefixDecorator(TextTransformer wrapped, String prefix) {
        super(wrapped);
        this.prefix = prefix;
    }

    @Override
    public String apply(String text) {
        return prefix + super.apply(text);                       // [PrependBehavior]
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [ConcreteDecorator 4] — HAS OWN STATE: suffix string
// ─────────────────────────────────────────────────────────────────────────────
class SuffixDecorator extends TextDecorator {

    private final String suffix;                                 // [OwnState]

    SuffixDecorator(TextTransformer wrapped, String suffix) {
        super(wrapped);
        this.suffix = suffix;
    }

    @Override
    public String apply(String text) {
        return super.apply(text) + suffix;                       // [AppendBehavior]
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Demo
// ─────────────────────────────────────────────────────────────────────────────
public class TextFormatterDemo {

    public static void main(String[] args) {
        String raw = "  hello world  ";

        // ── 1. ConcreteComponent alone ──────────────────────────────────────
        // [BaseCase] PassThrough returns text unchanged
        TextTransformer identity = new PassThroughTransformer();
        System.out.println("Identity:    \"" + identity.apply(raw) + "\"");

        // ── 2. Single decorator ─────────────────────────────────────────────
        // [SingleDecorator] Trim wraps PassThrough
        TextTransformer trim = new TrimDecorator(new PassThroughTransformer());
        System.out.println("Trim:        \"" + trim.apply(raw) + "\"");   // "hello world"

        // ── 3. Two decorators stacked ───────────────────────────────────────
        // [Stacking] UpperCase wraps Trim wraps PassThrough
        // Execution: PassThrough → Trim → UpperCase
        TextTransformer upper = new UpperCaseDecorator(
                new TrimDecorator(new PassThroughTransformer()));
        System.out.println("Trim+Upper:  \"" + upper.apply(raw) + "\"");  // "HELLO WORLD"

        // ── 4. Decorator with own state ─────────────────────────────────────
        // [OwnState] PrefixDecorator carries its own prefix field
        TextTransformer prefixed = new PrefixDecorator(
                new UpperCaseDecorator(
                        new TrimDecorator(new PassThroughTransformer())),
                "[INFO] ");
        System.out.println("Full chain:  \"" + prefixed.apply(raw) + "\"");  // "[INFO] HELLO WORLD"

        // ── 5. Order matters ────────────────────────────────────────────────
        // [OrderMatters] Prefix then Suffix vs Suffix then Prefix
        TextTransformer ps = new SuffixDecorator(
                new PrefixDecorator(new PassThroughTransformer(), ">>"), "<<");
        TextTransformer sp = new PrefixDecorator(
                new SuffixDecorator(new PassThroughTransformer(), "<<"), ">>");
        System.out.println("Prefix→Suffix: \"" + ps.apply("X") + "\"");  // ">>X<<"
        System.out.println("Suffix→Prefix: \"" + sp.apply("X") + "\"");  // ">>X<<" (same here, but not always)

        // ── 6. Polymorphism ─────────────────────────────────────────────────
        // [Polymorphism] All are TextTransformer — client treats them uniformly
        TextTransformer[] formatters = {
            new PassThroughTransformer(),
            new TrimDecorator(new PassThroughTransformer()),
            new UpperCaseDecorator(new TrimDecorator(new PassThroughTransformer())),
            new PrefixDecorator(new UpperCaseDecorator(
                    new TrimDecorator(new PassThroughTransformer())), "[LOG] ")
        };

        System.out.println("\n── Polymorphic pipeline ──");
        for (TextTransformer f : formatters) {
            System.out.println("  \"" + f.apply(raw) + "\"");
        }
    }
}

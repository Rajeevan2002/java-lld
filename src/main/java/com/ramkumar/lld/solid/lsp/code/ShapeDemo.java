package com.ramkumar.lld.solid.lsp.code;

/**
 * Scenario A: Shape Hierarchy (Rectangle / Square)
 *
 * Demonstrates the Liskov Substitution Principle (LSP).
 *
 * STEP 1 — VIOLATION: MutableRectangle + MutableSquare
 *   Square extends Rectangle and overrides setWidth/setHeight to keep width==height.
 *   This breaks Rectangle's invariant ("width and height are independent") and makes
 *   Square UN-substitutable for Rectangle.
 *   The helper method resizeAndCheck() proves the violation at runtime.
 *
 * STEP 2 — FIX: Shape interface + immutable Rectangle and Square
 *   No inheritance between Rectangle and Square.
 *   Both are independent implementations of Shape.
 *   LSP is satisfied: any Shape can be substituted for any other Shape in code
 *   that only calls area() or perimeter().
 *
 * Key LSP concepts illustrated:
 *   - "IS-A" (Java extends) ≠ "IS-SUBSTITUTABLE-FOR" (LSP)
 *   - Invariant: "width and height are independent" — Square breaks it
 *   - Fix: remove the parent-child relationship; use a shared abstraction instead
 *   - Immutability enforces LSP structurally (no setters → no invariant breach)
 *   - instanceof as a code smell that signals LSP violation in the caller
 */
public class ShapeDemo {

    // =========================================================================
    // ❌  STEP 1 — VIOLATION: Mutable Rectangle / Square hierarchy
    // =========================================================================

    /**
     * A mutable rectangle with independent width and height.
     *
     * INVARIANT: changing width does NOT change height, and vice versa.
     *            area() == width * height at all times.
     */
    static class MutableRectangle {
        protected int width;
        protected int height;

        public MutableRectangle(int width, int height) {
            if (width  <= 0) throw new IllegalArgumentException("width must be > 0");
            if (height <= 0) throw new IllegalArgumentException("height must be > 0");
            this.width  = width;
            this.height = height;
        }

        // Contract: setWidth changes ONLY width; height is unaffected
        public void setWidth(int w) {
            if (w <= 0) throw new IllegalArgumentException("width must be > 0");
            this.width = w;
        }

        // Contract: setHeight changes ONLY height; width is unaffected
        public void setHeight(int h) {
            if (h <= 0) throw new IllegalArgumentException("height must be > 0");
            this.height = h;
        }

        public int area()      { return width * height; }
        public int perimeter() { return 2 * (width + height); }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(w=" + width + ", h=" + height + ", area=" + area() + ")";
        }
    }

    /**
     * A square — all sides equal.
     *
     * ❌ OCP VIOLATION — breaks MutableRectangle's invariant:
     *   setWidth(5) → ALSO sets height to 5
     *   setHeight(3) → ALSO sets width to 3
     *
     * Java says: Square IS-A Rectangle (extends keyword).
     * LSP says: Square IS NOT SUBSTITUTABLE for a mutable Rectangle.
     *           Code that calls setWidth then setHeight and checks area() breaks.
     */
    static class MutableSquare extends MutableRectangle {

        public MutableSquare(int side) {
            super(side, side);
        }

        @Override
        public void setWidth(int w) {
            // ← INVARIANT BROKEN: a Rectangle user expects ONLY width to change
            super.width  = w;
            super.height = w;   // square constraint: keep both equal
        }

        @Override
        public void setHeight(int h) {
            // ← INVARIANT BROKEN: a Rectangle user expects ONLY height to change
            super.height = h;
            super.width  = h;   // square constraint: keep both equal
        }
    }

    /**
     * This method works correctly for MutableRectangle.
     * It BREAKS for MutableSquare because Square doesn't honour the
     * "width and height are independent" invariant.
     *
     * This is the LSP substitutability test.
     */
    static void resizeAndCheck(MutableRectangle r, int w, int h) {
        r.setWidth(w);
        r.setHeight(h);
        int expectedArea = w * h;
        int actualArea   = r.area();
        System.out.printf("  setWidth(%d), setHeight(%d) → expected area=%d, actual=%d → %s%n",
                w, h, expectedArea, actualArea,
                actualArea == expectedArea ? "✅ OK" : "❌ BROKEN — LSP VIOLATED");
    }

    // =========================================================================
    // ✅  STEP 2 — FIX: Immutable Shape hierarchy (no Rectangle→Square inheritance)
    // =========================================================================

    /**
     * The stable abstraction.
     * Both Rectangle and Square implement this independently — they are siblings,
     * not parent and child.
     */
    interface Shape {
        int area();
        int perimeter();
        String describe();
    }

    /**
     * An immutable rectangle.
     * IMMUTABILITY ENFORCES LSP: no setters → no invariant can be broken by a subclass.
     * Width and height are independent by construction (final fields).
     */
    static class Rectangle implements Shape {
        private final int width;
        private final int height;

        public Rectangle(int width, int height) {
            if (width  <= 0) throw new IllegalArgumentException("width must be > 0");
            if (height <= 0) throw new IllegalArgumentException("height must be > 0");
            this.width  = width;
            this.height = height;
        }

        @Override public int area()      { return width * height; }
        @Override public int perimeter() { return 2 * (width + height); }
        @Override public String describe() {
            return "Rectangle(w=" + width + ", h=" + height + ", area=" + area() + ")";
        }
        public int getWidth()  { return width; }
        public int getHeight() { return height; }
    }

    /**
     * An immutable square.
     * NOT a subclass of Rectangle — they are sibling implementations of Shape.
     * A square IS-A shape, but a square IS NOT a Rectangle (in LSP sense).
     */
    static class Square implements Shape {
        private final int side;

        public Square(int side) {
            if (side <= 0) throw new IllegalArgumentException("side must be > 0");
            this.side = side;
        }

        @Override public int area()      { return side * side; }
        @Override public int perimeter() { return 4 * side; }
        @Override public String describe() {
            return "Square(side=" + side + ", area=" + area() + ")";
        }
        public int getSide() { return side; }
    }

    /**
     * A circle — added without changing Rectangle or Square.
     * OCP + LSP working together: extend the abstraction without touching it.
     */
    static class Circle implements Shape {
        private final double radius;

        public Circle(double radius) {
            if (radius <= 0) throw new IllegalArgumentException("radius must be > 0");
            this.radius = radius;
        }

        @Override public int area()      { return (int) (Math.PI * radius * radius); }
        @Override public int perimeter() { return (int) (2 * Math.PI * radius); }
        @Override public String describe() {
            return "Circle(r=" + radius + ", area=" + area() + ")";
        }
    }

    /**
     * Works with ANY Shape — substitutable by design.
     * No instanceof. No if/else on type. LSP satisfied.
     */
    static void printShapeInfo(Shape shape) {
        System.out.println("  " + shape.describe());
        System.out.println("  area      : " + shape.area());
        System.out.println("  perimeter : " + shape.perimeter());
    }

    /**
     * Scales all shapes and sums total area.
     * This code never needs to know which Shape it has — LSP ensures it works for all.
     */
    static int totalArea(Shape[] shapes) {
        int total = 0;
        for (Shape s : shapes) total += s.area();
        return total;
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" LSP Demo: Shape Hierarchy (Rectangle / Square)");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ── STEP 1: Show the violation ────────────────────────────────────────
        System.out.println("── VIOLATION: MutableRectangle vs MutableSquare ─────────");

        System.out.println("\n[MutableRectangle] setWidth(5), setHeight(3):");
        resizeAndCheck(new MutableRectangle(1, 1), 5, 3);
        // Expected: area = 15  ← passes for Rectangle

        System.out.println("\n[MutableSquare] setWidth(5), setHeight(3):");
        resizeAndCheck(new MutableSquare(1), 5, 3);
        // Expected: area = 15, Actual: area = 9  ← BREAKS — Square is NOT substitutable

        System.out.println("\nProof: Square IS-A Rectangle in Java (compiles), but...");
        System.out.println("       Square IS NOT SUBSTITUTABLE for MutableRectangle (LSP violated).");
        System.out.println("       Any code that uses MutableRectangle and sets w and h independently");
        System.out.println("       will silently produce wrong results when given a MutableSquare.\n");

        // ── STEP 2: Show the fix ──────────────────────────────────────────────
        System.out.println("── FIX: Immutable Shape implementations (siblings, not parent-child) ──");

        Shape rect   = new Rectangle(5, 3);
        Shape square = new Square(4);
        Shape circle = new Circle(3.5);

        System.out.println("\n[Rectangle]:");
        printShapeInfo(rect);

        System.out.println("\n[Square]:");
        printShapeInfo(square);

        System.out.println("\n[Circle]:");
        printShapeInfo(circle);

        // ── STEP 3: Substitutability in action ───────────────────────────────
        System.out.println("\n── Substitutability: totalArea() works for ALL Shape types ────");
        Shape[] shapes = { rect, square, circle, new Rectangle(2, 6), new Square(3) };
        System.out.println("Shapes: " + shapes.length);
        System.out.println("Total area: " + totalArea(shapes));
        // No instanceof. No type checks. Every Shape is substitutable.

        // ── STEP 4: LSP invariant enforcement via final fields ───────────────
        System.out.println("\n── LSP enforcement: immutable fields prevent invariant breaches ─");
        Rectangle r = new Rectangle(5, 3);
        System.out.println("Rectangle width : " + r.getWidth());    // always 5
        System.out.println("Rectangle height: " + r.getHeight());   // always 3
        System.out.println("No setWidth/setHeight → no Square can override them → LSP structurally safe.");
    }
}

package com.ramkumar.lld.designpatterns.structural.flyweight.results;

import java.util.HashMap;
import java.util.Map;

/**
 * Reference solution — Flyweight Pattern: Chess Piece System
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>Cache key is {@code color + "-" + name} — covers BOTH intrinsic dimensions.
 *       A key of just {@code name} would merge White-Pawn and Black-Pawn.</li>
 *   <li>{@code row} and {@code col} are method parameters, never fields.
 *       Storing them would break sharing (Test 8 catches this).</li>
 *   <li>{@code computeIfAbsent} replaces the double-lookup {@code containsKey + get} pattern.</li>
 *   <li>Inner-class members are package-private by default — no {@code public} needed on {@code render()}.</li>
 * </ul>
 */
public class FlyweightReference {

    // ── [Flyweight] — intrinsic state only; extrinsic state passed as parameters ──
    static class PieceType {

        // [Intrinsic] Shared, immutable. private final = compile-time guarantee.
        // Without final, a subclass or reflection could corrupt the shared state.
        private final String name;
        private final String color;

        PieceType(String name, String color) {
            this.name  = name;
            this.color = color;
        }

        // [ExtrinsicParameters] row and col are NOT stored as fields.
        // They are used momentarily and discarded. This is the defining rule:
        // the flyweight receives extrinsic state, uses it, and forgets it.
        String render(int row, int col) {
            System.out.printf("[%s %s] at (%d, %d)%n", color, name, row, col);
            return String.format("[%s %s] at (%d, %d)", color, name, row, col);
        }
    }

    // ── [FlyweightFactory] — one instance per unique intrinsic key ─────────────
    static class PieceTypeFactory {

        // [Cache] Maps "White-Pawn" → one shared PieceType instance.
        // Only created once per key, regardless of how many pieces use it.
        private final Map<String, PieceType> cache = new HashMap<>();

        // [KeyDesign] Key = color + "-" + name covers both intrinsic dimensions.
        // "White-Pawn" != "Black-Pawn" != "White-Rook" — 12 keys for a full board.
        // [computeIfAbsent] Single map lookup on the hit path; creates only on miss.
        PieceType getPieceType(String name, String color) {
            String key = color + "-" + name;
            return cache.computeIfAbsent(key, k -> new PieceType(name, color));
        }

        int getCacheSize() {
            return cache.size();
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: Factory returns a non-null PieceType ─────────────────────
        PieceTypeFactory f1 = new PieceTypeFactory();
        PieceType pt = f1.getPieceType("Pawn", "White");
        System.out.println("Test 1 — getPieceType returns non-null: "
            + (pt != null ? "PASSED" : "FAILED"));

        // ── Test 2: Same call returns the SAME instance — reference equality ─
        PieceTypeFactory f2 = new PieceTypeFactory();
        PieceType p1 = f2.getPieceType("Pawn", "White");
        PieceType p2 = f2.getPieceType("Pawn", "White");
        System.out.println("Test 2 — same call returns same instance (==): "
            + (p1 == p2 ? "PASSED" : "FAILED"));

        // ── Test 3: Different color returns a DIFFERENT instance ──────────────
        PieceTypeFactory f3 = new PieceTypeFactory();
        PieceType white = f3.getPieceType("Pawn", "White");
        PieceType black = f3.getPieceType("Pawn", "Black");
        System.out.println("Test 3 — different color = different instance: "
            + (white != black ? "PASSED" : "FAILED"));

        // ── Test 4: Cache size after 3 unique types = 3 ───────────────────────
        PieceTypeFactory f4 = new PieceTypeFactory();
        f4.getPieceType("Pawn",  "White");
        f4.getPieceType("Pawn",  "Black");
        f4.getPieceType("Rook",  "White");
        f4.getPieceType("Pawn",  "White");   // duplicate — must NOT grow cache
        System.out.println("Test 4 — cache size = 3 after 3 unique types: "
            + (f4.getCacheSize() == 3 ? "PASSED" : "FAILED (got: " + f4.getCacheSize() + ")"));

        // ── Test 5: render() output format ────────────────────────────────────
        PieceTypeFactory f5 = new PieceTypeFactory();
        PieceType pawn = f5.getPieceType("Pawn", "White");
        String r5 = pawn.render(1, 0);
        System.out.println("Test 5 — render format: "
            + ("[White Pawn] at (1, 0)".equals(r5) ? "PASSED" : "FAILED (got: " + r5 + ")"));

        // ── Test 6: 8 White Pawns all share 1 flyweight ───────────────────────
        PieceTypeFactory f6 = new PieceTypeFactory();
        PieceType shared = f6.getPieceType("Pawn", "White");
        for (int i = 0; i < 8; i++) {
            PieceType p = f6.getPieceType("Pawn", "White");
            if (p != shared) { System.out.println("Test 6 — FAILED: pawn " + i + " is a different instance"); }
        }
        System.out.println("Test 6 — 8 White Pawns share 1 flyweight, cache size = 1: "
            + (f6.getCacheSize() == 1 ? "PASSED" : "FAILED (got: " + f6.getCacheSize() + ")"));

        // ── Test 7: Full chess board — 32 pieces, 12 unique flyweights ─────────
        PieceTypeFactory board = new PieceTypeFactory();
        String[] pieceNames = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};
        String[] colors     = {"White", "Black"};
        int[]    counts     = {8, 2, 2, 2, 1, 1};
        for (String color : colors) {
            for (int i = 0; i < pieceNames.length; i++) {
                for (int j = 0; j < counts[i]; j++) {
                    board.getPieceType(pieceNames[i], color);
                }
            }
        }
        System.out.println("Test 7 — 32 pieces use 12 unique flyweights: "
            + (board.getCacheSize() == 12 ? "PASSED" : "FAILED (got: " + board.getCacheSize() + ")"));

        // ── Test 8: render() on same flyweight at two positions — no state corruption
        PieceTypeFactory f8 = new PieceTypeFactory();
        PieceType queen = f8.getPieceType("Queen", "White");
        String r8a = queen.render(7, 3);
        String r8b = queen.render(4, 4);
        String r8c = queen.render(7, 3);   // must NOT return (4, 4) — the "last call" value
        System.out.println("Test 8a — render at (7,3): "
            + ("[White Queen] at (7, 3)".equals(r8a) ? "PASSED" : "FAILED (got: " + r8a + ")"));
        System.out.println("Test 8b — render at (4,4): "
            + ("[White Queen] at (4, 4)".equals(r8b) ? "PASSED" : "FAILED (got: " + r8b + ")"));
        System.out.println("Test 8c — no state corruption, back to (7,3): "
            + ("[White Queen] at (7, 3)".equals(r8c) ? "PASSED" : "FAILED (got: " + r8c + ")"));

        // ── Test 9 (catches the most common mistake: wrong cache key) ──────────
        // Mistake: key = name only (e.g., "Pawn") instead of color + "-" + name.
        // That would cause White-Pawn and Black-Pawn to share one flyweight,
        // making the board return only 6 flyweights instead of 12, and
        // rendering all Pawns as "White Pawn" (whichever was created first).
        PieceTypeFactory f9 = new PieceTypeFactory();
        PieceType whitePawn = f9.getPieceType("Pawn", "White");
        PieceType blackPawn = f9.getPieceType("Pawn", "Black");
        // Must be different objects — different intrinsic color
        boolean differentInstances = whitePawn != blackPawn;
        // Renders must reflect their own color, not bleed across
        String wRender = whitePawn.render(1, 0);
        String bRender = blackPawn.render(6, 0);
        boolean whitePawnRendersCorrectly = "[White Pawn] at (1, 0)".equals(wRender);
        boolean blackPawnRendersCorrectly = "[Black Pawn] at (6, 0)".equals(bRender);
        System.out.println("Test 9 — wrong key would merge White/Black Pawns: "
            + (differentInstances && whitePawnRendersCorrectly && blackPawnRendersCorrectly
               ? "PASSED" : "FAILED"));
        System.out.println("  white renders: " + wRender);
        System.out.println("  black renders: " + bRender);
    }
}

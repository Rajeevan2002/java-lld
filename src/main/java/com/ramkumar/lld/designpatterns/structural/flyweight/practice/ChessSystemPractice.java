package com.ramkumar.lld.designpatterns.structural.flyweight.practice;

import java.util.HashMap;
import java.util.Map;

/**
 * Practice Exercise — Flyweight Pattern: Chess Piece System
 *
 * <p><b>Scenario B — Intrinsic / Extrinsic State Split</b>
 *
 * <p>A chess application tracks 32 pieces on a board. Each piece has:
 * <ul>
 *   <li><b>Intrinsic state</b> (shared, immutable): {@code name} ("Pawn", "Rook", …)
 *       and {@code color} ("White", "Black"). 8 White Pawns all share the same intrinsic
 *       state — there is no need for 8 separate objects.</li>
 *   <li><b>Extrinsic state</b> (unique per occurrence): board position {@code (row, col)}.
 *       This changes every move and must NOT be stored inside the flyweight.</li>
 * </ul>
 *
 * <p>Without Flyweight: 32 piece objects. With Flyweight: 12 shared {@code PieceType}
 * objects (6 piece names × 2 colors) regardless of how many pieces are on the board.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   PieceType         [Flyweight]         ← TODOs 1–3
 *   PieceTypeFactory  [FlyweightFactory]  ← TODOs 4–7
 * </pre>
 *
 * <p><b>PieceType (Flyweight)</b> (TODOs 1–3):
 * <ul>
 *   <li>Fields: {@code private final String name}, {@code private final String color}
 *       — both immutable intrinsic state.</li>
 *   <li>Constructor: {@code PieceType(String name, String color)}.</li>
 *   <li>{@code render(int row, int col) → String} — uses row and col as extrinsic state;
 *       does NOT store them as fields.
 *       <ul>
 *         <li>Print: {@code System.out.printf("[%s %s] at (%d, %d)%n", color, name, row, col)}</li>
 *         <li>Return: {@code String.format("[%s %s] at (%d, %d)", color, name, row, col)}</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><b>PieceTypeFactory (FlyweightFactory)</b> (TODOs 4–7):
 * <ul>
 *   <li>Field: {@code private final Map<String, PieceType> cache} — initialized to
 *       {@code new HashMap<>()} (inline or in constructor).</li>
 *   <li>Constructor: {@code PieceTypeFactory()} — default; initialises the cache.</li>
 *   <li>{@code getPieceType(String name, String color) → PieceType}:
 *       <ul>
 *         <li>Build cache key: {@code color + "-" + name}  (e.g., {@code "White-Pawn"})</li>
 *         <li>If key not in cache: create {@code new PieceType(name, color)} and put it.</li>
 *         <li>Return the cached {@code PieceType}.</li>
 *         <li>Both {@code containsKey+put} and {@code computeIfAbsent} are acceptable.</li>
 *       </ul>
 *   </li>
 *   <li>{@code getCacheSize() → int} — returns {@code cache.size()} — the number of
 *       unique flyweight objects created so far.</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code row} and {@code col} must NOT be stored as fields in {@code PieceType}.</li>
 *   <li>{@code name} and {@code color} must be {@code private final} — never mutable.</li>
 *   <li>Do NOT call {@code new PieceType()} outside the factory — always go through
 *       {@code getPieceType()}.</li>
 * </ul>
 */
public class ChessSystemPractice {

    // ── Flyweight ──────────────────────────────────────────────────────────────
    static class PieceType {

        private final String name;
        private final String color;

        PieceType(String name, String color){
            this.name = name;
            this.color = color;
        }

        public String render(int row, int col){
            System.out.printf("[%s %s] at (%d, %d)%n", color, name, row, col);
            return String.format("[%s %s] at (%d, %d)", color, name, row, col);
        }
    }

    // ── FlyweightFactory ───────────────────────────────────────────────────────
    static class PieceTypeFactory {

        private final Map<String, PieceType> cache;

        PieceTypeFactory() {
            cache = new HashMap<>();
        }

        public PieceType getPieceType(String name, String color){
            String cacheKey = color + "-" + name;
            if(!cache.containsKey(cacheKey)) {
                cache.put(cacheKey, new PieceType(name, color));
            }
            return cache.get(cacheKey);
        }

        public int getCacheSize() {
            return cache.size();
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Factory returns a non-null PieceType (uncomment after TODO 6) ───────────
         PieceTypeFactory f1 = new PieceTypeFactory();
         PieceType pt = f1.getPieceType("Pawn", "White");
         System.out.println("Test 1 — getPieceType returns non-null: "
             + (pt != null ? "PASSED" : "FAILED"));

        // ── Test 2: Same call returns the SAME instance — reference equality (uncomment after TODO 6) ──
         PieceTypeFactory f2 = new PieceTypeFactory();
         PieceType p1 = f2.getPieceType("Pawn", "White");
         PieceType p2 = f2.getPieceType("Pawn", "White");
         System.out.println("Test 2 — same call returns same instance (==): "
             + (p1 == p2 ? "PASSED" : "FAILED"));

        // ── Test 3: Different color returns a DIFFERENT instance (uncomment after TODO 6) ──
         PieceTypeFactory f3 = new PieceTypeFactory();
         PieceType white = f3.getPieceType("Pawn", "White");
         PieceType black = f3.getPieceType("Pawn", "Black");
         System.out.println("Test 3 — different color = different instance: "
             + (white != black ? "PASSED" : "FAILED"));

        // ── Test 4: Cache size after 3 unique types = 3 (uncomment after TODO 7) ──────────
         PieceTypeFactory f4 = new PieceTypeFactory();
         f4.getPieceType("Pawn",  "White");
         f4.getPieceType("Pawn",  "Black");
         f4.getPieceType("Rook",  "White");
         f4.getPieceType("Pawn",  "White");   // duplicate — must NOT grow cache
         System.out.println("Test 4 — cache size = 3 after 3 unique types: "
             + (f4.getCacheSize() == 3 ? "PASSED" : "FAILED (got: " + f4.getCacheSize() + ")"));

        // ── Test 5: render() output format (uncomment after TODO 3) ─────────────────────
         PieceTypeFactory f5 = new PieceTypeFactory();
         PieceType pawn = f5.getPieceType("Pawn", "White");
         String r5 = pawn.render(1, 0);
         System.out.println("Test 5 — render format: "
             + ("[White Pawn] at (1, 0)".equals(r5) ? "PASSED" : "FAILED (got: " + r5 + ")"));

        // ── Test 6: 8 White Pawns all share 1 flyweight — cache size stays 1 (uncomment after TODOs 6–7) ──
         PieceTypeFactory f6 = new PieceTypeFactory();
         PieceType shared = f6.getPieceType("Pawn", "White");
         for (int i = 0; i < 8; i++) {
             PieceType p = f6.getPieceType("Pawn", "White");
             if (p != shared) { System.out.println("Test 6 — FAILED: pawn " + i + " is a different instance"); }
         }
         System.out.println("Test 6 — 8 White Pawns share 1 flyweight, cache size = 1: "
             + (f6.getCacheSize() == 1 ? "PASSED" : "FAILED (got: " + f6.getCacheSize() + ")"));

        // ── Test 7: Full chess board — 32 pieces, 12 unique flyweights (uncomment after all TODOs) ──
         PieceTypeFactory board = new PieceTypeFactory();
         String[] pieceNames = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};
         String[] colors     = {"White", "Black"};
         int[] counts        = {8, 2, 2, 2, 1, 1};   // pieces per type per color
         for (String color : colors) {
             for (int i = 0; i < pieceNames.length; i++) {
                 for (int j = 0; j < counts[i]; j++) {
                     board.getPieceType(pieceNames[i], color);
                 }
             }
         }
         System.out.println("Test 7 — 32 pieces use 12 unique flyweights: "
             + (board.getCacheSize() == 12 ? "PASSED" : "FAILED (got: " + board.getCacheSize() + ")"));

        // ── Test 8: render() on same flyweight at two positions — no state corruption (uncomment after TODOs 3, 6) ──
         PieceTypeFactory f8 = new PieceTypeFactory();
         PieceType queen = f8.getPieceType("Queen", "White");
         String r8a = queen.render(7, 3);   // expected: [White Queen] at (7, 3)
         String r8b = queen.render(4, 4);   // expected: [White Queen] at (4, 4)
         String r8c = queen.render(7, 3);   // expected: [White Queen] at (7, 3) — NOT (4, 4)
         System.out.println("Test 8a — render at (7,3): "
             + ("[White Queen] at (7, 3)".equals(r8a) ? "PASSED" : "FAILED (got: " + r8a + ")"));
         System.out.println("Test 8b — render at (4,4): "
             + ("[White Queen] at (4, 4)".equals(r8b) ? "PASSED" : "FAILED (got: " + r8b + ")"));
         System.out.println("Test 8c — no state corruption, back to (7,3): "
             + ("[White Queen] at (7, 3)".equals(r8c) ? "PASSED" : "FAILED (got: " + r8c + ")"));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   A "Pawn" is always a "Pawn" — its name and color never change. What always
    //   changes is where it sits on the board. Separate what never changes (store it
    //   once, share it) from what always changes (pass it in, never store it).
    //   A single factory keeps track of what has already been created.

    // HINT 2 (Direct):
    //   Use the Flyweight pattern.
    //   PieceType stores private final String name and color (intrinsic — shared).
    //   render(int row, int col) uses row/col as parameters only — do NOT assign them
    //   to fields.
    //   PieceTypeFactory holds private final Map<String, PieceType> cache.
    //   getPieceType() builds key = color + "-" + name; returns cached instance or
    //   creates and caches a new one.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   static class PieceType {
    //       private final String name;
    //       private final String color;
    //       PieceType(String name, String color) { ... }
    //       String render(int row, int col) { ... }   // returns String; does NOT store row/col
    //   }
    //
    //   static class PieceTypeFactory {
    //       private final Map<String, PieceType> cache = new HashMap<>();
    //       PieceTypeFactory() { }
    //       PieceType getPieceType(String name, String color) { ... }  // key = color+"-"+name
    //       int getCacheSize() { ... }
    //   }
}

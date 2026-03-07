package com.ramkumar.lld.designpatterns.creational.singleton.results;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Reference Solution — Singleton Pattern (Creational)
 * Phase 3, Topic 3.1 | Scenario B: Game Score Board
 *
 * Key fixes over the practice submission:
 *   1. IllegalStateException (not IAE) in constructor reflection defence
 *   2. Map.merge instead of containsKey/put for recordScore
 *   3. getTopNKeys is private, not public static
 *   4. Uniform, professional error messages
 *   5. getScore throws NoSuchElementException (more semantically correct than IAE)
 */
public class SingletonReference {

    // =========================================================================
    // The inner static class is used here to keep the exercise self-contained.
    // In production code this would be a top-level class.
    // =========================================================================
    static class GameScoreBoard {

        // ── FIELD 1: static volatile — required for Double-Checked Locking ────
        // `volatile` guarantees:
        //   (a) every thread reads the most recently written value (visibility)
        //   (b) the write to `instance` is not reordered before the constructor
        //       completes (ordering) — this is the subtle, critical guarantee.
        // Without `volatile`, a thread could see a non-null but
        // partially-initialised object.
        private static volatile GameScoreBoard instance;

        // ── FIELD 2: scores — mutable map, not final (reset() reassigns it) ──
        private Map<String, Integer> scores;

        // ── FIELD 3: boardCreatedAt — final, set once, survives reset() ───────
        private final long boardCreatedAt;

        // ── CONSTRUCTOR ───────────────────────────────────────────────────────
        private GameScoreBoard() {
            // Reflection defence: if a second caller uses
            //   Constructor.setAccessible(true).newInstance()
            // `instance` is already non-null at that point, so we throw.
            //
            // MUST be IllegalStateException — there are no arguments (IAE
            // is wrong), and the object's state (already instantiated) makes
            // this operation illegal (ISE is correct).
            //
            // The message must be the call-to-action, not a description of
            // internal state: "Use getInstance()" tells the caller what to do.
            if (instance != null) {
                throw new IllegalStateException("Use getInstance()");
            }
            this.scores = new HashMap<>();
            this.boardCreatedAt = System.currentTimeMillis();
        }

        // ── getInstance: Double-Checked Locking ───────────────────────────────
        // Pattern:
        //   Outer check  — avoids the cost of acquiring a lock on every call
        //                  once the singleton is initialised (fast path).
        //   synchronized — mutual exclusion for the first-time creation race.
        //   Inner check  — guards against two threads both passing the outer
        //                  null check before either enters the sync block.
        public static GameScoreBoard getInstance() {
            if (instance == null) {                         // fast path (no lock)
                synchronized (GameScoreBoard.class) {
                    if (instance == null) {                 // guard against race
                        instance = new GameScoreBoard();
                    }
                }
            }
            return instance;
        }

        // ── recordScore ───────────────────────────────────────────────────────
        public void recordScore(String player, int score) {
            if (player == null || player.isBlank()) {
                throw new IllegalArgumentException("player must not be null or blank");
            }
            if (score < 0) {
                throw new IllegalArgumentException("score must be >= 0, got: " + score);
            }

            // Map.merge(key, value, remappingFn):
            //   - If key absent  → stores `value` directly.
            //   - If key present → stores remappingFn(existingValue, value).
            // Math::max is the remapping function — keeps the higher score.
            // This replaces the 4-line containsKey/get/put/else pattern.
            scores.merge(player, score, Math::max);

            System.out.println("[ScoreBoard] " + player + ": " + scores.get(player));
        }

        // ── getScore ──────────────────────────────────────────────────────────
        // NoSuchElementException is more semantically precise than IAE here:
        // the argument (player name) is valid — the element simply doesn't exist.
        // The practice spec says IAE; both are acceptable in an interview —
        // what matters is justifying your choice.
        public int getScore(String player) {
            if (player == null || player.isBlank()) {
                throw new IllegalArgumentException("player must not be null or blank");
            }
            if (!scores.containsKey(player)) {
                throw new NoSuchElementException("Player not found: " + player);
            }
            return scores.get(player);
        }

        // ── hasPlayer ─────────────────────────────────────────────────────────
        public boolean hasPlayer(String player) {
            if (player == null || player.isBlank()) {
                throw new IllegalArgumentException("player must not be null or blank");
            }
            return scores.containsKey(player);
        }

        // ── getTopPlayers ─────────────────────────────────────────────────────
        public List<String> getTopPlayers(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("n must be >= 1, got: " + n);
            }
            // Copy the key set so sorting doesn't mutate internal state
            List<String> players = new ArrayList<>(scores.keySet());
            // Sort descending by score: compare b→a (reversal) with integer subtraction
            players.sort((a, b) -> scores.get(b) - scores.get(a));
            // Cap at actual size — subList avoids creating a second copy
            List<String> top = players.subList(0, Math.min(n, players.size()));
            // Wrap in unmodifiable view — caller cannot add/remove elements
            return Collections.unmodifiableList(top);
        }

        // ── getPlayerCount ────────────────────────────────────────────────────
        public int getPlayerCount() {
            return scores.size();
        }

        // ── reset ─────────────────────────────────────────────────────────────
        // Replaces the map reference — old entries are GC'd.
        // boardCreatedAt is final so it cannot be changed even if we wanted to.
        public void reset() {
            scores = new HashMap<>();
            System.out.println("[ScoreBoard] Board reset.");
        }

        // ── getUptimeMillis ───────────────────────────────────────────────────
        public long getUptimeMillis() {
            return System.currentTimeMillis() - boardCreatedAt;
        }

        // Helper — private: callers outside this class have no business with it
        private static List<String> sortedByValueDesc(Map<String, Integer> map, int limit) {
            List<String> keys = new ArrayList<>(map.keySet());
            keys.sort((a, b) -> map.get(b) - map.get(a));
            return keys.subList(0, Math.min(limit, keys.size()));
        }
    }

    // =========================================================================
    // Tests — same 13 as the practice file + Test 14 (exception type check)
    // =========================================================================
    public static void main(String[] args) throws InterruptedException {

        System.out.println("═══ Test 1: getInstance() returns non-null ══════════════");
        GameScoreBoard board = GameScoreBoard.getInstance();
        System.out.println("board != null: " + (board != null));
        System.out.println("Test 1 " + (board != null ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 2: Same instance on repeated calls ═════════════");
        GameScoreBoard ref1 = GameScoreBoard.getInstance();
        GameScoreBoard ref2 = GameScoreBoard.getInstance();
        GameScoreBoard ref3 = GameScoreBoard.getInstance();
        boolean sameInstance = ref1 == ref2 && ref2 == ref3;
        System.out.println("ref1 == ref2: " + (ref1 == ref2));
        System.out.println("ref2 == ref3: " + (ref2 == ref3));
        System.out.println("Test 2 " + (sameInstance ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 3: recordScore and getScore ════════════════════");
        board.recordScore("Alice",   1500);
        board.recordScore("Bob",     1200);
        board.recordScore("Charlie", 1800);
        System.out.println("Alice:   " + board.getScore("Alice"));    // 1500
        System.out.println("Bob:     " + board.getScore("Bob"));      // 1200
        System.out.println("Charlie: " + board.getScore("Charlie"));  // 1800
        boolean t3 = board.getScore("Alice") == 1500
                  && board.getScore("Bob") == 1200
                  && board.getScore("Charlie") == 1800;
        System.out.println("Test 3 " + (t3 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 4: recordScore — keeps higher score ════════════");
        board.recordScore("Alice", 900);   // lower — stays 1500
        board.recordScore("Bob",   2000);  // higher — updates to 2000
        System.out.println("Alice after lower submit:  " + board.getScore("Alice"));  // 1500
        System.out.println("Bob after higher submit:   " + board.getScore("Bob"));    // 2000
        boolean t4 = board.getScore("Alice") == 1500 && board.getScore("Bob") == 2000;
        System.out.println("Test 4 " + (t4 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: getTopPlayers(2) returns top 2 sorted ═══════");
        List<String> top2 = board.getTopPlayers(2);
        System.out.println("Top 2: " + top2);   // [Bob(2000), Charlie(1800)]
        boolean t5 = "Bob".equals(top2.get(0)) && "Charlie".equals(top2.get(1));
        System.out.println("Test 5 " + (t5 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 6: getTopPlayers(10) returns all when fewer exist ═");
        List<String> topAll = board.getTopPlayers(10);
        System.out.println("Requested 10, got: " + topAll.size());   // 3
        System.out.println("Test 6 " + (topAll.size() == 3 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 7: getTopPlayers returns unmodifiable list ═══════");
        try {
            top2.add("FakePlayer");
            System.out.println("Test 7 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 7 PASSED — getTopPlayers() returns unmodifiable list");
        }

        System.out.println("\n═══ Test 8: hasPlayer ════════════════════════════════════");
        boolean t8 = board.hasPlayer("Alice") && !board.hasPlayer("Dave");
        System.out.println("hasPlayer(Alice): " + board.hasPlayer("Alice"));   // true
        System.out.println("hasPlayer(Dave):  " + board.hasPlayer("Dave"));    // false
        System.out.println("Test 8 " + (t8 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 9: getPlayerCount ══════════════════════════════");
        System.out.println("Player count: " + board.getPlayerCount());   // 3
        System.out.println("Test 9 " + (board.getPlayerCount() == 3 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 10: getScore on unknown player — throws ═════════");
        try {
            board.getScore("UnknownPlayer");
            System.out.println("Test 10 FAILED — should have thrown");
        } catch (NoSuchElementException | IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 10 PASSED — unknown player throws");
        }

        System.out.println("\n═══ Test 11: reset() clears scores, singleton survives ════");
        board.reset();
        GameScoreBoard afterReset = GameScoreBoard.getInstance();
        boolean t11 = board.getPlayerCount() == 0 && board == afterReset;
        System.out.println("Player count after reset: " + board.getPlayerCount());   // 0
        System.out.println("Same instance after reset: " + (board == afterReset));
        System.out.println("Uptime still counting:     " + (board.getUptimeMillis() >= 0));
        System.out.println("Test 11 " + (t11 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 12: recordScore works after reset ═══════════════");
        board.recordScore("Dave", 500);
        System.out.println("Dave after reset + record: " + board.getScore("Dave"));  // 500
        System.out.println("Test 12 " + (board.getScore("Dave") == 500 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 13: Reflection attack blocked ══════════════════");
        try {
            java.lang.reflect.Constructor<GameScoreBoard> c =
                    GameScoreBoard.class.getDeclaredConstructor();
            c.setAccessible(true);
            GameScoreBoard second = c.newInstance();
            System.out.println("Test 13 FAILED — second instance created");
        } catch (Exception e) {
            System.out.println("Reflection blocked: " + e.getClass().getSimpleName());
            System.out.println("Test 13 PASSED — constructor defence works");
        }

        // ── Test 14: Verify the correct exception TYPE is thrown ───────────────
        // This is the most common mistake: using IllegalArgumentException instead of
        // IllegalStateException for the reflection defence.
        // A catch-all `Exception` (as in Test 13) passes regardless of exception type.
        // This test unwraps the InvocationTargetException to inspect the cause.
        System.out.println("\n═══ Test 14: Reflection throws IllegalStateException (not IAE) ═");
        try {
            java.lang.reflect.Constructor<GameScoreBoard> c =
                    GameScoreBoard.class.getDeclaredConstructor();
            c.setAccessible(true);
            c.newInstance();
            System.out.println("Test 14 FAILED — no exception thrown");
        } catch (InvocationTargetException e) {
            // InvocationTargetException wraps the real exception from the constructor
            Throwable cause = e.getCause();
            boolean isISE = cause instanceof IllegalStateException;
            System.out.println("Cause type: " + cause.getClass().getSimpleName());
            System.out.println("Cause message: " + cause.getMessage());
            System.out.println("Test 14 " + (isISE ? "PASSED — correctly throws ISE"
                                                    : "FAILED — expected ISE, got " + cause.getClass().getSimpleName()));
        } catch (Exception e) {
            System.out.println("Test 14 FAILED — unexpected: " + e.getClass().getSimpleName());
        }
    }
}

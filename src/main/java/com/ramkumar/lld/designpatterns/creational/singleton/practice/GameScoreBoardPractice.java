package com.ramkumar.lld.designpatterns.creational.singleton.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Practice Exercise — Singleton Pattern (Creational)
 * Phase 3, Topic 3.1 | Scenario B: Game Score Board
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building the backend for an online multiplayer game. The game
 * needs a single, shared leaderboard that all game modules access to
 * record and retrieve player scores.
 *
 * There must be EXACTLY ONE leaderboard for the entire game session.
 * Any module that asks for the leaderboard must get the same object.
 *
 * Implement GameScoreBoard as a Singleton using Double-Checked Locking
 * with a volatile instance field. Do NOT use eager initialization or
 * the Holder pattern — the goal is to practice DCL + volatile explicitly.
 *
 * ── Fields ─────────────────────────────────────────────────────────────
 *
 *   1. instance : static volatile GameScoreBoard
 *        - Must be `static volatile` — the DCL sentinel.
 *        - Starts as null; assigned once on first getInstance() call.
 *
 *   2. scores : Map<String, Integer>  (private, non-final — contents mutable)
 *        - Maps player name → score. Initialized to an empty HashMap.
 *
 *   3. boardCreatedAt : long  (private final — immutable timestamp)
 *        - Set to System.currentTimeMillis() when the singleton is first
 *          constructed. Never changes even after reset().
 *
 * ── Constructor ─────────────────────────────────────────────────────────
 *
 *   private GameScoreBoard()
 *     - Must be private to prevent direct instantiation.
 *     - Initialises scores = new HashMap<>() and boardCreatedAt.
 *     - DEFENCE: throw IllegalStateException("Use getInstance()")
 *       if instance is already non-null (blocks reflection attack).
 *
 * ── Methods ─────────────────────────────────────────────────────────────
 *
 *   static GameScoreBoard getInstance()
 *     - Double-checked locking with volatile:
 *         if (instance == null) {
 *             synchronized (GameScoreBoard.class) {
 *                 if (instance == null) { instance = new GameScoreBoard(); }
 *             }
 *         }
 *         return instance;
 *
 *   void recordScore(String player, int score)
 *     - Validates: player not null/blank → IllegalArgumentException
 *     - Validates: score >= 0 → IllegalArgumentException
 *     - If player is NOT on the board: add them with the given score.
 *     - If player IS already on the board: keep whichever score is HIGHER.
 *       (Players can only improve, not regress.)
 *     - Prints: "[ScoreBoard] <player>: <finalScore>"
 *
 *   int getScore(String player)
 *     - Validates: player not null/blank → IllegalArgumentException
 *     - If player not found → throw IllegalArgumentException("Player not found: " + player)
 *     - Returns the player's current score.
 *
 *   boolean hasPlayer(String player)
 *     - Validates: player not null/blank → IllegalArgumentException
 *     - Returns true if the player is on the board, false otherwise.
 *
 *   List<String> getTopPlayers(int n)
 *     - Validates: n >= 1 → IllegalArgumentException("n must be >= 1")
 *     - Returns a list of player names sorted by score DESCENDING.
 *     - If fewer than n players exist, returns all players (no error).
 *     - Returns an UNMODIFIABLE list.
 *     - Players with equal scores may appear in any order between themselves.
 *
 *   int getPlayerCount()
 *     - Returns the total number of players on the board.
 *
 *   void reset()
 *     - Clears all scores from the board (scores = new HashMap<>()).
 *     - The singleton INSTANCE itself is NOT destroyed — boardCreatedAt
 *       is preserved. getInstance() still returns the same object after reset().
 *     - Prints: "[ScoreBoard] Board reset."
 *
 *   long getUptimeMillis()
 *     - Returns System.currentTimeMillis() - boardCreatedAt.
 *     - Always >= 0.
 *
 * ── Design Constraints ──────────────────────────────────────────────────
 *
 *   1. Implement using Double-Checked Locking — DO NOT use eager init,
 *      Holder pattern, or Enum (save those for reference solution comparison).
 *   2. instance field MUST be declared `static volatile`.
 *   3. Constructor MUST be private.
 *   4. Reflection defence: throw IllegalStateException in constructor
 *      if instance is already non-null.
 *   5. getTopPlayers() MUST return an unmodifiable list.
 *   6. recordScore() keeps the HIGHER score (not the newer one).
 *
 * ═══════════════════════════════════════════════════════════════════════
 * DO NOT MODIFY the main() method — fill in the TODOs to make tests pass
 * ═══════════════════════════════════════════════════════════════════════
 */
public class GameScoreBoardPractice {

    // =========================================================================
    // ── TODO 1: Declare the static volatile instance field
    //            private static volatile GameScoreBoard instance;
    // =========================================================================
    static class GameScoreBoard {

        private static volatile GameScoreBoard instance;



    // =========================================================================
    // ── TODO 2: Declare private non-final scores field (Map<String, Integer>)
    //            and private final boardCreatedAt field (long)
    // =========================================================================
        private Map<String, Integer> scores;
        private final long boardCreatedAt;

    // =========================================================================
    // ── TODO 3: Implement private constructor
    //            - throw IllegalStateException if instance is already non-null
    //            - initialise scores = new HashMap<>()
    //            - initialise boardCreatedAt = System.currentTimeMillis()
    // =========================================================================
        private GameScoreBoard() {
            if(instance != null){
                throw new IllegalArgumentException("Instance cannot be null");
            }
            scores = new HashMap<>();
            boardCreatedAt = System.currentTimeMillis();
        }

    // =========================================================================
    // ── TODO 4: Implement static GameScoreBoard getInstance()
    //            Use Double-Checked Locking:
    //              if (instance == null) {
    //                  synchronized (GameScoreBoard.class) {
    //                      if (instance == null) { instance = new GameScoreBoard(); }
    //                  }
    //              }
    //              return instance;
    // =========================================================================
        public static GameScoreBoard getInstance() {
            if(instance == null){
                synchronized (GameScoreBoard.class){
                    if(instance == null){
                        instance = new GameScoreBoard();
                    }
                }
            }
            return instance;
        }

    // =========================================================================
    // ── TODO 5: Implement void recordScore(String player, int score)
    //            - Validate player not null/blank, score >= 0
    //            - If new player: add with score
    //            - If existing player: keep whichever is HIGHER (Math.max)
    //            - Print: "[ScoreBoard] <player>: <finalScore>"
    // =========================================================================
        public void recordScore(String player, int score){
            if(player == null || player.isBlank()){
                throw new IllegalArgumentException("Player cannot be null or blank!!");
            }
            if(score < 0){
                throw new IllegalArgumentException("Score has to be >= 0");
            }

            if(scores.containsKey(player)){
                scores.put(player, Math.max(scores.get(player), score));
            } else {
                scores.put(player, score);
            }

            System.out.println("[ScoreBoard] " + player + ": " + scores.get(player));
        }

    // =========================================================================
    // ── TODO 6: Implement int getScore(String player)
    //            - Validate player not null/blank
    //            - If not found: throw IllegalArgumentException("Player not found: " + player)
    //            - Return the score
    // =========================================================================
        public int getScore(String player){
            if(player == null || player.isBlank()){
                throw new IllegalArgumentException("Player cannot be null or Blank!!");
            }

            if(!scores.containsKey(player)) {
                throw new IllegalArgumentException("Player not found: " + player);
            }
            return scores.get(player);
        }

    // =========================================================================
    // ── TODO 7: Implement boolean hasPlayer(String player)
    //            - Validate player not null/blank
    //            - Return scores.containsKey(player)
    // =========================================================================
        public boolean hasPlayer(String player){
            if(player == null || player.isBlank()){
                throw new IllegalArgumentException("Player cannot be null or Blank!!");
            }

            return scores.containsKey(player);
        }

        public static List<String> getTopNKeys(Map<String, Integer> map, int n) {
            return map.entrySet()
                    .stream()
                    // Sort by value descending
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    // Take only the top N
                    .limit(n)
                    // Extract the keys
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

    // =========================================================================
    // ── TODO 8: Implement List<String> getTopPlayers(int n)
    //            - Validate n >= 1
    //            - Sort player names by score descending
    //            - Take min(n, playerCount) entries
    //            - Return Collections.unmodifiableList(...)
    //
    //            Hint: create a List<String> of all keys, then sort with:
    //              list.sort((a, b) -> scores.get(b) - scores.get(a));
    // =========================================================================
        public List<String> getTopPlayers(int n){
            if(n <= 0){
                throw new IllegalArgumentException("Players count has to be atleast > 0 !!");
            }

            return Collections.unmodifiableList(getTopNKeys(scores, Math.min(n, getPlayerCount())));
        }

    // =========================================================================
    // ── TODO 9: Implement int getPlayerCount()
    //            - Return scores.size()
    // =========================================================================
        public int getPlayerCount() {
            return scores.size();
        }
    // =========================================================================
    // ── TODO 10: Implement void reset()
    //             - scores = new HashMap<>()
    //             - boardCreatedAt must NOT change
    //             - Print: "[ScoreBoard] Board reset."
    // =========================================================================
        public void reset(){
            scores  = new HashMap<>();
            System.out.println("[ScoreBoard] Board reset.");
        }

    // =========================================================================
    // ── TODO 11: Implement long getUptimeMillis()
    //             - Return System.currentTimeMillis() - boardCreatedAt
    // =========================================================================
        public long getUptimeMillis(){
            return System.currentTimeMillis() - boardCreatedAt;
        }

    // =========================================================================
    // NOTE: All TODOs above must be inside the inner static class GameScoreBoard.
    //       The class declaration skeleton is:
    //
    //   static class GameScoreBoard {
    //       // TODO 1  — static volatile field
    //       // TODO 2  — instance fields
    //       // TODO 3  — private constructor
    //       // TODO 4  — getInstance()
    //       // TODO 5  — recordScore()
    //       // TODO 6  — getScore()
    //       // TODO 7  — hasPlayer()
    //       // TODO 8  — getTopPlayers()
    //       // TODO 9  — getPlayerCount()
    //       // TODO 10 — reset()
    //       // TODO 11 — getUptimeMillis()
    //   }
    // =========================================================================
    }
    // =========================================================================
    // DO NOT MODIFY — fill in TODOs above to make all tests pass
    // =========================================================================
    public static void main(String[] args) throws InterruptedException {

        System.out.println("═══ Test 1: getInstance() returns non-null ══════════════");
        GameScoreBoard board = GameScoreBoard.getInstance();
        System.out.println("board != null: " + (board != null));
        System.out.println("Test 1 PASSED: " + (board != null));

        System.out.println("\n═══ Test 2: Same instance on repeated calls ═════════════");
        GameScoreBoard ref1 = GameScoreBoard.getInstance();
        GameScoreBoard ref2 = GameScoreBoard.getInstance();
        GameScoreBoard ref3 = GameScoreBoard.getInstance();
        System.out.println("ref1 == ref2: " + (ref1 == ref2));
        System.out.println("ref2 == ref3: " + (ref2 == ref3));
        System.out.println("Test 2 PASSED: " + (ref1 == ref2 && ref2 == ref3));

        System.out.println("\n═══ Test 3: recordScore and getScore ════════════════════");
        board.recordScore("Alice", 1500);
        board.recordScore("Bob",   1200);
        board.recordScore("Charlie", 1800);
        System.out.println("Alice:   " + board.getScore("Alice"));    // 1500
        System.out.println("Bob:     " + board.getScore("Bob"));      // 1200
        System.out.println("Charlie: " + board.getScore("Charlie"));  // 1800
        System.out.println("Test 3 PASSED: " + (board.getScore("Alice") == 1500
                && board.getScore("Bob") == 1200
                && board.getScore("Charlie") == 1800));

        System.out.println("\n═══ Test 4: recordScore — keeps higher score ════════════");
        board.recordScore("Alice", 900);   // lower — should be ignored, Alice stays 1500
        board.recordScore("Bob",   2000);  // higher — Bob updates to 2000
        System.out.println("Alice after lower submit:  " + board.getScore("Alice"));  // 1500
        System.out.println("Bob after higher submit:   " + board.getScore("Bob"));    // 2000
        System.out.println("Test 4 PASSED: " + (board.getScore("Alice") == 1500
                && board.getScore("Bob") == 2000));

        System.out.println("\n═══ Test 5: getTopPlayers(2) returns top 2 sorted ═══════");
        List<String> top2 = board.getTopPlayers(2);
        System.out.println("Top 2: " + top2);   // [Bob(2000), Charlie(1800)]
        System.out.println("First place: " + top2.get(0));   // Bob
        System.out.println("Second place: " + top2.get(1));  // Charlie
        System.out.println("Test 5 PASSED: " + ("Bob".equals(top2.get(0))
                && "Charlie".equals(top2.get(1))));

        System.out.println("\n═══ Test 6: getTopPlayers(10) returns all when fewer exist ═");
        List<String> topAll = board.getTopPlayers(10);
        System.out.println("Requested 10, got: " + topAll.size());   // 3
        System.out.println("Test 6 PASSED: " + (topAll.size() == 3));

        System.out.println("\n═══ Test 7: getTopPlayers returns unmodifiable list ═══════");
        try {
            top2.add("FakePlayer");
            System.out.println("Test 7 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 7 PASSED — getTopPlayers() returns unmodifiable list");
        }

        System.out.println("\n═══ Test 8: hasPlayer ════════════════════════════════════");
        System.out.println("hasPlayer(Alice):  " + board.hasPlayer("Alice"));    // true
        System.out.println("hasPlayer(Dave):   " + board.hasPlayer("Dave"));     // false
        System.out.println("Test 8 PASSED: " + (board.hasPlayer("Alice")
                && !board.hasPlayer("Dave")));

        System.out.println("\n═══ Test 9: getPlayerCount ══════════════════════════════");
        System.out.println("Player count: " + board.getPlayerCount());   // 3
        System.out.println("Test 9 PASSED: " + (board.getPlayerCount() == 3));

        System.out.println("\n═══ Test 10: getScore on unknown player — throws ═════════");
        try {
            board.getScore("UnknownPlayer");
            System.out.println("Test 10 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("Test 10 PASSED — unknown player throws IAE");
        }

        System.out.println("\n═══ Test 11: reset() clears scores, singleton survives ════");
        long uptimeBefore = board.getUptimeMillis();
        board.reset();
        System.out.println("Player count after reset: " + board.getPlayerCount());   // 0
        // Singleton must still be the SAME instance after reset
        GameScoreBoard afterReset = GameScoreBoard.getInstance();
        System.out.println("Same instance after reset: " + (board == afterReset));
        System.out.println("Uptime still counting (>= 0): " + (board.getUptimeMillis() >= 0));
        System.out.println("boardCreatedAt preserved (uptime > 0): " + (board.getUptimeMillis() >= 0));
        System.out.println("Test 11 PASSED: " + (board.getPlayerCount() == 0
                && board == afterReset));

        System.out.println("\n═══ Test 12: recordScore works after reset ═══════════════");
        board.recordScore("Dave", 500);
        System.out.println("Dave after reset + record: " + board.getScore("Dave"));  // 500
        System.out.println("Test 12 PASSED: " + (board.getScore("Dave") == 500));

        System.out.println("\n═══ Test 13: Reflection attack blocked ══════════════════");
        try {
            java.lang.reflect.Constructor<GameScoreBoard> c =
                    GameScoreBoard.class.getDeclaredConstructor();
            c.setAccessible(true);
            GameScoreBoard second = c.newInstance();
            System.out.println("Test 13 FAILED — second instance created (reflection not blocked)");
        } catch (Exception e) {
            // The IllegalStateException is wrapped in an InvocationTargetException
            System.out.println("Reflection blocked: " + e.getClass().getSimpleName());
            System.out.println("Test 13 PASSED — constructor defence works");
        }
    }

    // =========================================================================
    // HINTS (read only if stuck)
    // =========================================================================

    /*
     * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
     * The key constraint is: "only one instance can ever exist."
     * Think about what access modifier prevents a class from being instantiated
     * from outside. Then think about how to store a reference to that single
     * instance so any caller can retrieve it — without creating a new one.
     *
     * For the "keeps higher score" rule: Java has a built-in method that takes
     * two numbers and returns the larger of the two.
     *
     * ── HINT 2 (Direct) ────────────────────────────────────────────────────
     * Skeleton fields and method signatures:
     *
     *   private static volatile GameScoreBoard instance;       // DCL sentinel
     *   private Map<String, Integer> scores;                   // mutable state
     *   private final long boardCreatedAt;                     // immutable timestamp
     *
     *   private GameScoreBoard() { ... }                       // blocks external `new`
     *
     *   public static GameScoreBoard getInstance() {           // double-checked locking
     *       if (instance == null) {
     *           synchronized (GameScoreBoard.class) {
     *               if (instance == null) { instance = new GameScoreBoard(); }
     *           }
     *       }
     *       return instance;
     *   }
     *
     * For recordScore: scores.merge(player, score, Math::max)
     *   — if key absent, stores score; if present, stores max(existing, score).
     *
     * For getTopPlayers:
     *   List<String> players = new ArrayList<>(scores.keySet());
     *   players.sort((a, b) -> scores.get(b) - scores.get(a));   // descending
     *   return Collections.unmodifiableList(players.subList(0, Math.min(n, players.size())));
     *
     * ── HINT 3 (Near-Solution) ─────────────────────────────────────────────
     * Class skeleton:
     *
     *   static class GameScoreBoard {
     *
     *       private static volatile GameScoreBoard instance;
     *       private Map<String, Integer> scores;
     *       private final long boardCreatedAt;
     *
     *       private GameScoreBoard() {
     *           if (instance != null)
     *               throw new IllegalStateException("Use getInstance()");
     *           this.scores        = new HashMap<>();
     *           this.boardCreatedAt = System.currentTimeMillis();
     *       }
     *
     *       public static GameScoreBoard getInstance() {
     *           if (instance == null) {
     *               synchronized (GameScoreBoard.class) {
     *                   if (instance == null) {
     *                       instance = new GameScoreBoard();
     *                   }
     *               }
     *           }
     *           return instance;
     *       }
     *
     *       public void recordScore(String player, int score) {
     *           if (player == null || player.isBlank()) throw new IllegalArgumentException(...);
     *           if (score < 0) throw new IllegalArgumentException(...);
     *           scores.merge(player, score, Math::max);
     *           System.out.println("[ScoreBoard] " + player + ": " + scores.get(player));
     *       }
     *
     *       public int getScore(String player) {
     *           if (player == null || player.isBlank()) throw new IllegalArgumentException(...);
     *           if (!scores.containsKey(player)) throw new IllegalArgumentException("Player not found: " + player);
     *           return scores.get(player);
     *       }
     *
     *       public void reset() {
     *           scores = new HashMap<>();
     *           System.out.println("[ScoreBoard] Board reset.");
     *       }
     *
     *       // ... hasPlayer, getTopPlayers, getPlayerCount, getUptimeMillis
     *   }
     */
}

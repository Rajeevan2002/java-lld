package com.ramkumar.lld.designpatterns.structural.flyweight.code;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// ─────────────────────────────────────────────────────────────────────────────
// Flyweight Pattern — Scenario A: Forest Simulation
//
// Problem: A forest renderer needs to display 10,000 trees. Each tree has a
//          position (x, y) and age that are unique — but species, color, and
//          texture are shared across many trees of the same type.
//
//          Without Flyweight: 10,000 tree objects each store species + color +
//          texture + x + y + age = large heap.
//          With Flyweight: 3 TreeType objects (intrinsic) shared by all 10,000
//          tree records (which only store the extrinsic x, y, age + reference).
//
// Participants:
//   TreeType         [Flyweight] — intrinsic: species, color, texture
//   TreeTypeRegistry [FlyweightFactory] — HashMap cache, get-or-create
//   Tree             [Client record] — extrinsic: x, y, age + flyweight reference
// ─────────────────────────────────────────────────────────────────────────────

// ── [Flyweight] — stores only intrinsic (shared, immutable) state ─────────────
class TreeType {

    // [Intrinsic] These fields are the same for every Oak tree, every Pine tree, etc.
    // private final — shared by thousands of callers; must never change.
    private final String species;
    private final String color;
    private final String texture;

    TreeType(String species, String color, String texture) {
        this.species = species;
        this.color   = color;
        this.texture = texture;
        // [CreationLog] In production this constructor might load a heavy texture asset.
        System.out.printf("  [Factory] Created TreeType: %s/%s%n", species, color);
    }

    // [Extrinsic] x, y, and age are NOT stored — they differ per tree and are passed in.
    // The method uses them momentarily and discards them. No state is modified.
    void plant(int x, int y, int age) {
        System.out.printf("  [%s/%s] planted at (%d,%d) age=%d%n", species, color, x, y, age);
    }

    String getSpecies() { return species; }
}

// ── [FlyweightFactory] — cache guarantees one instance per unique intrinsic key ─
class TreeTypeRegistry {

    // [Cache] Maps "species-color" → TreeType. Only one TreeType per unique combination.
    private final Map<String, TreeType> cache = new HashMap<>();

    TreeType getTreeType(String species, String color, String texture) {
        String key = species + "-" + color;   // [Key] deterministic, based on intrinsic state only
        if (!cache.containsKey(key)) {
            // [LazyCreate] Only create when first requested
            cache.put(key, new TreeType(species, color, texture));
        }
        return cache.get(key);   // [Return] same object for all future calls with this key
    }

    int getCacheSize() { return cache.size(); }
}

// ── [Client record] — holds extrinsic state + a reference to the shared flyweight ─
// Note: Tree is NOT the flyweight; it's a lightweight wrapper that exists per-instance.
class Tree {
    // [Extrinsic] unique per tree
    private final int x;
    private final int y;
    private final int age;

    // [FlyweightRef] shared — this reference costs only 8 bytes, not a full TreeType copy
    private final TreeType type;

    Tree(int x, int y, int age, TreeType type) {
        this.x    = x;
        this.y    = y;
        this.age  = age;
        this.type = type;
    }

    void draw() {
        type.plant(x, y, age);   // [Delegation] passes extrinsic state to the flyweight
    }
}

// ── Demo ─────────────────────────────────────────────────────────────────────
public class ForestDemo {

    public static void main(String[] args) {
        TreeTypeRegistry registry = new TreeTypeRegistry();
        Random rng = new Random(42);

        // ── 1. Populate the registry with 3 tree types ────────────────────────
        // [OnlyThreeObjects] Despite 50 trees below, only 3 TreeType objects exist.
        System.out.println("─── Populating tree types (expect 3 constructor logs) ───");
        TreeType oak   = registry.getTreeType("Oak",   "DarkGreen", "rough-bark");
        TreeType pine  = registry.getTreeType("Pine",  "LightGreen","needle-bark");
        TreeType birch = registry.getTreeType("Birch", "White",     "paper-bark");

        // Second request for Oak — no constructor log; same instance returned
        TreeType oak2  = registry.getTreeType("Oak", "DarkGreen", "rough-bark");
        System.out.println("oak == oak2 (same cached instance): " + (oak == oak2));
        System.out.printf("Unique tree types in cache: %d%n%n", registry.getCacheSize());

        // ── 2. Build a forest of 50 trees using the 3 shared flyweights ───────
        System.out.println("─── Planting 50 trees (only 3 TreeType objects used) ───");
        Tree[] forest = new Tree[50];
        TreeType[] types = { oak, pine, birch };
        for (int i = 0; i < forest.length; i++) {
            TreeType t = types[i % 3];              // [Rotation] Oak, Pine, Birch, Oak, ...
            forest[i] = new Tree(rng.nextInt(800), rng.nextInt(600), rng.nextInt(100) + 1, t);
        }
        // Draw just the first 6 to keep output readable
        for (int i = 0; i < 6; i++) forest[i].draw();
        System.out.printf("  ... (%d more trees)%n%n", forest.length - 6);

        // ── 3. Memory comparison illustration ────────────────────────────────
        System.out.println("─── Memory footprint comparison ───");
        System.out.printf(
            "  Without Flyweight: %,d tree objects × ~80 bytes = ~%,d bytes%n",
            forest.length, forest.length * 80L);
        System.out.printf(
            "  With Flyweight:    %d TreeType objects × ~48 bytes shared + %,d Tree refs × ~32 bytes = ~%,d bytes%n",
            registry.getCacheSize(),
            forest.length,
            registry.getCacheSize() * 48L + forest.length * 32L);
        System.out.printf(
            "  Savings: %,d bytes%n%n",
            (forest.length * 80L) - (registry.getCacheSize() * 48L + forest.length * 32L));

        // ── 4. Scale to 10,000 trees — still only 3 TreeType objects ─────────
        System.out.println("─── Scaling to 10,000 trees ───");
        int treeCount = 10_000;
        for (int i = 0; i < treeCount; i++) {
            TreeType t = registry.getTreeType(
                i % 3 == 0 ? "Oak" : i % 3 == 1 ? "Pine" : "Birch",
                i % 3 == 0 ? "DarkGreen" : i % 3 == 1 ? "LightGreen" : "White",
                "texture"
            );
            // tree record would hold x, y, age, t — not shown to keep output clean
        }
        System.out.printf("Unique TreeType objects after 10,000 trees: %d (was %d — no new types created)%n",
            registry.getCacheSize(), registry.getCacheSize());
    }
}

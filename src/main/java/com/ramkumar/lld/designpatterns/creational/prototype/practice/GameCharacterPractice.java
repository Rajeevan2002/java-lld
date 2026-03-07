package com.ramkumar.lld.designpatterns.creational.prototype.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Practice Exercise — Prototype Pattern (Creational)
 * Phase 3, Topic 3.5 | Scenario B: Game Character Loadout Cloning
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building a game character loadout system. Players design a base
 * character template (Warrior, Mage, or Archer) and then CLONE it to create
 * variants without rebuilding from scratch. A clone starts identical to the
 * original; the player then mutates only what differs.
 *
 * Cloning must be DEEP — adding an item to a clone's inventory must NOT affect
 * the original's inventory, and vice versa.
 *
 * ── ABSTRACT BASE: GameCharacter ─────────────────────────────────────────
 *
 * Fields (all mutable — can change on both originals and clones):
 *   String       name       — character name
 *   int          level      — character level
 *   List<String> inventory  — mutable list; MUST be deep-copied in every clone
 *
 * Normal constructor:
 *   GameCharacter(String name, int level)
 *   Initialises name, level, and an empty ArrayList for inventory.
 *
 * Protected copy constructor:
 *   GameCharacter(GameCharacter source)
 *   Used by subclass clone() methods to copy base fields.
 *   Copies name and level; creates a NEW ArrayList from source.inventory
 *   (deep copy — the two lists must be independent after this call).
 *
 * Abstract method:
 *   abstract GameCharacter clone()
 *   Each subclass returns a fully independent deep copy of itself.
 *   Do NOT use Java's Cloneable / Object.clone().
 *
 * addItem(String item) → void
 *   Appends item to inventory.
 *   Throws IllegalArgumentException("item must not be blank") if item is null or blank.
 *
 * removeItem(String item) → void
 *   Removes first occurrence of item from inventory.
 *   Throws NoSuchElementException("item not found: " + item) if not present.
 *
 * getInventory() → List<String>
 *   Returns Collections.unmodifiableList(inventory) — NEVER the raw list.
 *
 * Getters / setters: getName(), getLevel(), setName(String), setLevel(int)
 *
 * ── CONCRETE PROTOTYPE: Warrior ──────────────────────────────────────────
 *
 * Extends GameCharacter.
 * Extra mutable field: int armor
 *
 * Normal constructor:
 *   Warrior(String name, int level, int armor)
 *   Calls super(name, level), sets armor.
 *
 * Private copy constructor:
 *   Warrior(Warrior source)
 *   Calls super(source) to copy base fields (including deep-copied inventory),
 *   then copies armor.
 *
 * clone() → Warrior
 *   Returns new Warrior(this)  (delegates to copy constructor).
 *   Covariant return type: Warrior, not GameCharacter.
 *
 * getArmor() / setArmor(int)
 *
 * toString():
 *   "Warrior{name='<name>', level=<n>, armor=<n>, inventory=<list>}"
 *
 * ── CONCRETE PROTOTYPE: Mage ──────────────────────────────────────────────
 *
 * Extends GameCharacter.
 * Extra mutable field: int manaPool
 *
 * Normal constructor:
 *   Mage(String name, int level, int manaPool)
 *
 * Private copy constructor:
 *   Mage(Mage source) — calls super(source), copies manaPool.
 *
 * clone() → Mage   (covariant)
 *
 * getManaPool() / setManaPool(int)
 *
 * toString():
 *   "Mage{name='<name>', level=<n>, manaPool=<n>, inventory=<list>}"
 *
 * ── CONCRETE PROTOTYPE: Archer ────────────────────────────────────────────
 *
 * Extends GameCharacter.
 * Extra mutable field: int arrowCount
 *
 * Normal constructor:
 *   Archer(String name, int level, int arrowCount)
 *
 * Private copy constructor:
 *   Archer(Archer source) — calls super(source), copies arrowCount.
 *
 * clone() → Archer   (covariant)
 *
 * getArrowCount() / setArrowCount(int)
 *
 * toString():
 *   "Archer{name='<name>', level=<n>, arrowCount=<n>, inventory=<list>}"
 *
 * ── DESIGN CONSTRAINTS ────────────────────────────────────────────────────
 *   - Do NOT use Java's Cloneable or Object.clone() — use copy constructors
 *   - Deep copy: the clone's inventory must be a NEW ArrayList, not the same reference
 *   - getInventory() must return Collections.unmodifiableList(inventory), never the raw list
 *   - No instanceof chains anywhere — use polymorphism
 *   - Covariant return: Warrior.clone() returns Warrior, Mage.clone() returns Mage, etc.
 *
 * ═══════════════════════════════════════════════════════════════════════
 */
public class GameCharacterPractice {

    // =========================================================================
    // ABSTRACT PROTOTYPE
    // =========================================================================

    abstract static class GameCharacter {

        protected String name;
        protected int level;
        protected List<String> inventory;


        GameCharacter(String name, int level) {
            if(name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name cannot be null or Blank!!");
            }
            if(level < 0) {
                throw new IllegalArgumentException("Level should be greater than zero!!!");
            }
            this.name = name;
            this.level = level;
            this.inventory = new ArrayList<>();
        }


        protected GameCharacter(GameCharacter source) {
            // your code here
            this.name = source.name;
            this.level = source.level;
            this.inventory = new ArrayList<>(source.inventory);
        }


        public abstract GameCharacter clone();


        public void addItem(String item) {
            if(item == null || item.isBlank()){
                throw new IllegalArgumentException("item must not be blank");
            }
            inventory.add(item);
            System.out.println("Item : " +  item + " added to the Inventory");
        }


        public void removeItem(String item) {
            if(item == null || item.isBlank()) {
                throw new IllegalArgumentException("Item should not be null or Blank");
            }
            if(!inventory.contains(item)) {
                throw new NoSuchElementException("item not found: " +  item);
            }
            inventory.remove(item);
        }

        public List<String> getInventory() {
            return Collections.unmodifiableList(inventory);
        }

        public String getName() { return name; }
        public int getLevel() { return level; }
        public void setName(String v) {this.name = v;}
        public void setLevel(int v) {this.level = v;}
    }

    // =========================================================================
    // CONCRETE PROTOTYPE: Warrior
    // =========================================================================

    static class Warrior extends GameCharacter {


        private int armor;


        Warrior(String name, int level, int armor) {
            super(name, level);
            if(armor <= 0 ) {
                throw new IllegalArgumentException("Armor has to positive");
            }
            this.armor = armor;
        }

        private Warrior(Warrior source) {
            super(source);
            // your code here
            this.armor = source.armor;
        }

        @Override
        public Warrior clone() {
            // your code here
            return new Warrior(this);
        }

        public int getArmor() {
            return this.armor;
        }

        public void setArmor(int v){
            if(v<=0) {
                throw new IllegalArgumentException("v has to > 0 ");
            }
            this.armor = v;
        }

        @Override
        public String toString() {
            return String.format("Warrior{name='%s', level=%d, armor=%d, inventory=%s}", name, level, armor, inventory);
        }
    }

    // =========================================================================
    // CONCRETE PROTOTYPE: Mage
    // =========================================================================

    static class Mage extends GameCharacter {


        private int manaPool;

        Mage(String name, int level, int manaPool) {
            super(name, level);
            if(manaPool <= 0){
                throw new IllegalArgumentException("ManaPool has to > 0");
            }
            this.manaPool = manaPool;
        }


        private Mage(Mage source) {
            super(source);
            // your code here
            this.manaPool = source.manaPool;
        }


        @Override
        public Mage clone() {
            // your code here
            return new Mage(this);
        }

        public int getManaPool() {
            return manaPool;
        }

        public void setManaPool(int v) {
            if(v <= 0) {
                throw new IllegalArgumentException("v has to be greater than 0");
            }
            this.manaPool = v;
        }

        // ── TODO 20: toString() → String
        //    Format: "Mage{name='<name>', level=<n>, manaPool=<n>, inventory=<list>}"
        @Override
        public String toString() {
            // your code here
            return String.format("Mage{name='%s', level=%d, manaPool=%d, inventory=%s}", name, level, manaPool, inventory);
        }
    }

    // =========================================================================
    // CONCRETE PROTOTYPE: Archer
    // =========================================================================

    static class Archer extends GameCharacter {


        private int arrowCount;

        Archer(String name, int level, int arrowCount) {
            super(name, level);
            // your code here
            if(arrowCount <= 0) {
                throw new IllegalArgumentException("ArrowCount has to > 0");
            }
            this.arrowCount = arrowCount;
        }


        private Archer(Archer source) {
            super(source);
            // your code here
            this.arrowCount = source.arrowCount;
        }

        @Override
        public Archer clone() {
            // your code here
            return new Archer(this);
        }

        public int getArrowCount(){ return arrowCount;}
        public void setArrowCount(int v) {
            if(v <= 0 ) throw new IllegalArgumentException("Arrow Count has to be greater than zero!!!");
            this.arrowCount = v;
        }

        // ── TODO 26: toString() → String
        //    Format: "Archer{name='<name>', level=<n>, arrowCount=<n>, inventory=<list>}"
        @Override
        public String toString() {
            // your code here
            return String.format("Archer{name='%s', level=%d, arrowCount=%s, inventory=%s}", name, level,
                    arrowCount, inventory) ;
        }
    }

    // =========================================================================
    // DO NOT MODIFY — test cases; fill in the TODOs above to make them pass
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: Clone Warrior — all fields copied ═══════════════");
        Warrior w = new Warrior("Thorin", 40, 350);
        w.addItem("Battleaxe");
        w.addItem("Iron Shield");
        Warrior wClone = w.clone();
        boolean t1 = "Thorin".equals(wClone.getName())
                  && wClone.getLevel() == 40
                  && wClone.getArmor() == 350
                  && wClone.getInventory().contains("Battleaxe")
                  && wClone.getInventory().contains("Iron Shield");
        System.out.println("Clone: " + wClone);
        System.out.println("Test 1 " + (t1 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 2: Deep copy — clone inventory independent from original ═");
        wClone.addItem("War Hammer");
        boolean t2 = !w.getInventory().contains("War Hammer")
                  && wClone.getInventory().contains("War Hammer");
        System.out.println("Original inventory: " + w.getInventory());
        System.out.println("Clone inventory:    " + wClone.getInventory());
        System.out.println("Test 2 " + (t2 ? "PASSED" : "FAILED — inventory not deep-copied"));

        System.out.println("\n═══ Test 3: Mutate clone — original unchanged ════════════════");
        wClone.setName("Thorin Boss");
        wClone.setLevel(99);
        wClone.setArmor(800);
        boolean t3 = "Thorin".equals(w.getName())
                  && w.getLevel() == 40
                  && w.getArmor() == 350;
        System.out.println("Original: " + w);
        System.out.println("Clone:    " + wClone);
        System.out.println("Test 3 " + (t3 ? "PASSED" : "FAILED — original was mutated"));

        System.out.println("\n═══ Test 4: Clone Mage — all fields copied ═══════════════════");
        Mage m = new Mage("Gandalf", 60, 500);
        m.addItem("Staff of Power");
        m.addItem("Spell Tome");
        Mage mClone = m.clone();
        boolean t4 = "Gandalf".equals(mClone.getName())
                  && mClone.getLevel() == 60
                  && mClone.getManaPool() == 500
                  && mClone.getInventory().contains("Staff of Power");
        System.out.println("Clone: " + mClone);
        System.out.println("Test 4 " + (t4 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: Clone Archer — covariant return (no cast needed) ═");
        Archer a = new Archer("Legolas", 55, 200);
        a.addItem("Elven Bow");
        Archer aClone = a.clone();   // must return Archer, not GameCharacter
        aClone.setArrowCount(300);
        boolean t5 = a.getArrowCount() == 200
                  && aClone.getArrowCount() == 300
                  && aClone.getInventory().contains("Elven Bow");
        System.out.println("Original arrow count: " + a.getArrowCount());
        System.out.println("Clone arrow count:    " + aClone.getArrowCount());
        System.out.println("Test 5 " + (t5 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 6: getInventory returns unmodifiable view ═══════════");
        try {
            w.getInventory().add("Hack Attempt");
            System.out.println("Test 6 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 6 PASSED — getInventory() returns unmodifiable view");
        }

        System.out.println("\n═══ Test 7: addItem — blank item → IAE ══════════════════════");
        try {
            w.addItem("   ");
            System.out.println("Test 7 FAILED — should have thrown");
        } catch (IllegalArgumentException e) {
            boolean t7 = "item must not be blank".equals(e.getMessage());
            System.out.println("Caught IAE: " + e.getMessage());
            System.out.println("Test 7 " + (t7 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 8: removeItem — item not present → NSE ══════════════");
        try {
            w.removeItem("Dragon Scale");
            System.out.println("Test 8 FAILED — should have thrown");
        } catch (NoSuchElementException e) {
            boolean t8 = "item not found: Dragon Scale".equals(e.getMessage());
            System.out.println("Caught NSE: " + e.getMessage());
            System.out.println("Test 8 " + (t8 ? "PASSED" : "FAILED — wrong message"));
        }

        System.out.println("\n═══ Test 9: removeItem — present item removed correctly ══════");
        Warrior w9 = new Warrior("Test", 1, 10);
        w9.addItem("Sword");
        w9.addItem("Potion");
        w9.removeItem("Sword");
        boolean t9 = !w9.getInventory().contains("Sword")
                  && w9.getInventory().contains("Potion");
        System.out.println("After remove: " + w9.getInventory());
        System.out.println("Test 9 " + (t9 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 10: Polymorphic clone — clone via base reference ════");
        GameCharacter[] templates = {
            new Warrior("Knight", 30, 200),
            new Mage("Wizard", 45, 400),
            new Archer("Hunter", 35, 150)
        };
        boolean t10 = true;
        for (GameCharacter gc : templates) {
            GameCharacter copy = gc.clone();
            copy.setName(gc.getName() + " Copy");
            if (gc.getName().equals(copy.getName())) {
                t10 = false; // mutation of clone should not affect original name
            }
            System.out.println("  Original: " + gc.getName() + " | Clone: " + copy.getName());
        }
        System.out.println("Test 10 " + (t10 ? "PASSED" : "FAILED — clone name bled back to original"));

        System.out.println("\n═══ Test 11: Deep copy — remove from original, clone unchanged ═");
        Mage m11 = new Mage("Merlin", 70, 600);
        m11.addItem("Crystal Ball");
        m11.addItem("Arcane Tome");
        Mage m11clone = m11.clone();
        m11.removeItem("Crystal Ball");
        boolean t11 = !m11.getInventory().contains("Crystal Ball")
                   && m11clone.getInventory().contains("Crystal Ball");
        System.out.println("Original: " + m11.getInventory());
        System.out.println("Clone:    " + m11clone.getInventory());
        System.out.println("Test 11 " + (t11 ? "PASSED" : "FAILED — inventory shared between original and clone"));

        System.out.println("\n═══ Test 12: toString() format correct for all three types ═══");
        Warrior tw = new Warrior("Rex", 10, 100);
        Mage    tm = new Mage("Zara", 20, 300);
        Archer  ta = new Archer("Swift", 15, 50);
        boolean t12 = tw.toString().equals("Warrior{name='Rex', level=10, armor=100, inventory=[]}")
                   && tm.toString().equals("Mage{name='Zara', level=20, manaPool=300, inventory=[]}")
                   && ta.toString().equals("Archer{name='Swift', level=15, arrowCount=50, inventory=[]}");
        System.out.println(tw);
        System.out.println(tm);
        System.out.println(ta);
        System.out.println("Test 12 " + (t12 ? "PASSED" : "FAILED — toString() format wrong"));
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * HINTS — read only if stuck; try for at least 20 minutes first
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle):
 *   Think about what "deep copy" means for a List field — if you copy the
 *   reference, both objects point to the same List and mutations bleed.
 *   The clone's list must be a completely new object containing the same elements.
 *   Consider: how can a subclass's clone() reuse the base class's copy logic?
 *
 * HINT 2 (Direct):
 *   - Use the copy constructor approach — NOT Java's Cloneable.
 *   - In GameCharacter: add a protected copy constructor that takes a GameCharacter.
 *     Inside it: copy name and level by value; create the inventory as
 *     new ArrayList<>(source.inventory)  ← this is the deep copy.
 *   - In each subclass: add a private copy constructor that calls super(source)
 *     and then copies the subclass-specific field (armor/manaPool/arrowCount).
 *   - Each clone() method simply returns new Warrior(this), new Mage(this), etc.
 *   - Covariant return: declare clone() as returning Warrior (not GameCharacter)
 *     in Warrior.clone(), etc.
 *   - getInventory() must return Collections.unmodifiableList(inventory).
 *   - removeItem(): use the boolean return of List.remove(Object) to detect missing items.
 *
 * HINT 3 (Near-solution — class skeleton without method bodies):
 *
 *   abstract static class GameCharacter {
 *       protected String       name;
 *       protected int          level;
 *       protected List<String> inventory;
 *
 *       GameCharacter(String name, int level) { ... }
 *       protected GameCharacter(GameCharacter source) {
 *           // copy name, level; deep-copy inventory
 *       }
 *       public abstract GameCharacter clone();
 *       public void addItem(String item)    { ... }
 *       public void removeItem(String item) { ... }
 *       public List<String> getInventory()  { ... }
 *       public String getName()   { ... }
 *       public int    getLevel()  { ... }
 *       public void   setName(String v) { ... }
 *       public void   setLevel(int v)   { ... }
 *   }
 *
 *   static class Warrior extends GameCharacter {
 *       int armor;
 *       Warrior(String name, int level, int armor)  { ... }
 *       private Warrior(Warrior source)              { ... }
 *       @Override public Warrior clone()             { return new Warrior(this); }
 *       public int  getArmor()      { ... }
 *       public void setArmor(int v) { ... }
 *       @Override public String toString() { ... }
 *   }
 *   // Mage and Archer follow the same pattern with manaPool / arrowCount
 */

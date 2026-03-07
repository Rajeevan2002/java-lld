package com.ramkumar.lld.designpatterns.creational.prototype.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reference Solution — Prototype Pattern (Creational)
 * Phase 3, Topic 3.5 | Scenario B: Game Character Loadout Cloning
 *
 * ── Key decisions ──────────────────────────────────────────────────────────
 *   • Copy constructor approach — NOT Cloneable / Object.clone()
 *     Effective Java §13: Cloneable is fragile, shallow by default, and
 *     throws a checked exception. Copy constructors are explicit and safe.
 *   • protected copy constructor on the base class — the single place that
 *     performs the deep copy (new ArrayList<>). Every subclass clone() is
 *     just one line.
 *   • Covariant return types — Warrior.clone() returns Warrior, not
 *     GameCharacter, so the caller never needs a cast.
 *   • getInventory() returns Collections.unmodifiableList — the internal list
 *     is never exposed; external code cannot mutate it.
 *   • Setters have NO validation — the spec does not require it. Validating
 *     beyond the spec means rejecting inputs the spec deems valid.
 * ──────────────────────────────────────────────────────────────────────────
 */
public class PrototypeReference {

    // =========================================================================
    // ABSTRACT PROTOTYPE
    // =========================================================================

    // [AbstractPrototype] — declares the clone() contract that all subclasses must fulfill
    abstract static class GameCharacter {

        // [Mutable fields] — all three can be changed on both originals and clones
        protected String       name;
        protected int          level;
        // [Deep-copy target] — List is mutable; sharing its reference between original
        // and clone would cause mutations to bleed. Must be deep-copied in every clone.
        protected List<String> inventory;

        // [Normal constructor] — used to build the original template
        GameCharacter(String name, int level) {
            this.name      = name;
            this.level     = level;
            // [Empty ArrayList] — not null; subclasses and callers can call addItem immediately
            this.inventory = new ArrayList<>();
        }

        // [Protected copy constructor] — THE engine of the Prototype pattern.
        // Every subclass clone() delegates here via super(source), so there is
        // only ONE place that performs the deep copy. If you ever add a new
        // mutable base field, you update it here and every clone() stays correct.
        protected GameCharacter(GameCharacter source) {
            this.name  = source.name;   // String is immutable — sharing the reference is safe
            this.level = source.level;  // int is a primitive — copied by value automatically
            // [Deep copy] — new ArrayList from source list → two independent lists.
            // Mutations on clone.inventory do NOT affect source.inventory, and vice versa.
            this.inventory = new ArrayList<>(source.inventory);
        }

        // [Abstract clone()] — each ConcretePrototype must implement this.
        // Declared here so callers can clone through a GameCharacter reference
        // (polymorphic clone — Test 10).
        public abstract GameCharacter clone();

        // [addItem] — appends an item; validates against blank per spec
        public void addItem(String item) {
            if (item == null || item.isBlank())
                throw new IllegalArgumentException("item must not be blank");
            inventory.add(item);
            // No extra println — callers do not expect side effects from addItem
        }

        // [removeItem] — removes first occurrence; throws NSE if absent
        // The spec says to throw NSE("item not found: " + item) if not present.
        // The spec does NOT require a blank check on removeItem — only addItem.
        public void removeItem(String item) {
            // List.remove(Object) returns false when the element is not present
            if (!inventory.remove(item))
                throw new NoSuchElementException("item not found: " + item);
        }

        // [getInventory] — returns an unmodifiable view, NEVER the raw list.
        // Collections.unmodifiableList wraps the same list (no copy), so the view
        // stays in sync as items are added/removed, but the caller cannot mutate it.
        // [Encapsulation] — internal state is read-only from the outside.
        public List<String> getInventory() {
            return Collections.unmodifiableList(inventory);
        }

        // Standard getters/setters — no extra validation; the spec does not require it
        public String getName()         { return name; }
        public int    getLevel()        { return level; }
        public void   setName(String v) { this.name  = v; }
        public void   setLevel(int v)   { this.level = v; }
    }

    // =========================================================================
    // CONCRETE PROTOTYPE: Warrior
    // =========================================================================

    // [ConcretePrototype A] — extends base; adds armor
    static class Warrior extends GameCharacter {

        // [Subclass-specific mutable field] — not final; can be changed on clone
        int armor;

        // [Normal constructor] — builds the original template
        Warrior(String name, int level, int armor) {
            super(name, level);   // delegates base-field init to GameCharacter
            this.armor = armor;
        }

        // [Private copy constructor] — called only by clone()
        // super(source) runs GameCharacter's copy constructor:
        //   • copies name and level
        //   • deep-copies inventory into a new ArrayList
        // Then we copy our own extra field.
        private Warrior(Warrior source) {
            super(source);
            this.armor = source.armor;  // int primitive — copied by value
        }

        // [clone()] — one line; all the work is in the copy constructor
        // [Covariant return] — return type is Warrior, not GameCharacter.
        // Callers can write: Warrior wClone = w.clone()  (no cast needed).
        @Override
        public Warrior clone() {
            return new Warrior(this);
        }

        // Getter/setter — setter MUST assign the field (the most common mistake in this exercise)
        public int  getArmor()      { return armor; }
        public void setArmor(int v) { this.armor = v; }  // ← assignment is mandatory

        // [toString()] — %d for int fields throughout; inventory uses its own toString()
        @Override
        public String toString() {
            return String.format("Warrior{name='%s', level=%d, armor=%d, inventory=%s}",
                    name, level, armor, inventory);
        }
    }

    // =========================================================================
    // CONCRETE PROTOTYPE: Mage
    // =========================================================================

    // [ConcretePrototype B] — same pattern as Warrior, different extra field
    static class Mage extends GameCharacter {

        int manaPool;

        Mage(String name, int level, int manaPool) {
            super(name, level);
            this.manaPool = manaPool;
        }

        private Mage(Mage source) {
            super(source);
            this.manaPool = source.manaPool;
        }

        // [Covariant return] — Mage, not GameCharacter
        @Override
        public Mage clone() {
            return new Mage(this);
        }

        public int  getManaPool()      { return manaPool; }
        public void setManaPool(int v) { this.manaPool = v; }

        @Override
        public String toString() {
            return String.format("Mage{name='%s', level=%d, manaPool=%d, inventory=%s}",
                    name, level, manaPool, inventory);
        }
    }

    // =========================================================================
    // CONCRETE PROTOTYPE: Archer
    // =========================================================================

    // [ConcretePrototype C] — same pattern; arrowCount is the extra field
    static class Archer extends GameCharacter {

        int arrowCount;

        Archer(String name, int level, int arrowCount) {
            super(name, level);
            this.arrowCount = arrowCount;
        }

        private Archer(Archer source) {
            super(source);
            this.arrowCount = source.arrowCount;
        }

        // [Covariant return] — Archer, not GameCharacter
        @Override
        public Archer clone() {
            return new Archer(this);
        }

        public int  getArrowCount()      { return arrowCount; }
        public void setArrowCount(int v) { this.arrowCount = v; }

        // [%d not %s] — use the correct format specifier for an int field
        @Override
        public String toString() {
            return String.format("Archer{name='%s', level=%d, arrowCount=%d, inventory=%s}",
                    name, level, arrowCount, inventory);
        }
    }

    // =========================================================================
    // REFERENCE MAIN — same 12 tests as practice + Test 13 (most common mistake)
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
                t10 = false;
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

        // ── Extra Test 13: setter actually mutates the field (most common mistake) ────────
        // A setter that validates but forgets `this.field = v;` is silently broken.
        // setArmor(800) must be observable via getArmor() afterwards.
        System.out.println("\n═══ Test 13 (extra): setter actually mutates the field ══════");
        Warrior ws = new Warrior("Setter Test", 10, 100);
        ws.setArmor(500);
        boolean t13armor = ws.getArmor() == 500;
        Mage ms = new Mage("Mage Test", 10, 200);
        ms.setManaPool(999);
        boolean t13mana = ms.getManaPool() == 999;
        Archer as = new Archer("Archer Test", 10, 50);
        as.setArrowCount(75);
        boolean t13arrow = as.getArrowCount() == 75;
        System.out.println("setArmor(500)      → getArmor()      = " + ws.getArmor()   + (t13armor ? " PASSED" : " FAILED"));
        System.out.println("setManaPool(999)   → getManaPool()   = " + ms.getManaPool() + (t13mana  ? " PASSED" : " FAILED"));
        System.out.println("setArrowCount(75)  → getArrowCount() = " + as.getArrowCount() + (t13arrow ? " PASSED" : " FAILED"));
        System.out.println("Test 13 " + ((t13armor && t13mana && t13arrow) ? "PASSED" : "FAILED — setter does not assign"));
    }
}

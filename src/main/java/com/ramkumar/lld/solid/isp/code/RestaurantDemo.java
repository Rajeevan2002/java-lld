package com.ramkumar.lld.solid.isp.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scenario A: Restaurant Staff System
 *
 * Demonstrates the Interface Segregation Principle (ISP).
 *
 * STEP 1 — VIOLATION: FatRestaurantWorker
 *   One monolithic interface forces Chef, Waiter, and Cashier to implement
 *   every method — even methods that don't apply to them.
 *   Chef.serve() and Chef.processPayment() throw UnsupportedOperationException.
 *   This is the ISP violation: a client is forced to depend on methods it doesn't use.
 *
 * STEP 2 — FIX: Role interfaces
 *   Cookable, Servable, Cleanable, InventoryManageable, CashierOperable.
 *   Each class implements only the roles it fills.
 *   RestaurantService uses typed lists — no instanceof, no casts.
 *
 * Key ISP concepts illustrated:
 *   - "Fat interface" forces throws for unused operations
 *   - Role interface = one interface, one client need
 *   - Multiple interfaces per class is the solution, not the problem
 *   - instanceof in the caller is a red flag that ISP is violated
 */
public class RestaurantDemo {

    // =========================================================================
    // ❌  STEP 1 — VIOLATION: Fat interface
    // =========================================================================

    /**
     * ❌ Fat interface — every restaurant worker must implement ALL operations,
     * even operations that don't apply to their role.
     *
     * Chef is forced to implement serve() and processPayment().
     * Waiter is forced to implement cook() and checkInventory().
     * Both will throw UnsupportedOperationException — ISP VIOLATED.
     */
    interface FatRestaurantWorker {
        void cook(String dish);                        // Chef only
        void serve(String order, String table);        // Waiter / Manager
        void clean(String area);                       // Everyone
        int  checkInventory(String item);              // Manager only
        void reorderItem(String item, int qty);        // Manager only
        double processPayment(double amount);          // Cashier / Manager
        void openCashRegister();                       // Cashier / Manager
    }

    /**
     * ❌ Chef forced to implement serve() and processPayment() — ISP VIOLATED.
     * Any code that calls violatingChef.serve() will throw at runtime.
     */
    static class ViolatingChef implements FatRestaurantWorker {

        @Override
        public void cook(String dish) {
            System.out.println("[Chef] Cooking: " + dish);   // ✅ genuinely does this
        }

        @Override
        public void clean(String area) {
            System.out.println("[Chef] Cleaning: " + area);   // ✅ genuinely does this
        }

        @Override
        public void serve(String order, String table) {
            // ← ISP VIOLATED: Chef doesn't serve — but the interface forces this
            throw new UnsupportedOperationException("Chef cannot serve tables");
        }

        @Override
        public int checkInventory(String item) {
            throw new UnsupportedOperationException("Chef cannot check inventory");
        }

        @Override
        public void reorderItem(String item, int qty) {
            throw new UnsupportedOperationException("Chef cannot reorder items");
        }

        @Override
        public double processPayment(double amount) {
            // ← ISP VIOLATED: Chef doesn't handle payments — but the interface forces this
            throw new UnsupportedOperationException("Chef cannot process payments");
        }

        @Override
        public void openCashRegister() {
            throw new UnsupportedOperationException("Chef cannot open cash register");
        }
    }

    /**
     * The caller must use instanceof to avoid the UnsupportedOperationException.
     * instanceof in the caller is the clearest signal that the interface is too fat.
     */
    static void violatingKitchenOrder(FatRestaurantWorker worker, String dish) {
        // ← ISP VIOLATION signal: caller must check type before calling cook()
        if (worker instanceof ViolatingChef) {
            worker.cook(dish);
        } else {
            // Only Chefs can cook — but the interface says all workers can
            System.out.println("This worker cannot cook!");
        }
    }

    // =========================================================================
    // ✅  STEP 2 — FIX: Role interfaces
    // =========================================================================

    // Each interface represents exactly ONE capability (one client need).
    // Classes implement only the interfaces for roles they genuinely fill.

    /** Role 1: Cooking capability */
    interface Cookable {
        void cook(String dish);
    }

    /** Role 2: Table service capability */
    interface Servable {
        void serve(String order, String table);
    }

    /** Role 3: Cleaning capability */
    interface Cleanable {
        void clean(String area);
    }

    /** Role 4: Inventory management capability (two related methods — grouped logically) */
    interface InventoryManageable {
        int  checkInventory(String item);
        void reorderItem(String item, int qty);
    }

    /** Role 5: Cashier capability */
    interface CashierOperable {
        double processPayment(double amount);
        void   openCashRegister();
    }

    // ── Concrete classes implement ONLY the roles they actually fill ──────────

    /**
     * ✅ Chef: cooks and cleans. Does NOT implement Servable or CashierOperable.
     * No UnsupportedOperationException anywhere — ISP satisfied.
     */
    static class Chef implements Cookable, Cleanable {

        private final String name;

        public Chef(String name) { this.name = name; }

        @Override
        public void cook(String dish) {
            System.out.println("[Chef:" + name + "] Cooking: " + dish);
        }

        @Override
        public void clean(String area) {
            System.out.println("[Chef:" + name + "] Cleaning: " + area);
        }
    }

    /**
     * ✅ Waiter: serves and cleans. Does NOT implement Cookable or CashierOperable.
     */
    static class Waiter implements Servable, Cleanable {

        private final String name;

        public Waiter(String name) { this.name = name; }

        @Override
        public void serve(String order, String table) {
            System.out.println("[Waiter:" + name + "] Serving \"" + order + "\" to " + table);
        }

        @Override
        public void clean(String area) {
            System.out.println("[Waiter:" + name + "] Cleaning: " + area);
        }
    }

    /**
     * ✅ Manager: handles floor service, inventory, and payments.
     * Implements multiple role interfaces — this is ISP's intended pattern,
     * not a violation. Manager genuinely fills all these roles.
     */
    static class Manager implements Servable, Cleanable, InventoryManageable, CashierOperable {

        private final String name;

        public Manager(String name) { this.name = name; }

        @Override
        public void serve(String order, String table) {
            System.out.println("[Manager:" + name + "] Assisting with \"" + order + "\" at " + table);
        }

        @Override
        public void clean(String area) {
            System.out.println("[Manager:" + name + "] Overseeing clean-up of: " + area);
        }

        @Override
        public int checkInventory(String item) {
            int stock = 42;   // simulated
            System.out.println("[Manager:" + name + "] Inventory for \"" + item + "\": " + stock);
            return stock;
        }

        @Override
        public void reorderItem(String item, int qty) {
            System.out.println("[Manager:" + name + "] Reordering " + qty + "x " + item);
        }

        @Override
        public double processPayment(double amount) {
            System.out.printf("[Manager:%s] Processing payment: ₹%.2f%n", name, amount);
            return amount;   // simulated success
        }

        @Override
        public void openCashRegister() {
            System.out.println("[Manager:" + name + "] Opening cash register.");
        }
    }

    // =========================================================================
    // RestaurantService — ISP-clean orchestrator
    // Uses role-typed lists — NO instanceof, NO casts, NO fat interface
    // =========================================================================

    /**
     * ✅ RestaurantService depends only on the specific capabilities it needs.
     * Each list holds a role interface, not a concrete type.
     * Adding a new staff role (e.g., Sommelier implements Servable) requires
     * zero changes here — OCP + ISP working together.
     */
    static class RestaurantService {

        // Each list is typed to the exact capability needed — no fat dependency
        private final List<Cookable>            kitchenStaff  = new ArrayList<>();
        private final List<Servable>            floorStaff    = new ArrayList<>();
        private final List<Cleanable>           cleaningStaff = new ArrayList<>();
        private final List<InventoryManageable> inventoryMgrs = new ArrayList<>();
        private final List<CashierOperable>     cashiers      = new ArrayList<>();

        public void addKitchenStaff(Cookable c)            { kitchenStaff.add(c); }
        public void addFloorStaff(Servable s)              { floorStaff.add(s); }
        public void addCleaningStaff(Cleanable c)          { cleaningStaff.add(c); }
        public void addInventoryManager(InventoryManageable m) { inventoryMgrs.add(m); }
        public void addCashier(CashierOperable o)          { cashiers.add(o); }

        /** Sends a dish to all kitchen staff — no instanceof, pure polymorphism */
        public void placeKitchenOrder(String dish) {
            if (kitchenStaff.isEmpty()) {
                System.out.println("[RestaurantService] No kitchen staff available!");
                return;
            }
            for (Cookable c : kitchenStaff) c.cook(dish);
        }

        /** Dispatches a table order to all floor staff */
        public void dispatchTableOrder(String order, String table) {
            if (floorStaff.isEmpty()) {
                System.out.println("[RestaurantService] No floor staff available!");
                return;
            }
            for (Servable s : floorStaff) s.serve(order, table);
        }

        /** Requests a clean-up from all cleaning staff */
        public void requestCleanup(String area) {
            for (Cleanable c : cleaningStaff) c.clean(area);
        }

        /** Checks stock level via the first available inventory manager */
        public int checkStock(String item) {
            if (inventoryMgrs.isEmpty()) return -1;
            return inventoryMgrs.get(0).checkInventory(item);
        }

        /** Processes a bill at the first available cashier */
        public double processBill(double amount) {
            if (cashiers.isEmpty()) {
                System.out.println("[RestaurantService] No cashier available!");
                return -1;
            }
            return cashiers.get(0).processPayment(amount);
        }

        /** Returns staff counts per role as a summary string */
        public String getStaffSummary() {
            return String.format(
                "Kitchen=%d, Floor=%d, Cleaning=%d, Inventory=%d, Cashiers=%d",
                kitchenStaff.size(), floorStaff.size(), cleaningStaff.size(),
                inventoryMgrs.size(), cashiers.size());
        }
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" ISP Demo: Restaurant Staff System");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ── STEP 1: Show the violation ────────────────────────────────────────
        System.out.println("── VIOLATION: FatRestaurantWorker ──────────────────────");

        ViolatingChef badChef = new ViolatingChef();
        badChef.cook("Pasta");     // ✅ works
        badChef.clean("Kitchen");  // ✅ works

        System.out.println("\nCalling badChef.serve() — THROWS:");
        try {
            badChef.serve("Pasta", "Table 3");
        } catch (UnsupportedOperationException e) {
            System.out.println("❌ UnsupportedOperationException: " + e.getMessage());
            System.out.println("   (This means the interface is TOO FAT — ISP VIOLATED)");
        }

        System.out.println("\nCalling badChef.processPayment() — THROWS:");
        try {
            badChef.processPayment(250.0);
        } catch (UnsupportedOperationException e) {
            System.out.println("❌ UnsupportedOperationException: " + e.getMessage());
        }

        // ── STEP 2: Show the fix ──────────────────────────────────────────────
        System.out.println("\n── FIX: Role interfaces ──────────────────────────────");

        Chef    chef    = new Chef("Gordon");
        Waiter  waiter  = new Waiter("Alice");
        Manager manager = new Manager("Bob");

        RestaurantService service = new RestaurantService();

        // Register each staff member to only the roles they fill
        service.addKitchenStaff(chef);           // Chef is Cookable
        service.addCleaningStaff(chef);          // Chef is also Cleanable
        service.addFloorStaff(waiter);           // Waiter is Servable
        service.addCleaningStaff(waiter);        // Waiter is also Cleanable
        service.addFloorStaff(manager);          // Manager is Servable
        service.addCleaningStaff(manager);       // Manager is Cleanable
        service.addInventoryManager(manager);    // Manager is InventoryManageable
        service.addCashier(manager);             // Manager is CashierOperable

        System.out.println("\nStaff summary: " + service.getStaffSummary());

        System.out.println("\n[Kitchen orders]");
        service.placeKitchenOrder("Risotto");
        service.placeKitchenOrder("Tiramisu");

        System.out.println("\n[Table service]");
        service.dispatchTableOrder("Risotto", "Table 5");
        service.dispatchTableOrder("Tiramisu", "Table 5");

        System.out.println("\n[Cleanup]");
        service.requestCleanup("Main dining area");

        System.out.println("\n[Inventory check]");
        int stock = service.checkStock("Arborio rice");
        System.out.println("Arborio rice stock: " + stock);

        System.out.println("\n[Payment]");
        double paid = service.processBill(1250.50);
        System.out.printf("Amount settled: ₹%.2f%n", paid);

        // ── STEP 3: ISP in action — OCP bonus ───────────────────────────────
        System.out.println("\n── ISP + OCP: Add Sommelier without changing RestaurantService ─");

        // Sommelier only serves — implements Servable, nothing else
        // Adding a new role type requires ZERO changes to RestaurantService
        Servable sommelier = (order, table) ->
                System.out.println("[Sommelier] Recommending wine for \"" + order + "\" at " + table);

        service.addFloorStaff(sommelier);
        service.dispatchTableOrder("Dinner menu", "Table 2");

        System.out.println("\n── Key ISP takeaway ──────────────────────────────────");
        System.out.println("✅ Chef: implements Cookable + Cleanable only (2 interfaces)");
        System.out.println("✅ Waiter: implements Servable + Cleanable only (2 interfaces)");
        System.out.println("✅ Manager: implements Servable + Cleanable + InventoryManageable + CashierOperable");
        System.out.println("✅ RestaurantService: uses typed capability lists — NO instanceof anywhere");
        System.out.println("✅ Adding Sommelier: zero changes to existing code — OCP + ISP");
    }
}

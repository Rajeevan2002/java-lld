package com.ramkumar.lld.javareadiness.enums.results;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reference solution — 4.4 Enums as Design Tools (streaming subscription desk).
 */
public class StreamingPlanReference {

    enum Plan {
        // Constant-specific bodies: a 4th plan will not compile until it supplies quote().
        FREE(0.0, 1) {
            @Override double quote(int months) { return 0.0; }
        },
        BASIC(199.0, 2) {
            @Override double quote(int months) { return months * getMonthlyPrice(); }
        },
        PREMIUM(499.0, 4) {
            @Override double quote(int months) {
                double amount = months * getMonthlyPrice();
                return months >= 12 ? amount * 0.85 : amount;   // annual discount on the whole amount
            }
        };

        private final double monthlyPrice;                       // final: constants are shared singletons
        private final int maxScreens;

        Plan(double monthlyPrice, int maxScreens) {
            this.monthlyPrice = monthlyPrice;
            this.maxScreens = maxScreens;
        }

        double getMonthlyPrice() { return monthlyPrice; }
        int getMaxScreens() { return maxScreens; }

        abstract double quote(int months);

        // Built once. Static fields initialise AFTER the constants, so values() is safe here.
        private static final Map<String, Plan> BY_CODE = new HashMap<>();
        static {
            for (Plan p : values()) {
                BY_CODE.put(p.name().toLowerCase(), p);
            }
        }

        static Plan fromCode(String code) {
            Plan p = (code == null) ? null : BY_CODE.get(code.trim().toLowerCase());
            if (p == null) {
                throw new IllegalArgumentException("unknown plan: " + code);
            }
            return p;
        }
    }

    enum SubState {
        TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED;

        // Wired in a static block: constructor args cannot name later constants.
        private static final Map<SubState, Set<SubState>> NEXT = new EnumMap<>(SubState.class);
        static {
            NEXT.put(TRIAL,     EnumSet.of(ACTIVE, CANCELLED, EXPIRED));
            NEXT.put(ACTIVE,    EnumSet.of(PAST_DUE, CANCELLED));
            NEXT.put(PAST_DUE,  EnumSet.of(ACTIVE, CANCELLED, EXPIRED));
            NEXT.put(CANCELLED, EnumSet.noneOf(SubState.class));
            NEXT.put(EXPIRED,   EnumSet.noneOf(SubState.class));
        }

        boolean canMoveTo(SubState target) { return NEXT.get(this).contains(target); }
        boolean isTerminal() { return NEXT.get(this).isEmpty(); }
    }

    static class Subscription {
        private final String id;
        private final String customer;
        private final Plan plan;
        private SubState state = SubState.TRIAL;

        Subscription(String id, String customer, Plan plan) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (customer == null || customer.isBlank()) throw new IllegalArgumentException("customer must not be blank");
            if (plan == null) throw new IllegalArgumentException("plan must not be null");
            this.id = id;
            this.customer = customer;
            this.plan = plan;
        }

        String getId() { return id; }
        String getCustomer() { return customer; }
        Plan getPlan() { return plan; }
        SubState getState() { return state; }

        void moveTo(SubState target) {
            if (target == null) throw new IllegalArgumentException("target must not be null");
            if (!state.canMoveTo(target)) {                      // the enum owns the rule
                throw new IllegalStateException("illegal transition: " + state + " -> " + target);
            }
            state = target;
        }

        @Override
        public String toString() {
            // Plain concatenation: String.format(userText) would break on a '%' in the customer name.
            return id + "[" + customer + ", " + plan + ", " + state + "]";
        }
    }

    static class SubscriptionDesk {
        private int counter;                                          // per instance, NOT static
        private final Map<String, Subscription> byId = new LinkedHashMap<>();   // insertion order

        Subscription open(String customer, Plan plan) {
            if (customer == null || customer.isBlank()) throw new IllegalArgumentException("customer must not be blank");
            if (plan == null) throw new IllegalArgumentException("plan must not be null");
            // Validation is done above, so a failure can never burn an id.
            Subscription s = new Subscription(String.format("SUB-%03d", ++counter), customer, plan);
            byId.put(s.getId(), s);
            return s;
        }

        Optional<Subscription> find(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        void move(String id, SubState target) {
            Subscription s = byId.get(id);
            if (s == null) throw new IllegalArgumentException("unknown subscription: " + id);
            s.moveTo(target);
        }

        Map<SubState, Integer> countByState() {
            Map<SubState, Integer> counts = new EnumMap<>(SubState.class);
            for (SubState st : SubState.values()) counts.put(st, 0);      // zero-fill: every key present
            for (Subscription s : byId.values()) counts.merge(s.getState(), 1, Integer::sum);
            return counts;
        }

        Set<Plan> plansInUse() {
            Set<Plan> plans = EnumSet.noneOf(Plan.class);                  // declaration-order iteration
            for (Subscription s : byId.values()) {
                if (!s.getState().isTerminal()) plans.add(s.getPlan());
            }
            return plans;
        }

        double quote(String planCode, int months) {
            if (months < 1) throw new IllegalArgumentException("months must be >= 1");   // before the lookup
            return Plan.fromCode(planCode).quote(months);
        }
    }

    public static void main(String[] args) {
        System.out.println("-- Test 1");
        System.out.println(Plan.PREMIUM.getMaxScreens());                  // 4
        System.out.println(Plan.BASIC.getMonthlyPrice());                  // 199.0
        System.out.println(java.util.Arrays.toString(Plan.values()));      // [FREE, BASIC, PREMIUM]

        System.out.println("-- Test 2");
        System.out.printf("%.2f%n", Plan.BASIC.quote(3));                  // 597.00
        System.out.printf("%.2f%n", Plan.PREMIUM.quote(6));                // 2994.00
        System.out.printf("%.2f%n", Plan.PREMIUM.quote(12));               // 5089.80
        System.out.printf("%.2f%n", Plan.FREE.quote(7));                   // 0.00

        System.out.println("-- Test 3");
        System.out.println(Plan.fromCode("  Premium "));                   // PREMIUM
        System.out.println(Plan.fromCode("basic"));                        // BASIC
        try { Plan.fromCode("gold"); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { Plan.fromCode(null); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        System.out.println("-- Test 4");
        System.out.println(SubState.TRIAL.canMoveTo(SubState.ACTIVE));     // true
        System.out.println(SubState.TRIAL.canMoveTo(SubState.PAST_DUE));   // false
        System.out.println(SubState.ACTIVE.canMoveTo(SubState.TRIAL));     // false
        System.out.println(SubState.PAST_DUE.canMoveTo(SubState.ACTIVE));  // true
        System.out.println(SubState.CANCELLED.isTerminal());               // true
        System.out.println(SubState.ACTIVE.isTerminal());                  // false

        System.out.println("-- Test 5");
        Subscription s = new Subscription("SUB-001", "alice", Plan.BASIC);
        System.out.println(s);
        s.moveTo(SubState.ACTIVE);
        System.out.println(s.getState());
        try { s.moveTo(SubState.TRIAL); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
        try { new Subscription("S", " ", Plan.FREE); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        System.out.println("-- Test 6");
        SubscriptionDesk d = new SubscriptionDesk();
        System.out.println(d.open("alice", Plan.BASIC));
        System.out.println(d.open("bob", Plan.PREMIUM));
        try { d.open(" ", Plan.BASIC); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        System.out.println(d.open("carol", Plan.FREE));
        System.out.println(d.find("SUB-002").isPresent());
        System.out.println(d.find("SUB-999").isPresent());

        System.out.println("-- Test 7");
        d.move("SUB-001", SubState.ACTIVE);
        d.move("SUB-002", SubState.CANCELLED);
        System.out.println(d.countByState());      // {TRIAL=1, ACTIVE=1, PAST_DUE=0, CANCELLED=1, EXPIRED=0}
        try { d.move("SUB-999", SubState.ACTIVE); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { d.move("SUB-002", SubState.ACTIVE); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }

        System.out.println("-- Test 8");
        System.out.println(d.plansInUse());        // [FREE, BASIC]

        System.out.println("-- Test 9");
        System.out.printf("%.2f%n", d.quote("premium", 12));
        try { d.quote("basic", 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { d.quote("gold", 1); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }

        // Extra: catches the most common mistakes (missing zero keys, unordered set, message typo).
        System.out.println("-- Extra");
        SubscriptionDesk fresh = new SubscriptionDesk();
        System.out.println(fresh.countByState());  // all five states at 0 on an empty desk
        fresh.open("x", Plan.PREMIUM);
        fresh.open("y", Plan.FREE);
        fresh.open("z", Plan.BASIC);
        System.out.println(fresh.plansInUse());    // [FREE, BASIC, PREMIUM] regardless of insertion order
        try { fresh.quote("basic", 0); } catch (IllegalArgumentException e) {
            System.out.println("months must be >= 1".equals(e.getMessage()));   // true
        }
        System.out.println(new Subscription("S-1", "50% off", Plan.FREE));      // no format crash on '%'
    }
}

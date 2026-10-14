package com.ramkumar.lld.javareadiness.enums.practice;

import com.ramkumar.lld.javareadiness.cheatsheet.code.TripDispatchDemo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Practice Exercise — Phase 4 Java Readiness | 4.4 Enums as Design Tools
 * Scenario B: Streaming-service subscription desk
 *
 * ════════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ════════════════════════════════════════════════════════════════════════
 * A video-streaming service sells three plans and tracks every customer
 * subscription through a lifecycle. Model the fixed sets (plans, lifecycle
 * states) as enums that own their data and rules, then build a small desk
 * that creates subscriptions, moves them through the lifecycle and reports.
 *
 * ── enum Plan (constants in this order: FREE, BASIC, PREMIUM) ───────────
 *   Fields (all final):
 *     1. monthlyPrice  (double)  FREE 0.0,  BASIC 199.0,  PREMIUM 499.0
 *     2. maxScreens    (int)     FREE 1,    BASIC 2,      PREMIUM 4
 *   Constructor:
 *     1. One constructor taking (monthlyPrice, maxScreens); stores both.
 *   Methods:
 *     1. getMonthlyPrice(), getMaxScreens()
 *     2. quote(int months) -> double. Every constant supplies its own rule:
 *          FREE    -> 0.0
 *          BASIC   -> months * monthlyPrice
 *          PREMIUM -> months * monthlyPrice, and when months >= 12 the whole
 *                     amount is multiplied by 0.85 (annual discount)
 *        The rule must live in the enum constants (no switch/if on the plan
 *        outside the enum). Adding a 4th plan must force a compile error
 *        until its rule is written.
 *     3. static fromCode(String code) -> Plan. Case-insensitive, surrounding
 *        spaces ignored ("  Premium " -> PREMIUM). null or no match ->
 *        IllegalArgumentException("unknown plan: " + code).
 *        Build the lookup ONCE (not a loop on every call).
 *
 * ── enum SubState (constants in this order: TRIAL, ACTIVE, PAST_DUE,
 *                   CANCELLED, EXPIRED) ───────────────────────────────────
 *   Allowed transitions (anything else is illegal):
 *     TRIAL     -> ACTIVE, CANCELLED, EXPIRED
 *     ACTIVE    -> PAST_DUE, CANCELLED
 *     PAST_DUE  -> ACTIVE, CANCELLED, EXPIRED
 *     CANCELLED -> (none)       EXPIRED -> (none)
 *   Methods:
 *     1. canMoveTo(SubState target) -> boolean
 *     2. isTerminal() -> boolean   (true when no transition is allowed)
 *   The rules live inside the enum. No ordinal(), no if/else chain.
 *
 * ── class Subscription (static nested) ──────────────────────────────────
 *   Fields:
 *     1. id        (String)   final
 *     2. customer  (String)   final
 *     3. plan      (Plan)     final
 *     4. state     (SubState) mutable; starts as TRIAL
 *   Constructor Subscription(String id, String customer, Plan plan):
 *     1. id null/blank       -> IllegalArgumentException("id must not be blank")
 *     2. customer null/blank -> IllegalArgumentException("customer must not be blank")
 *     3. plan null           -> IllegalArgumentException("plan must not be null")
 *        Validate in that order; assign fields only after all checks pass.
 *   Methods:
 *     1. getId(), getCustomer(), getPlan(), getState()
 *     2. moveTo(SubState target):
 *          target null -> IllegalArgumentException("target must not be null")
 *          not allowed -> IllegalStateException("illegal transition: ACTIVE -> TRIAL")
 *                         (current -> target, using the constant names)
 *          otherwise   -> state becomes target
 *     3. toString() -> "SUB-001[alice, BASIC, TRIAL]"  (id[customer, plan, state])
 *
 * ── class SubscriptionDesk (static nested) ──────────────────────────────
 *   Fields:
 *     1. a per-instance id counter (NOT static)
 *     2. storage of subscriptions by id that keeps insertion order
 *   Methods:
 *     1. open(String customer, Plan plan) -> Subscription
 *          customer null/blank -> IllegalArgumentException("customer must not be blank")
 *          plan null           -> IllegalArgumentException("plan must not be null")
 *          Id format "SUB-%03d" from the counter. A failed open() must NOT
 *          consume an id (the next successful open gets the next number).
 *     2. find(String id) -> Optional<Subscription>   (empty when unknown)
 *     3. move(String id, SubState target) -> void
 *          unknown id -> IllegalArgumentException("unknown subscription: " + id)
 *          otherwise delegate to Subscription.moveTo
 *     4. countByState() -> Map<SubState, Integer>
 *          Every SubState is present (zero-filled). Iteration/printing order
 *          is the declaration order of SubState.
 *     5. plansInUse() -> Set<Plan>
 *          Plans of subscriptions whose state is NOT terminal. Iteration
 *          order is the declaration order of Plan.
 *     6. quote(String planCode, int months) -> double
 *          months < 1 -> IllegalArgumentException("months must be >= 1")
 *          (checked BEFORE the plan code), then Plan.fromCode(...).quote(months)
 *
 * ── Design constraints ──────────────────────────────────────────────────
 *   - No String or int constants standing in for plans or states.
 *   - No ordinal() anywhere; no default branch in any switch over an enum.
 *   - Use the enum-specialised collections for countByState / plansInUse /
 *     transition tables (not HashMap / HashSet).
 *   - Enum instance fields must be final.
 */
public class StreamingPlanPractice {

    // ── TODO 1: Declare enum Plan constants and fields (see "enum Plan" above)
    //    Constants in order: FREE(0.0, 1), BASIC(199.0, 2), PREMIUM(499.0, 4)
    //    Two fields, both private final: double monthlyPrice, int maxScreens
    //    Semicolon after the last constant
    //    Private constructor taking (monthlyPrice, maxScreens) that stores both
    //    Getters getMonthlyPrice() and getMaxScreens()
    //    Each constant provides its own body for the quote rule (TODO 2)
    //    Static lookup for fromCode (TODO 3)
    enum Plan {
        FREE(0.0, 1){
            @Override double quote(int months){ return 0.0; }
        },
        BASIC(199.0, 2){
            @Override double quote(int months) { return months * getMonthlyPrice() ; }
        },
        PREMIUM(499.0, 4) {
            @Override double quote(int months) {
                if (months >= 12) {
                    return 0.85 * months * getMonthlyPrice();
                }
                return months * getMonthlyPrice();
            }
        };


        private final double monthlyPrice;
        private final int maxScreens;

        Plan(double monthlyPrice, int maxScreens){
            this.monthlyPrice = monthlyPrice;
            this.maxScreens = maxScreens;
        }

        public double getMonthlyPrice() {
            return monthlyPrice;
        }

        public int getMaxScreens() {
            return maxScreens;
        }

        private static final Map<String, Plan> BY_CODE = new HashMap<>();
        static {
            for(Plan p : values()){
                BY_CODE.put(p.name().toLowerCase() , p);
            }
        }

        static Plan fromCode(String s){
            Plan p = ( s==null ) ? null : BY_CODE.get(s.trim().toLowerCase());
            if(p == null) {
                throw new IllegalArgumentException("unknown plan: " + s);
            }
            return p;
        }

        abstract double quote(int months);
    }

    // ── TODO 2: Plan.quote(int months) → double
    //    A method every constant must implement itself (compiler-enforced),
    //    declared once in the enum and given a body inside each constant
    //    FREE    → 0.0
    //    BASIC   → months * monthlyPrice
    //    PREMIUM → months * monthlyPrice; if months >= 12, multiply the whole amount by 0.85
    //    No validation here (the desk checks months)

    // ── TODO 3: Plan.fromCode(String code) → Plan  (static)
    //    Case-insensitive; leading/trailing spaces ignored
    //    code == null or no match → throw IllegalArgumentException("unknown plan: " + code)
    //    The code→constant lookup is built ONCE when the enum loads, not per call
    //    (remember: static fields are initialised after the constants)

    // ── TODO 4: Declare enum SubState — constants in order:
    //    TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED
    //    Transition table:
    //      TRIAL → ACTIVE, CANCELLED, EXPIRED
    //      ACTIVE → PAST_DUE, CANCELLED
    //      PAST_DUE → ACTIVE, CANCELLED, EXPIRED
    //      CANCELLED → nothing        EXPIRED → nothing
    //    Constants cannot name constants declared after them in their own
    //    constructor arguments, so wire the table after all constants exist
    //    canMoveTo(SubState target) → boolean
    //    isTerminal() → boolean (no transition allowed from this state)
    //    No ordinal(), no if/else chain, no default branch
    enum SubState {
        TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED;

        private static final Map<SubState, EnumSet<SubState>> NEXT = new EnumMap<>(SubState.class);
         static {
             NEXT.put(TRIAL ,EnumSet.of(ACTIVE, CANCELLED, EXPIRED));
             NEXT.put(ACTIVE, EnumSet.of(PAST_DUE, CANCELLED));
             NEXT.put(PAST_DUE, EnumSet.of(ACTIVE, CANCELLED, EXPIRED));
             NEXT.put(CANCELLED, EnumSet.noneOf(SubState.class));
             NEXT.put(EXPIRED, EnumSet.noneOf(SubState.class));
         }

         public boolean canMoveTo(SubState target) {
             if(target == null ) throw new IllegalArgumentException("Target should not be null");
             return NEXT.get(this).contains(target);
         }

         public boolean isTerminal() {
             return NEXT.get(this).isEmpty();
         }

    }

    // ── TODO 5: Subscription — fields and constructor
    //    Fields: String id, String customer, Plan plan (all final);
    //            SubState state (mutable, starts as TRIAL)
    //    Constructor (String id, String customer, Plan plan):
    //      id null/blank       → IllegalArgumentException("id must not be blank")
    //      customer null/blank → IllegalArgumentException("customer must not be blank")
    //      plan null           → IllegalArgumentException("plan must not be null")
    //      Validate in that order; assign only after every check passes
    static class Subscription {

        private final String id;
        private final String customer;
        private final Plan plan;
        private SubState state = SubState.TRIAL;

        public Subscription(String id, String customer, Plan plan) {
            if(id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            if(customer == null || customer.isBlank()) {
                throw new IllegalArgumentException("customer must not be blank");
            }
            if(plan == null){
                throw new IllegalArgumentException("plan must not be null");
            }

            this.id = id;
            this.customer = customer;
            this.plan = plan;
        }
        // ── TODO 6: getId(), getCustomer(), getPlan(), getState()
        //    Plain getters
        public String getId() { return this.id; }
        public String getCustomer() { return this.customer; }
        public Plan getPlan() { return this.plan;}
        public SubState getState() { return this.state; }

        // ── TODO 7: moveTo(SubState target) → void
        //    target == null → IllegalArgumentException("target must not be null")
        //    transition not allowed by the enum → IllegalStateException with message
        //      "illegal transition: " + current + " -> " + target   (constant names)
        //    otherwise state becomes target
        //    Ask the enum whether the move is allowed; do not re-implement the rules here
        public void moveTo(SubState target){
            if(target == null) throw new IllegalArgumentException("target must not be null");
            boolean stateTransition = state.canMoveTo(target);
            if(!stateTransition) throw new IllegalStateException("illegal transition: " + state.name() + " -> " + target.name());
            state = target;
        }

        // ── TODO 8: toString() → "SUB-001[alice, BASIC, TRIAL]"
        //    Format: id + "[" + customer + ", " + plan + ", " + state + "]"
        public String toString() {
            return String.format(id + "[" + customer + ", " + plan.toString() +", " + state + "]");
        }
    }

    // ── TODO 9: SubscriptionDesk — fields
    //    A per-instance id counter (NOT static) and storage of Subscription by id
    //    that preserves insertion order (key = id)
    static class SubscriptionDesk {

        // ── TODO 10: open(String customer, Plan plan) → Subscription
        //    customer null/blank → IllegalArgumentException("customer must not be blank")
        //    plan null           → IllegalArgumentException("plan must not be null")
        //    Id format: String.format("SUB-%03d", counter after incrementing)
        //    A failed open() must NOT burn an id: validate before touching the counter
        //    Store the new Subscription by id and return it
        private final Map<String, Subscription> subscriptionStore = new HashMap<>();
        private int counter = 0;

        public Subscription open(String customer, Plan plan){
            if(customer == null  || customer.isBlank())
                throw new IllegalArgumentException("customer must not be blank");
            if(plan == null)
                throw new IllegalArgumentException("plan must not be null");
            String id = String.format("SUB-%03d", counter + 1);
            Subscription subscription = new Subscription(id, customer, plan);
            subscriptionStore.put(id, subscription);
            counter += 1;
            return subscription;
        }

        // ── TODO 11: find(String id) → Optional<Subscription>
        //    Empty when the id is unknown; never returns null
        public Optional<Subscription> find(String id) {
            return Optional.ofNullable(subscriptionStore.get(id));
        }

        // ── TODO 12: move(String id, SubState target) → void
        //    Unknown id → IllegalArgumentException("unknown subscription: " + id)
        //    Otherwise delegate to the subscription's own transition method
        public void move(String id, SubState target){
            Subscription subscription = subscriptionStore.get(id);
            if(subscription == null) throw new IllegalArgumentException("unknown subscription: " + id);
            subscription.moveTo(target);
        }
        // ── TODO 13: countByState() → Map<SubState, Integer>
        //    Every SubState appears as a key, zero when nobody is in it
        //    Printing the map shows keys in SubState declaration order
        //    Use the enum-specialised map, not HashMap
        public EnumMap<SubState, Integer> countByState(){
            return subscriptionStore.values().stream()
                    .collect(Collectors.groupingBy(
                            Subscription::getState,
                            () -> new EnumMap<>(SubState.class),
                            Collectors.summingInt(e -> 1) // Maps every element to 1 and sums them up into an Integer
                    ));
        }

        // ── TODO 14: plansInUse() → Set<Plan>
        //    Plans of subscriptions whose state is NOT terminal
        //    Printing the set shows Plan declaration order
        //    Use the enum-specialised set, not HashSet
        public Set<Plan> plansInUse() {
            return subscriptionStore.values().stream().filter(subscription -> !subscription.state.isTerminal()).map(Subscription::getPlan).collect(Collectors.toSet());
        }

        // ── TODO 15: quote(String planCode, int months) → double
        //    months < 1 → IllegalArgumentException("months must be >= 1")  (checked FIRST)
        //    then resolve the plan with Plan.fromCode and return its quote for months
        public double quote(String planCode, int months){
            if(months < 1) throw new IllegalArgumentException("months must be >=1");
            return Plan.fromCode(planCode).quote(months);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DO NOT MODIFY
    // ══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: plan data (uncomment after TODO 1) ───────────────────────
         System.out.println(Plan.PREMIUM.getMaxScreens());     // expected: 4
         System.out.println(Plan.BASIC.getMonthlyPrice());     // expected: 199.0
         System.out.println(java.util.Arrays.toString(Plan.values()));   // expected: [FREE, BASIC, PREMIUM]

        // ── Test 2: constant-specific quote (uncomment after TODO 2) ─────────
         System.out.printf("%.2f%n", Plan.BASIC.quote(3));     // expected: 597.00
         System.out.printf("%.2f%n", Plan.PREMIUM.quote(6));   // expected: 2994.00
         System.out.printf("%.2f%n", Plan.PREMIUM.quote(12));  // expected: 5089.80
         System.out.printf("%.2f%n", Plan.FREE.quote(7));      // expected: 0.00

        // ── Test 3: fromCode (uncomment after TODO 3) ────────────────────────
         System.out.println(Plan.fromCode("  Premium "));      // expected: PREMIUM
         System.out.println(Plan.fromCode("basic"));           // expected: BASIC
         try { Plan.fromCode("gold"); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: unknown plan: gold
         try { Plan.fromCode(null); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }     // expected: unknown plan: null

        // ── Test 4: state machine rules (uncomment after TODO 4) ─────────────
         System.out.println(SubState.TRIAL.canMoveTo(SubState.ACTIVE));       // expected: true
         System.out.println(SubState.TRIAL.canMoveTo(SubState.PAST_DUE));     // expected: false
         System.out.println(SubState.ACTIVE.canMoveTo(SubState.TRIAL));       // expected: false
         System.out.println(SubState.PAST_DUE.canMoveTo(SubState.ACTIVE));    // expected: true
         System.out.println(SubState.CANCELLED.isTerminal());                 // expected: true
         System.out.println(SubState.ACTIVE.isTerminal());                    // expected: false

        // ── Test 5: Subscription (uncomment after TODOs 5–8) ─────────────────
         Subscription s = new Subscription("SUB-001", "alice", Plan.BASIC);
         System.out.println(s);                                // expected: SUB-001[alice, BASIC, TRIAL]
         s.moveTo(SubState.ACTIVE);
         System.out.println(s.getState());                     // expected: ACTIVE
         try { s.moveTo(SubState.TRIAL); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }   // expected: illegal transition: ACTIVE -> TRIAL
         try { new Subscription("S", " ", Plan.FREE); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: customer must not be blank

        // ── Test 6: desk open + no burned ids (uncomment after TODOs 9–11) ───
         SubscriptionDesk d = new SubscriptionDesk();
         System.out.println(d.open("alice", Plan.BASIC));      // expected: SUB-001[alice, BASIC, TRIAL]
         System.out.println(d.open("bob", Plan.PREMIUM));      // expected: SUB-002[bob, PREMIUM, TRIAL]
         try { d.open(" ", Plan.BASIC); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: customer must not be blank
         System.out.println(d.open("carol", Plan.FREE));       // expected: SUB-003[carol, FREE, TRIAL]
         System.out.println(d.find("SUB-002").isPresent());    // expected: true
         System.out.println(d.find("SUB-999").isPresent());    // expected: false

        // ── Test 7: move + countByState (uncomment after TODOs 12–13) ────────
         d.move("SUB-001", SubState.ACTIVE);
         d.move("SUB-002", SubState.CANCELLED);
         System.out.println(d.countByState());                 // expected: {TRIAL=1, ACTIVE=1, PAST_DUE=0, CANCELLED=1, EXPIRED=0}
         try { d.move("SUB-999", SubState.ACTIVE); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: unknown subscription: SUB-999
         try { d.move("SUB-002", SubState.ACTIVE); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }     // expected: illegal transition: CANCELLED -> ACTIVE

        // ── Test 8: plansInUse (uncomment after TODO 14) ─────────────────────
         System.out.println(d.plansInUse());                   // expected: [FREE, BASIC]

        // ── Test 9: desk quote (uncomment after TODO 15) ─────────────────────
         System.out.printf("%.2f%n", d.quote("premium", 12));  // expected: 5089.80
         try { d.quote("basic", 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }   // expected: months must be >= 1
         try { d.quote("gold", 1); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }    // expected: unknown plan: gold
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════
 * HINTS — read one level at a time
 * ════════════════════════════════════════════════════════════════════════
 *
 * HINT 1 (Gentle)
 *   An enum is a class with a closed set of instances. It can hold data,
 *   and each instance can even behave differently. If a rule belongs to
 *   "which plan" or "which state", ask whether it should live inside the
 *   enum rather than in the code that uses it.
 *   Also ask: when a collection's keys are always the same small fixed set,
 *   is there a collection built for that?
 *
 * HINT 2 (Direct)
 *   - Plan.quote: declare an abstract method in the enum and give each
 *     constant a body in braces (constant-specific class body).
 *   - fromCode: a private static Map filled in a static block that loops
 *     over values(). Static fields are initialised after the constants.
 *   - SubState transitions: a static EnumMap of EnumSet filled in a static
 *     block (constructor arguments cannot name later constants).
 *   - countByState: EnumMap zero-filled by looping over values(), then merge.
 *   - plansInUse: EnumSet.noneOf(Plan.class) then add for non-terminal states.
 *   - find: Optional.ofNullable(map.get(id)).
 *   - Keep the subscriptions in a LinkedHashMap.
 *
 * HINT 3 (Near-solution) — structure only
 *   enum Plan {
 *       FREE(...) { quote body },
 *       BASIC(...) { quote body },
 *       PREMIUM(...) { quote body };
 *       private final fields
 *       private Plan(price, screens) { ... }
 *       abstract double quote(int months);
 *       private static final Map<String, Plan> BY_CODE = ...;
 *       static { fill BY_CODE from values() }
 *       static Plan fromCode(String code) { ... }
 *   }
 *   enum SubState {
 *       TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED;
 *       private static final Map<SubState, Set<SubState>> NEXT = ...;
 *       static { one put per state }
 *       boolean canMoveTo(SubState t) { ... }
 *       boolean isTerminal() { ... }
 *   }
 *   static class Subscription {
 *       fields; constructor(id, customer, plan) { validate in order; assign }
 *       getters; void moveTo(SubState t) { ... }   toString() { ... }
 *   }
 *   static class SubscriptionDesk {
 *       private int counter;  private final Map<String, Subscription> byId;
 *       Subscription open(...) { validate; ++counter; create; store; return }
 *       Optional<Subscription> find(String id) { ... }
 *       void move(String id, SubState t) { ... }
 *       Map<SubState, Integer> countByState() { ... }
 *       Set<Plan> plansInUse() { ... }
 *       double quote(String code, int months) { ... }
 *   }
 */

package com.ramkumar.lld.designpatterns.behavioral.mediator.practice;

import java.util.ArrayList;
import java.util.List;

/**
 * Practice Exercise — Mediator Pattern: Auction House
 *
 * <p><b>Scenario B — Many-to-many coordination through a shared auction mediator</b>
 *
 * <p>Bidders compete in an auction. No bidder holds a reference to any other bidder —
 * all bids and notifications travel through the {@code AuctionHouse} mediator. The mediator
 * tracks the highest bid, rejects lower bids, and announces the winner at close.
 *
 * <p><b>Class hierarchy (do not change the names):</b>
 * <pre>
 *   AuctionMediator   [Mediator interface]       ← TODO 1
 *   AuctionHouse      [ConcreteMediator]         ← TODO 2–5
 *   Bidder            [AbstractParticipant]      ← TODO 6
 *   HumanBidder       [ConcreteParticipant]      ← TODO 7
 * </pre>
 *
 * <p><b>AuctionMediator interface (TODO 1):</b>
 * <ul>
 *   <li>{@code void placeBid(Bidder bidder, double amount)}</li>
 *   <li>{@code void register(Bidder bidder)}</li>
 *   <li>{@code void closeAuction()}</li>
 * </ul>
 *
 * <p><b>AuctionHouse (TODO 2–5):</b>
 * <ul>
 *   <li>Fields:
 *     <ul>
 *       <li>{@code private final List<Bidder> bidders = new ArrayList<>()}</li>
 *       <li>{@code private final String item} — the auction item name</li>
 *       <li>{@code private double highestBid = 0.0}</li>
 *       <li>{@code private Bidder highestBidder = null}</li>
 *     </ul>
 *   </li>
 *   <li>Constructor: {@code AuctionHouse(String item)} — stores item</li>
 *   <li>{@code register(Bidder bidder)}: adds bidder to the list;
 *       prints {@code System.out.printf("[AuctionHouse] %s registered for \"%s\"%n", bidder.getName(), item)}</li>
 *   <li>{@code placeBid(Bidder bidder, double amount)}:
 *     <ul>
 *       <li>If {@code amount > highestBid}: update {@code highestBid} and {@code highestBidder};
 *           print {@code System.out.printf("[AuctionHouse] %s leads with $%.2f for \"%s\"%n", bidder.getName(), amount, item)};
 *           then notify every OTHER registered bidder by calling
 *           {@code b.onNewBid(bidder.getName(), amount)} for each {@code b != bidder}</li>
 *       <li>Else: print {@code System.out.printf("[AuctionHouse] %s bid $%.2f rejected — %s leads at $%.2f%n",
 *           bidder.getName(), amount, highestBidder.getName(), highestBid)}</li>
 *     </ul>
 *   </li>
 *   <li>{@code closeAuction()}:
 *       prints {@code System.out.printf("[AuctionHouse] Auction closed! \"%s\" won by %s at $%.2f%n",
 *       item, highestBidder.getName(), highestBid)};
 *       then calls {@code b.onAuctionClosed(highestBidder.getName(), highestBid)} for ALL registered bidders
 *       (including the winner)</li>
 * </ul>
 *
 * <p><b>Bidder abstract class (TODO 6):</b>
 * <ul>
 *   <li>Fields: {@code protected final AuctionMediator mediator}, {@code protected final String name}</li>
 *   <li>Constructor: {@code Bidder(String name, AuctionMediator mediator)}
 *       — stores both fields; calls {@code mediator.register(this)} (self-registration)</li>
 *   <li>{@code getName() → String} — returns name</li>
 *   <li>{@code placeBid(double amount) → void} — calls {@code mediator.placeBid(this, amount)}</li>
 *   <li>{@code abstract void onNewBid(String bidderName, double amount)}
 *       — called by mediator when another bidder outbids</li>
 *   <li>{@code abstract void onAuctionClosed(String winnerName, double amount)}
 *       — called by mediator when auction ends</li>
 * </ul>
 *
 * <p><b>HumanBidder (TODO 7):</b>
 * <ul>
 *   <li>Constructor: {@code HumanBidder(String name, AuctionMediator mediator)} — calls {@code super(name, mediator)}</li>
 *   <li>{@code onNewBid(String bidderName, double amount)}:
 *       {@code System.out.printf("[%s] Outbid by %s at $%.2f%n", getName(), bidderName, amount)}</li>
 *   <li>{@code onAuctionClosed(String winnerName, double amount)}:
 *       {@code System.out.printf("[%s] Auction over — winner: %s at $%.2f%n", getName(), winnerName, amount)}</li>
 * </ul>
 *
 * <p><b>Design constraints:</b>
 * <ul>
 *   <li>{@code Bidder} must hold only a {@code AuctionMediator} reference — never a reference to another {@code Bidder}.</li>
 *   <li>{@code placeBid()} in {@code Bidder} must delegate to the mediator — never call another bidder's method directly.</li>
 *   <li>The bidder who places a bid must NOT receive their own {@code onNewBid()} notification.</li>
 *   <li>{@code closeAuction()} notifies ALL bidders (including the winner) via {@code onAuctionClosed()}.</li>
 *   <li>No {@code instanceof}, no type-checking anywhere.</li>
 * </ul>
 */
public class AuctionHousePractice {

    interface AuctionMediator {
        void placeBid(Bidder bidder, double amount);
        void register(Bidder bidder);
        void closeAuction();
    }

    // ── AuctionHouse ───────────────────────────────────────────────────────────
    static class AuctionHouse implements AuctionMediator {
        private final List<Bidder> bidders = new ArrayList<>();
        private final String item;
        private double highestBid = 0.0;
        private Bidder highestBidder = null;

        public AuctionHouse(String item){
            this.item =  item;
        }

        public void register(Bidder bidder) {
            bidders.add(bidder);
            System.out.printf("[AuctionHouse] %s registered for \"%s\"%n", bidder.getName(), item);
        }

        @Override
        public void placeBid(Bidder bidder, double amount) {
            if(amount > highestBid){
                highestBid = amount;
                highestBidder = bidder;
                System.out.printf("[AuctionHouse] %s leads with $%.2f for \"%s\"%n", bidder.getName(), amount, item);
                for(Bidder b : bidders){
                    if(b != bidder){
                        b.onNewBid(bidder.getName(), amount);
                    }
                }
            } else {
                System.out.printf("[AuctionHouse] %s bid $%.2f rejected — %s leads at $%.2f%n",
                        bidder.getName(), amount, highestBidder.getName(), highestBid);
            }
        }

        @Override
        public void closeAuction() {
            System.out.printf("[AuctionHouse] Auction closed! \"%s\" won by %s at $%.2f%n",
                                  item, highestBidder.getName(), highestBid);
            for(Bidder b : bidders)
            {
                b.onAuctionClosed(highestBidder.getName(), highestBid);
            }
        }
    }

    // ── Bidder ─────────────────────────────────────────────────────────────────

    static abstract class Bidder {
        protected final AuctionMediator mediator;
        protected final String name;

        public Bidder(String name, AuctionMediator mediator){
            this.name = name;
            this.mediator = mediator;
            mediator.register(this);
        }

        public String getName() {return this.name;}
        public void placeBid(double amount) { mediator.placeBid(this, amount);}
        abstract void onNewBid(String bidderName, double amount);
        abstract void onAuctionClosed(String winnerName, double amount);
    }

    static class HumanBidder extends Bidder {
        HumanBidder(String name, AuctionMediator mediator) {
            super(name, mediator);
        }

        @Override
        void onNewBid(String bidderName, double amount) {
            System.out.printf("[%s] Outbid by %s at $%.2f%n", getName(), bidderName, amount);
        }

        @Override
        void onAuctionClosed(String winnerName, double amount) {
            System.out.printf("[%s] Auction over — winner: %s at $%.2f%n", getName(), winnerName, amount);
        }
    }

    // ── DO NOT MODIFY — test harness ──────────────────────────────────────────
    public static void main(String[] args) {
        // Uncomment each block after implementing the corresponding TODO.

        // ── Test 1: Single bidder — first bid becomes highest (TODO 1–6) ─────────
         AuctionHouse house1 = new AuctionHouse("Vintage Watch");
         Bidder alice = new HumanBidder("Alice", house1);
         alice.placeBid(500.00);
         // expected: [AuctionHouse] Alice leads with $500.00 for "Vintage Watch"

        // ── Test 2: Second bidder outbids first — first is notified (TODO 1–7) ───
         AuctionHouse house2 = new AuctionHouse("Antique Vase");
         Bidder a2 = new HumanBidder("Alice", house2);
         Bidder b2 = new HumanBidder("Bob",   house2);
         a2.placeBid(300.00);
         // expected: [AuctionHouse] Alice leads with $300.00 for "Antique Vase"
         b2.placeBid(450.00);
         // expected: [AuctionHouse] Bob leads with $450.00 for "Antique Vase"
         //           [Alice] Outbid by Bob at $450.00
         // (Bob does NOT receive onNewBid for his own bid)

        // ── Test 3: Lower bid is rejected (TODO 5a) ───────────────────────────────
         AuctionHouse house3 = new AuctionHouse("Oil Painting");
         Bidder a3 = new HumanBidder("Alice", house3);
         Bidder b3 = new HumanBidder("Bob",   house3);
         a3.placeBid(600.00);
         b3.placeBid(400.00);   // lower — rejected
         // expected: [AuctionHouse] Bob bid $400.00 rejected — Alice leads at $600.00

        // ── Test 4: Bidder does NOT receive onNewBid for their own bid (TODO 5a) ──
         AuctionHouse house4 = new AuctionHouse("Sculpture");
         Bidder a4 = new HumanBidder("Alice", house4);
         Bidder b4 = new HumanBidder("Bob",   house4);
         Bidder c4 = new HumanBidder("Carol", house4);
         a4.placeBid(200.00);
         // expected: Alice leads (no onNewBid for Alice herself)
         //           [Bob] Outbid by Alice at $200.00
         //           [Carol] Outbid by Alice at $200.00

        // ── Test 5: closeAuction() notifies ALL bidders including winner (TODO 5b) ─
         AuctionHouse house5 = new AuctionHouse("Diamond Ring");
         Bidder a5 = new HumanBidder("Alice", house5);
         Bidder b5 = new HumanBidder("Bob",   house5);
         a5.placeBid(1_000.00);
         b5.placeBid(1_500.00);
         house5.closeAuction();
         // expected:
         //   [AuctionHouse] Auction closed! "Diamond Ring" won by Bob at $1500.00
         //   [Alice] Auction over — winner: Bob at $1500.00
         //   [Bob]   Auction over — winner: Bob at $1500.00

        // ── Test 6: Multiple bids — highest tracked correctly (TODO 5a) ──────────
         AuctionHouse house6 = new AuctionHouse("First Edition Book");
         Bidder a6 = new HumanBidder("Alice", house6);
         Bidder b6 = new HumanBidder("Bob",   house6);
         Bidder c6 = new HumanBidder("Carol", house6);
         a6.placeBid(100.00);
         b6.placeBid(150.00);
         c6.placeBid(130.00);   // rejected — Bob leads at $150
         a6.placeBid(200.00);   // Alice retakes lead
         house6.closeAuction();
        // // expected winner: Alice at $200.00

        // ── Test 7: Bidder holds no reference to other bidders — only mediator ───
         // (structural constraint — verified by code inspection, not a runtime test)
         // Confirm: HumanBidder has no field of type HumanBidder or Bidder[] etc.
         System.out.println("Test 7 — structural: Bidder holds only mediator reference (code inspection)");

        // ── Test 8: Polymorphic via AuctionMediator reference (TODO 1–7) ─────────
         AuctionMediator mediator = new AuctionHouse("Rare Coin");
         Bidder x = new HumanBidder("Xavier", mediator);
         Bidder y = new HumanBidder("Yasmin", mediator);
         x.placeBid(750.00);
         y.placeBid(900.00);
         ((AuctionHouse) mediator).closeAuction();
         // expected winner: Yasmin at $900.00
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HINTS — read only if stuck
    // ════════════════════════════════════════════════════════════════════════════

    // HINT 1 (Gentle):
    //   Bidders compete but never talk to each other. Each bidder only knows the
    //   auction house — it calls the auction house to place a bid, and the auction
    //   house tells everyone else about the new leader. Bidders don't hold a list
    //   of other bidders; only the auction house does.

    // HINT 2 (Direct):
    //   Use the Mediator pattern.
    //   AuctionMediator is the interface. AuctionHouse is the concrete mediator that
    //   holds List<Bidder>, tracks highestBid/highestBidder, and routes notifications.
    //   Bidder is an abstract class that holds only an AuctionMediator reference.
    //   Bidder.placeBid(amount) calls mediator.placeBid(this, amount).
    //   AuctionHouse.placeBid() notifies all bidders EXCEPT the one who placed the bid.
    //   AuctionHouse.closeAuction() notifies ALL bidders including the winner.

    // HINT 3 (Near-solution skeleton — class outlines only, no method bodies):
    //
    //   interface AuctionMediator {
    //       void placeBid(Bidder bidder, double amount);
    //       void register(Bidder bidder);
    //       void closeAuction();
    //   }
    //
    //   static class AuctionHouse implements AuctionMediator {
    //       private final List<Bidder> bidders = new ArrayList<>();
    //       private final String item;
    //       private double highestBid   = 0.0;
    //       private Bidder highestBidder = null;
    //       AuctionHouse(String item) { this.item = item; }
    //       @Override public void register(Bidder b) { ... }
    //       @Override public void placeBid(Bidder bidder, double amount) {
    //           if (amount > highestBid) { /* update, print, notify others */ }
    //           else                     { /* print rejection */ }
    //       }
    //       @Override public void closeAuction() { /* print, notify all */ }
    //   }
    //
    //   static abstract class Bidder {
    //       protected final AuctionMediator mediator;
    //       protected final String name;
    //       Bidder(String name, AuctionMediator mediator) {
    //           this.name = name; this.mediator = mediator; mediator.register(this);
    //       }
    //       String getName() { return name; }
    //       void placeBid(double amount) { mediator.placeBid(this, amount); }
    //       abstract void onNewBid(String bidderName, double amount);
    //       abstract void onAuctionClosed(String winnerName, double amount);
    //   }
    //
    //   static class HumanBidder extends Bidder {
    //       HumanBidder(String name, AuctionMediator mediator) { super(name, mediator); }
    //       @Override void onNewBid(String bidderName, double amount) { ... }
    //       @Override void onAuctionClosed(String winnerName, double amount) { ... }
    //   }
}

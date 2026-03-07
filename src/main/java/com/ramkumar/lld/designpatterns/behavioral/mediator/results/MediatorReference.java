package com.ramkumar.lld.designpatterns.behavioral.mediator.results;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference solution — Mediator Pattern: Auction House
 *
 * <p>Key decisions vs common mistakes:
 * <ul>
 *   <li>{@code Bidder} holds only {@code AuctionMediator} — never another {@code Bidder}.
 *       All notifications flow through the mediator; participants are fully decoupled.</li>
 *   <li>{@code placeBid()} notifies bidders where {@code b != bidder} — the sender is
 *       excluded. Without this filter, a bidder receives their own bid notification,
 *       which is redundant and can cause feedback loops.</li>
 *   <li>{@code closeAuction()} notifies ALL bidders including the winner — this is a
 *       different policy from {@code placeBid()}. Both are intentional.</li>
 *   <li>State ({@code highestBid}, {@code highestBidder}) is updated BEFORE notifying
 *       others — so {@code onNewBid()} recipients observe a consistent mediator state.</li>
 *   <li>Self-registration in {@code Bidder} constructor — no client code required to
 *       call {@code register()} separately after construction.</li>
 *   <li>{@code @Override} on every interface-method implementation — compiler catches
 *       refactoring breaks that would otherwise silently create orphan methods.</li>
 * </ul>
 */
public class MediatorReference {

    // ── [MediatorInterface] ────────────────────────────────────────────────────
    interface AuctionMediator {
        void placeBid(Bidder bidder, double amount);
        void register(Bidder bidder);
        void closeAuction();
    }

    // ── [ConcreteMediator] ─────────────────────────────────────────────────────
    // The ONLY class that knows all bidders. Bidders know only this interface.
    static class AuctionHouse implements AuctionMediator {

        private final List<Bidder> bidders     = new ArrayList<>();
        private final String       item;
        private double             highestBid  = 0.0;
        private Bidder             highestBidder = null;

        AuctionHouse(String item) {
            this.item = item;
        }

        // [Register] Add participant; called from Bidder constructor (self-registration)
        // public required: interface methods are implicitly public; implementations must match
        @Override
        public void register(Bidder bidder) {
            bidders.add(bidder);
            System.out.printf("[AuctionHouse] %s registered for \"%s\"%n",
                bidder.getName(), item);
        }

        // [PlaceBid] Route bid: update state, notify OTHERS (not sender), or reject
        @Override
        public void placeBid(Bidder bidder, double amount) {
            if (amount > highestBid) {
                // [UpdateFirst] State updated BEFORE notifying — recipients see consistent data
                highestBid    = amount;
                highestBidder = bidder;
                System.out.printf("[AuctionHouse] %s leads with $%.2f for \"%s\"%n",
                    bidder.getName(), amount, item);
                // [ExcludeSender] Notify everyone EXCEPT the bidder who placed the bid.
                // Reference equality (!=) is correct — each Bidder is a distinct instance.
                for (Bidder b : bidders) {
                    if (b != bidder) {
                        b.onNewBid(bidder.getName(), amount);
                    }
                }
            } else {
                // [Rejection] Lower bid — print and do nothing else
                System.out.printf("[AuctionHouse] %s bid $%.2f rejected — %s leads at $%.2f%n",
                    bidder.getName(), amount, highestBidder.getName(), highestBid);
            }
        }

        // [CloseAuction] Announce winner; notify ALL bidders — INCLUDING the winner.
        // This differs from placeBid() which excludes the sender.
        // Both policies are intentional and correct for their respective situations.
        @Override
        public void closeAuction() {
            System.out.printf("[AuctionHouse] Auction closed! \"%s\" won by %s at $%.2f%n",
                item, highestBidder.getName(), highestBid);
            for (Bidder b : bidders) {
                b.onAuctionClosed(highestBidder.getName(), highestBid);
            }
        }
    }

    // ── [AbstractParticipant] ──────────────────────────────────────────────────
    // Bidder holds only the mediator — NEVER another Bidder.
    // This is the central encapsulation boundary that makes the pattern work.
    static abstract class Bidder {

        protected final AuctionMediator mediator;   // [MediatorRef] only external reference
        protected final String          name;

        // [SelfRegister] Register immediately on construction — client needs no extra setup
        Bidder(String name, AuctionMediator mediator) {
            this.name     = name;
            this.mediator = mediator;
            mediator.register(this);   // self-registration
        }

        String getName() { return name; }

        // [DelegateToMediator] Participant never calls another participant's method.
        // It passes itself (this) so the mediator knows the sender.
        void placeBid(double amount) {
            mediator.placeBid(this, amount);
        }

        // [NotificationHooks] Called by mediator — not by other bidders directly
        abstract void onNewBid(String bidderName, double amount);
        abstract void onAuctionClosed(String winnerName, double amount);
    }

    // ── [ConcreteParticipant] HumanBidder ─────────────────────────────────────
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
            System.out.printf("[%s] Auction over — winner: %s at $%.2f%n",
                getName(), winnerName, amount);
        }
    }

    // ── Reference main() ──────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Test 1: Single bidder — first bid becomes highest ─────────────────
        AuctionHouse house1 = new AuctionHouse("Vintage Watch");
        Bidder alice = new HumanBidder("Alice", house1);
        alice.placeBid(500.00);

        // ── Test 2: Second bidder outbids first — first is notified ───────────
        System.out.println();
        AuctionHouse house2 = new AuctionHouse("Antique Vase");
        Bidder a2 = new HumanBidder("Alice", house2);
        Bidder b2 = new HumanBidder("Bob",   house2);
        a2.placeBid(300.00);
        b2.placeBid(450.00);   // Alice notified; Bob is NOT (his own bid)

        // ── Test 3: Lower bid is rejected ─────────────────────────────────────
        System.out.println();
        AuctionHouse house3 = new AuctionHouse("Oil Painting");
        Bidder a3 = new HumanBidder("Alice", house3);
        Bidder b3 = new HumanBidder("Bob",   house3);
        a3.placeBid(600.00);
        b3.placeBid(400.00);   // rejected

        // ── Test 4: Bidder does NOT receive onNewBid for their own bid ─────────
        System.out.println();
        AuctionHouse house4 = new AuctionHouse("Sculpture");
        Bidder a4 = new HumanBidder("Alice", house4);
        Bidder b4 = new HumanBidder("Bob",   house4);
        Bidder c4 = new HumanBidder("Carol", house4);
        a4.placeBid(200.00);   // Bob and Carol notified; Alice is NOT

        // ── Test 5: closeAuction() notifies ALL bidders including winner ───────
        System.out.println();
        AuctionHouse house5 = new AuctionHouse("Diamond Ring");
        Bidder a5 = new HumanBidder("Alice", house5);
        Bidder b5 = new HumanBidder("Bob",   house5);
        a5.placeBid(1_000.00);
        b5.placeBid(1_500.00);
        house5.closeAuction();   // both Alice AND Bob receive onAuctionClosed

        // ── Test 6: Multiple bids — highest tracked correctly ─────────────────
        System.out.println();
        AuctionHouse house6 = new AuctionHouse("First Edition Book");
        Bidder a6 = new HumanBidder("Alice", house6);
        Bidder b6 = new HumanBidder("Bob",   house6);
        Bidder c6 = new HumanBidder("Carol", house6);
        a6.placeBid(100.00);
        b6.placeBid(150.00);
        c6.placeBid(130.00);   // rejected — Bob leads at $150
        a6.placeBid(200.00);   // Alice retakes lead
        house6.closeAuction();   // Alice wins at $200

        // ── Test 7: Structural — Bidder holds only mediator reference ──────────
        System.out.println("\nTest 7 — structural: Bidder holds only mediator reference (code inspection)");

        // ── Test 8: Polymorphic via AuctionMediator reference ─────────────────
        System.out.println();
        AuctionMediator mediator = new AuctionHouse("Rare Coin");
        Bidder x = new HumanBidder("Xavier", mediator);
        Bidder y = new HumanBidder("Yasmin", mediator);
        x.placeBid(750.00);
        y.placeBid(900.00);
        ((AuctionHouse) mediator).closeAuction();   // Yasmin wins

        // ── Test 9: Sender excluded from onNewBid — most common mistake ───────
        // The most common mistake: iterating ALL bidders without the `b != bidder`
        // filter. This causes the sender to receive their own onNewBid() call.
        //
        // Wrong loop:
        //   for (Bidder b : bidders) { b.onNewBid(...); }   // ← includes sender
        //
        // Correct loop:
        //   for (Bidder b : bidders) { if (b != bidder) b.onNewBid(...); }
        //
        // This test uses 3 bidders and Alice places the only bid.
        // Correct: Bob and Carol receive onNewBid — Alice does NOT.
        // Wrong:   Alice, Bob, and Carol all receive onNewBid (3 lines instead of 2).
        System.out.println("\n── Test 9: Sender never receives their own onNewBid ──");
        System.out.println("(Alice bids — exactly 2 'Outbid' lines expected, not 3)");
        AuctionHouse house9 = new AuctionHouse("Gemstone");
        Bidder aa = new HumanBidder("Alice", house9);
        Bidder bb = new HumanBidder("Bob",   house9);
        Bidder cc = new HumanBidder("Carol", house9);
        aa.placeBid(350.00);
        // Expected output (exactly 2 Outbid lines — Alice must NOT appear):
        //   [AuctionHouse] Alice leads with $350.00 for "Gemstone"
        //   [Bob]   Outbid by Alice at $350.00
        //   [Carol] Outbid by Alice at $350.00
        //   ← Alice does NOT appear here
    }
}

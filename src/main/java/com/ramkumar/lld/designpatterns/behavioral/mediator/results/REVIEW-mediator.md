# Review — Mediator Pattern: Auction House

---

## What You Got Right

- **`Bidder` holds only `AuctionMediator` — never another `Bidder`** — the central encapsulation boundary of the pattern. `Bidder.placeBid()` delegates to `mediator.placeBid(this, amount)` rather than calling any peer's method directly.
- **`b != bidder` filter in `placeBid()` loop** — the bidder who places a bid does NOT receive their own `onNewBid()` notification. Test 4 (3 bidders, only 2 notified) and Test 2 confirm this works correctly.
- **`closeAuction()` notifies ALL bidders including the winner** — Test 5 confirms both Alice and Bob receive `onAuctionClosed()`, which is required by the spec.
- **Self-registration in `Bidder` constructor** — `mediator.register(this)` called in `Bidder(String, AuctionMediator)` ensures every participant is automatically in the mediator's list without extra client setup.
- **`AuctionHouse` holds the participant list — not `Bidder`** — the mediator is the only class that knows all participants; participants know only the mediator.
- **Correct bid-rejection branch** — lower bids print the rejection message with the current leader's name and amount, not just a generic "rejected".
- **`highestBidder` and `highestBid` updated before notifying others** — the notification loop runs after the state update, so `onNewBid()` recipients see the correct leader immediately.
- **All 8 tests pass.**

---

## Issues Found

**1.**
- **Severity**: Minor
- **What**: Missing `@Override` on `AuctionHouse.register()` — the only interface method without the annotation.
- **Your code**:
  ```java
  public void register(Bidder bidder) {    // ← no @Override
      bidders.add(bidder);
      ...
  }
  ```
- **Fix**:
  ```java
  @Override
  public void register(Bidder bidder) {
      bidders.add(bidder);
      ...
  }
  ```
- **Why it matters**: `@Override` lets the compiler catch typos and refactoring breaks — if the interface method signature ever changes, the missing annotation means the class silently stops overriding it.

**2.**
- **Severity**: Minor
- **What**: Double space in the `AuctionHouse` constructor: `this.item =  item;`.
- **Your code**:
  ```java
  this.item =  item;
  ```
- **Fix**:
  ```java
  this.item = item;
  ```
- **Why it matters**: Style noise — signals a rushed edit; flagged by most formatters.

**3.**
- **Severity**: Minor
- **What**: Inconsistent brace placement in `closeAuction()` — `{` on a new line for the `for` loop, while all other blocks use same-line `{`.
- **Your code**:
  ```java
  for(Bidder b : bidders)
  {
      b.onAuctionClosed(highestBidder.getName(), highestBid);
  }
  ```
- **Fix**:
  ```java
  for (Bidder b : bidders) {
      b.onAuctionClosed(highestBidder.getName(), highestBid);
  }
  ```
- **Why it matters**: Style consistency — mixing brace styles in one file makes the code harder to read.

**4.**
- **Severity**: Minor
- **What**: Unnecessary `public` on constructors and non-interface methods: `AuctionHouse(String)`, `Bidder(String, AuctionMediator)`, `getName()`, and `Bidder.placeBid(double)`.
  Note: `public` on `register()`, `placeBid(Bidder, double)`, and `closeAuction()` in `AuctionHouse` is **correct and required** — interface methods are implicitly `public`, so their implementations must also be `public`.
- **Your code**:
  ```java
  public AuctionHouse(String item){ ... }          // unnecessary — constructor
  public Bidder(String name, AuctionMediator m){ } // unnecessary — constructor
  public String getName() { ... }                  // unnecessary — non-interface method
  public void placeBid(double amount) { ... }      // unnecessary — non-interface method
  ```
- **Fix**: Remove `public` only from constructors and non-interface methods; keep it on the three `AuctionMediator` implementations.
- **Why it matters**: Unnecessary `public` on constructors and helpers widens the API; but removing `public` from interface implementations is a compile error (Java rule: implementations cannot narrow interface method visibility).

---

## Score Card

| Requirement | Result |
|---|---|
| `AuctionMediator` — 3 method signatures | ✅ |
| `AuctionHouse` — 4 fields (`bidders`, `item`, `highestBid`, `highestBidder`) | ✅ |
| `AuctionHouse(String item)` constructor | ✅ |
| `register()` — adds bidder + prints registration | ✅ |
| `@Override` on `register()` | ❌ |
| `placeBid()` — updates state before notifying | ✅ |
| `placeBid()` — notifies all bidders except sender (`b != bidder`) | ✅ |
| `placeBid()` — rejects lower bid with correct message | ✅ |
| `closeAuction()` — notifies ALL bidders including winner | ✅ |
| `Bidder` — holds only `AuctionMediator` reference | ✅ |
| `Bidder` constructor — self-registers via `mediator.register(this)` | ✅ |
| `Bidder.placeBid()` delegates to `mediator.placeBid(this, amount)` | ✅ |
| `HumanBidder.onNewBid()` — correct print format | ✅ |
| `HumanBidder.onAuctionClosed()` — correct print format | ✅ |
| All 8 tests pass | ✅ |
| No missing `@Override` | ❌ |

---

## Key Takeaways — Do Not Miss These

**TK-1: Participants hold only the mediator reference — never each other.**
If `Bidder` held a `List<Bidder>` and called `peer.onNewBid()` directly, you've recreated `O(n²)` coupling. Adding a new bidder type would require updating every existing bidder. With a mediator, only the mediator changes.
*In interviews: "how does Bidder notify others?" — answer is "it doesn't; it calls mediator.placeBid() and the mediator decides who to notify."*

**TK-2: The sender must not receive their own notification.**
In `placeBid()`, the loop must filter `b != bidder`. Without this, Alice gets `onNewBid("Alice", 500.00)` — a self-notification that is redundant and can cause infinite loops if the bidder reacts by placing another bid.
*Test 4 (3 bidders, 2 notifications) directly probes this — the most common implementation mistake.*

**TK-3: `closeAuction()` differs from `placeBid()` — it notifies ALL including the winner.**
`placeBid()` excludes the sender; `closeAuction()` includes everyone. These are two distinct notification policies in the same mediator. Mixing them up causes the winner to miss their own close notification.
*In interviews: "which bidders get notified at close?" — "all of them" is the answer.*

**TK-4: `@Override` must be on every method that implements an interface.**
Without `@Override`, a typo in the method name silently becomes a new method rather than an implementation of the interface method. The compiler catches this with `@Override`; without it, the bug survives until runtime.
*Interviewers notice missing `@Override` — it signals unfamiliarity with Java best practices.*

**TK-5: Mediator vs Observer — know the distinction.**
Observer is one-to-many broadcast from a single subject. Mediator is many-to-many coordination where participants can interact with each other through a shared hub. An auction is a mediator: multiple bidders interact with multiple bidders (via the house); a stock ticker is observer: one price source notifies many listeners.
*Interviewers frequently ask: "is this Mediator or Observer and why?"*

package com.ramkumar.lld.javareadiness.lldpatterns.results;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Reference solution — Phase 4 | 4.2 Recurring LLD Patterns
 * Scenario B: Parcel Locker Service (entity → repository → service).
 */
public class ParcelLockerReference {

    static class Parcel {
        private final String id;
        private final String recipient;
        private final int weightGrams;
        private boolean pickedUp;                       // the only mutable field

        Parcel(String id, String recipient, int weightGrams) {
            // Spec order; assign only after every check passes.
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            Objects.requireNonNull(recipient, "recipient must not be null");   // NPE with message
            if (recipient.isBlank()) {
                throw new IllegalArgumentException("recipient must not be blank");
            }
            if (weightGrams <= 0) {
                throw new IllegalArgumentException("weightGrams must be > 0");
            }
            this.id = id;
            this.recipient = recipient;
            this.weightGrams = weightGrams;
        }

        String getId() { return id; }
        String getRecipient() { return recipient; }
        int getWeightGrams() { return weightGrams; }
        boolean isPickedUp() { return pickedUp; }
        void markPickedUp() { pickedUp = true; }

        @Override
        public String toString() {
            return String.format("%s[%s, %dg, %s]", id, recipient, weightGrams, pickedUp ? "PICKED_UP" : "WAITING");
        }
    }

    static class ParcelRepository {
        private final Map<String, Parcel> store = new LinkedHashMap<>();   // insertion order

        void save(Parcel p) {
            Objects.requireNonNull(p, "parcel must not be null");
            // putIfAbsent: single lookup, returns the previous value when the key existed
            if (store.putIfAbsent(p.getId(), p) != null) {
                throw new IllegalArgumentException("duplicate id: " + p.getId());
            }
        }

        Optional<Parcel> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        // Snapshot: caller cannot mutate it, later saves do not appear in it.
        List<Parcel> findAll() {
            return List.copyOf(store.values());
        }
    }

    static class ParcelLockerService {
        private final ParcelRepository repo = new ParcelRepository();
        private int counter;                            // per instance, NOT static

        Parcel register(String recipient, int weightGrams) {
            // Candidate id first; the Parcel constructor may throw, and then the counter must not move.
            String id = String.format("PCL-%04d", counter + 1);
            Parcel parcel = new Parcel(id, recipient, weightGrams);
            counter++;
            repo.save(parcel);
            System.out.printf("[LockerService] registered %s for %s%n", id, recipient);
            return parcel;
        }

        Parcel pickUp(String id) {
            Parcel parcel = repo.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("parcel not found: " + id));
            if (parcel.isPickedUp()) {
                throw new IllegalStateException("parcel already picked up: " + id);   // state, not argument
            }
            parcel.markPickedUp();
            System.out.printf("[LockerService] picked up %s%n", id);
            return parcel;
        }

        List<Parcel> pendingFor(String recipient) {
            return repo.findAll().stream()
                    .filter(p -> p.getRecipient().equals(recipient))   // BOTH conditions: recipient AND not picked up
                    .filter(p -> !p.isPickedUp())
                    .toList();                                         // unmodifiable
        }
    }

    public static void main(String[] args) {
        // Test 1
        Parcel p = new Parcel("PCL-0001", "Asha", 500);
        System.out.println(p);                                  // PCL-0001[Asha, 500g, WAITING]
        System.out.println(p.isPickedUp());                     // false
        p.markPickedUp();
        System.out.println(p);                                  // PCL-0001[Asha, 500g, PICKED_UP]

        // Test 2
        String[] bad = {"id", "nullRecipient", "blankRecipient", "weight"};
        for (String which : bad) {
            try {
                switch (which) {
                    case "id" -> new Parcel(" ", "Asha", 5);
                    case "nullRecipient" -> new Parcel("P-1", null, 5);
                    case "blankRecipient" -> new Parcel("P-1", "  ", 5);
                    default -> new Parcel("P-1", "Asha", 0);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Test 3
        ParcelRepository repo = new ParcelRepository();
        repo.save(new Parcel("PCL-0001", "Asha", 500));
        System.out.println(repo.findById("PCL-0001").isPresent());   // true
        System.out.println(repo.findById("PCL-0099").isPresent());   // false
        try { repo.save(new Parcel("PCL-0001", "Ben", 100)); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { repo.save(null); } catch (NullPointerException e) { System.out.println(e.getMessage()); }

        // Test 4
        List<Parcel> snapshot = repo.findAll();
        System.out.println(snapshot.size());                    // 1
        try { snapshot.add(new Parcel("X", "Y", 1)); } catch (UnsupportedOperationException e) { System.out.println("unmodifiable"); }
        repo.save(new Parcel("PCL-0002", "Ben", 100));
        System.out.println(snapshot.size());                    // 1
        System.out.println(repo.findAll().size());              // 2

        // Test 5
        ParcelLockerService svc = new ParcelLockerService();
        Parcel a = svc.register("Asha", 500);
        Parcel b = svc.register("Ben", 1200);
        System.out.println(a.getId() + " " + b.getId());        // PCL-0001 PCL-0002
        System.out.println(new ParcelLockerService().register("Cy", 10).getId());   // PCL-0001

        // Test 6
        System.out.println(svc.pickUp("PCL-0001").isPickedUp());   // true
        try { svc.pickUp("PCL-0001"); } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
        try { svc.pickUp("PCL-0099"); } catch (NoSuchElementException e) { System.out.println(e.getMessage()); }

        // Test 7
        svc.register("Asha", 300);
        System.out.println(svc.pendingFor("Asha").stream().map(Parcel::getId).toList());   // [PCL-0003]
        System.out.println(svc.pendingFor("Ben").stream().map(Parcel::getId).toList());    // [PCL-0002]
        System.out.println(svc.pendingFor("Zed"));              // []
        try { svc.pendingFor("Ben").add(a); } catch (UnsupportedOperationException e) { System.out.println("unmodifiable"); }

        // Test 8
        try { svc.register("Dee", 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        try { svc.register(null, 5); } catch (NullPointerException e) { System.out.println(e.getMessage()); }
        System.out.println(svc.register("Dee", 50).getId());    // PCL-0004

        // Extra: catches the most common mistake — pendingFor ignoring the recipient filter
        System.out.println(svc.pendingFor("Asha").stream().map(Parcel::getRecipient).distinct().toList());   // [Asha]
        System.out.println(svc.pendingFor("Dee").size());       // 1 (not 3)
    }
}

package com.ramkumar.lld.javareadiness.javaidioms.code;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Worked Example — Phase 4 Java Readiness | 4.3 Java 8–21 Idioms
 * Scenario A: Notification Dispatch Costing
 *
 * Concepts demonstrated (labelled inline):
 *   [SEALED]      closed hierarchy of notification kinds
 *   [RECORD]      immutable value types with compact-constructor validation
 *   [SWITCH]      exhaustive switch expression (no default)
 *   [RECORD-PAT]  record deconstruction pattern (Java 21)
 *   [COMPARATOR]  one comparator constant, reused
 *   [OPTIONAL]    stream max -> Optional, no null
 *   [STREAM]      filter / sorted / groupingBy with TreeMap
 */
public class NotificationCostDemo {

    // [SEALED] Only these three may implement Notification. Adding a fourth later makes
    // every switch below fail to compile until it handles the new case.
    sealed interface Notification permits Email, Sms, Push {
        String id();            // records satisfy this with their generated accessor
    }

    // [RECORD] Compact constructor: validate; fields are assigned automatically afterwards.
    record Email(String id, String to, int attachmentKb) implements Notification {
        Email {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (attachmentKb < 0) throw new IllegalArgumentException("attachmentKb must be >= 0");
        }
    }

    record Sms(String id, String to, int segments) implements Notification {
        Sms {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (segments < 1) throw new IllegalArgumentException("segments must be >= 1");
        }
    }

    record Push(String id, String deviceToken) implements Notification {
        Push {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        }
    }

    // [SWITCH] Switch EXPRESSION: returns a value, no break, no fall-through.
    // [SWITCH] No `default`: the sealed type lets the compiler prove every case is covered.
    static double cost(Notification n) {
        return switch (n) {
            case Email e -> 0.10 + e.attachmentKb() * 0.001;
            case Sms s   -> s.segments() * 0.05;
            case Push p  -> 0.0;
        };
    }

    // [SWITCH] Short label used as a grouping key.
    static String channel(Notification n) {
        return switch (n) {
            case Email e -> "EMAIL";
            case Sms s   -> "SMS";
            case Push p  -> "PUSH";
        };
    }

    // [RECORD-PAT] Deconstruct the record: components bound directly, no accessor calls.
    static String describe(Notification n) {
        return switch (n) {
            case Email(String id, String to, int kb) -> String.format("EMAIL %s -> %s (%d KB)", id, to, kb);
            case Sms(String id, String to, int seg)  -> String.format("SMS %s -> %s (%d segments)", id, to, seg);
            case Push(String id, String token)       -> String.format("PUSH %s -> %s", id, token);
        };
    }

    // [COMPARATOR] Defined once. reversed() applies to the cost key only (everything built so far);
    // the id tie-break is ascending and makes the order total.
    private static final Comparator<Notification> EXPENSIVE_FIRST =
            Comparator.comparingDouble(NotificationCostDemo::cost).reversed()
                      .thenComparing(Notification::id);

    // [STREAM] sorted() returns a new stream; the input list is never mutated. toList() is unmodifiable.
    static List<Notification> expensiveFirst(List<Notification> all) {
        return all.stream().sorted(EXPENSIVE_FIRST).toList();
    }

    // [OPTIONAL] findFirst/min on an empty stream -> Optional.empty(), not null / exception.
    // min() of the "expensive first" order == the most expensive (ties: lowest id).
    static Optional<Notification> mostExpensive(List<Notification> all) {
        return all.stream().min(EXPENSIVE_FIRST);
    }

    // [STREAM] groupingBy(key, TreeMap::new, summingDouble) -> sorted keys, totals per channel.
    // Returned through a typed return so type inference succeeds.
    static Map<String, Double> costByChannel(List<Notification> all) {
        return all.stream().collect(Collectors.groupingBy(
                NotificationCostDemo::channel, TreeMap::new, Collectors.summingDouble(NotificationCostDemo::cost)));
    }

    public static void main(String[] args) {
        List<Notification> all = List.of(
                new Email("N-1", "asha@x.com", 400),     // 0.10 + 0.4 = 0.5
                new Sms("N-2", "+4412345", 3),           // 0.15
                new Push("N-3", "tok-abc"),              // 0.0
                new Sms("N-4", "+4498765", 10),          // 0.5  (ties with N-1 -> id decides)
                new Email("N-5", "ben@x.com", 0));       // 0.10

        System.out.println("=== [SWITCH] cost / channel ===");
        for (Notification n : all) {
            System.out.printf("%-4s %-6s %.3f%n", n.id(), channel(n), cost(n));
        }

        System.out.println("\n=== [RECORD-PAT] describe ===");
        all.forEach(n -> System.out.println(describe(n)));

        System.out.println("\n=== [COMPARATOR] expensive first (tie -> id) ===");
        System.out.println(expensiveFirst(all).stream().map(Notification::id).toList());   // [N-1, N-4, N-2, N-5, N-3]

        System.out.println("\n=== [OPTIONAL] most expensive ===");
        System.out.println(mostExpensive(all).map(Notification::id).orElse("none"));       // N-1
        System.out.println(mostExpensive(List.of()).isPresent());                          // false

        System.out.println("\n=== [STREAM] cost by channel ===");
        System.out.println(costByChannel(all));     // doubles may print tiny rounding noise

        System.out.println("\n=== [RECORD] value equality + validation ===");
        System.out.println(new Push("P", "t").equals(new Push("P", "t")));                 // true
        try { new Sms("S", "x", 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
    }
}

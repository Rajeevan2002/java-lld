package com.ramkumar.lld.designpatterns.behavioral.strategy.code;

import java.util.Arrays;

// ─────────────────────────────────────────────────────────────────────────────
// Strategy Pattern — Scenario A: Sorting Algorithms
//
// Problem: A DataProcessor needs to sort an int[] in different ways depending
//          on the dataset size and type. Hard-coding if/else for each algorithm
//          would require modifying DataProcessor every time a new sort is added.
//
// Solution: Extract the sorting algorithm behind a SortStrategy interface.
//           DataProcessor holds a SortStrategy reference (the interface, NOT a
//           concrete class) and delegates sorting entirely to it.
//
// Participants:
//   SortStrategy        [Strategy interface]   — defines the algorithm contract
//   BubbleSortStrategy  [ConcreteStrategy]      — O(n²) in-place sort
//   QuickSortStrategy   [ConcreteStrategy]      — O(n log n) divide-and-conquer
//   MergeSortStrategy   [ConcreteStrategy]      — O(n log n) stable sort
//   DataProcessor       [Context]               — holds and delegates to SortStrategy
// ─────────────────────────────────────────────────────────────────────────────

// ── [Strategy] — the single method all concrete strategies must implement ─────
interface SortStrategy {
    // [Contract] Sorts the array in-place. Context passes the data; strategy does the work.
    void sort(int[] data);
}

// ── [ConcreteStrategy 1] — simple O(n²) sort ──────────────────────────────────
class BubbleSortStrategy implements SortStrategy {

    @Override
    public void sort(int[] data) {
        // [AlgorithmEncapsulation] All bubble-sort logic lives here, not in DataProcessor.
        int n = data.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (data[j] > data[j + 1]) {
                    int tmp = data[j]; data[j] = data[j + 1]; data[j + 1] = tmp;
                }
            }
        }
        System.out.printf("[BubbleSort] sorted %d elements%n", n);
    }
}

// ── [ConcreteStrategy 2] — O(n log n) divide-and-conquer ─────────────────────
class QuickSortStrategy implements SortStrategy {

    @Override
    public void sort(int[] data) {
        quickSort(data, 0, data.length - 1);
        System.out.printf("[QuickSort] sorted %d elements%n", data.length);
    }

    // [PrivateHelper] Implementation details stay private inside the strategy.
    private void quickSort(int[] data, int lo, int hi) {
        if (lo < hi) {
            int pivot = partition(data, lo, hi);
            quickSort(data, lo, pivot - 1);
            quickSort(data, pivot + 1, hi);
        }
    }

    private int partition(int[] data, int lo, int hi) {
        int pivot = data[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (data[j] <= pivot) { i++; int tmp = data[i]; data[i] = data[j]; data[j] = tmp; }
        }
        int tmp = data[i + 1]; data[i + 1] = data[hi]; data[hi] = tmp;
        return i + 1;
    }
}

// ── [ConcreteStrategy 3] — O(n log n) stable sort ────────────────────────────
class MergeSortStrategy implements SortStrategy {

    @Override
    public void sort(int[] data) {
        // [DelegateToJDK] Arrays.sort uses dual-pivot quicksort internally — fine for demo.
        // In a real implementation this would be a hand-rolled merge sort.
        int[] sorted = Arrays.stream(data).sorted().toArray();
        System.arraycopy(sorted, 0, data, 0, data.length);
        System.out.printf("[MergeSort] sorted %d elements%n", data.length);
    }
}

// ── [Context] ─────────────────────────────────────────────────────────────────
class DataProcessor {

    private final String datasetName;
    // [InterfaceField] CRITICAL: typed as SortStrategy (interface), NOT a concrete class.
    // This is what enables runtime swapping and open/closed compliance.
    private SortStrategy strategy;

    DataProcessor(String datasetName, SortStrategy strategy) {
        this.datasetName = datasetName;
        this.strategy    = strategy;
    }

    // [RuntimeSwap] The caller can change the algorithm at any point before process().
    // DataProcessor never needs to change regardless of how many strategies are added.
    void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }

    // [Delegation] Context does NOT sort — it holds the data and delegates the algorithm.
    // No if/else, no instanceof. One line covers all current and future strategies.
    void process(int[] data) {
        System.out.printf("[DataProcessor: %s] processing %d items with %s%n",
            datasetName, data.length, strategy.getClass().getSimpleName());
        strategy.sort(data);                             // [Delegation] only line that varies
        System.out.printf("  Result: %s%n", Arrays.toString(data));
    }
}

// ── Demo ──────────────────────────────────────────────────────────────────────
public class SortDemo {

    public static void main(String[] args) {

        int[] data1 = {5, 3, 8, 1, 9, 2};
        int[] data2 = {5, 3, 8, 1, 9, 2};
        int[] data3 = {5, 3, 8, 1, 9, 2};

        // ── 1. BubbleSort strategy ──────────────────────────────────────────
        System.out.println("─── Strategy: BubbleSort ───");
        // [Injection] Strategy is injected at construction — context is oblivious to which one.
        DataProcessor p1 = new DataProcessor("SmallSet", new BubbleSortStrategy());
        p1.process(data1);

        // ── 2. QuickSort strategy ───────────────────────────────────────────
        System.out.println("\n─── Strategy: QuickSort ───");
        DataProcessor p2 = new DataProcessor("MediumSet", new QuickSortStrategy());
        p2.process(data2);

        // ── 3. MergeSort strategy ───────────────────────────────────────────
        System.out.println("\n─── Strategy: MergeSort ───");
        DataProcessor p3 = new DataProcessor("LargeSet", new MergeSortStrategy());
        p3.process(data3);

        // ── 4. Runtime swap — same context, different strategy ──────────────
        System.out.println("\n─── Runtime swap on same DataProcessor ───");
        int[] data4 = {7, 2, 4, 6, 1};
        DataProcessor p4 = new DataProcessor("DynamicSet", new BubbleSortStrategy());
        p4.process(data4.clone());

        // [Swap] setStrategy() swaps the algorithm; DataProcessor code does not change.
        p4.setStrategy(new QuickSortStrategy());
        p4.process(data4.clone());

        p4.setStrategy(new MergeSortStrategy());
        p4.process(data4.clone());

        // ── 5. Polymorphic use via interface reference ──────────────────────
        System.out.println("\n─── Polymorphic strategy array ───");
        // [Polymorphism] All three strategies are held as SortStrategy — no concrete types.
        SortStrategy[] strategies = {
            new BubbleSortStrategy(),
            new QuickSortStrategy(),
            new MergeSortStrategy()
        };
        for (SortStrategy s : strategies) {
            int[] d = {9, 4, 7, 1, 5};
            DataProcessor p = new DataProcessor("PolySet", s);
            p.process(d);
        }
    }
}

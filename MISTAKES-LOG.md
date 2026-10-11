# Mistakes Log

One line per exercise: what went wrong, and the rule that prevents it. Review this **before every mock**.

| Date | Exercise | Mistake | Rule |
|---|---|---|---|
| 2026-10 | Loan Desk — `transition` | Wrote `if (containsKey(id)) throw NotFound` (inverted) | Never `containsKey` + `get`; use `findById(id).orElseThrow(...)` |
| 2026-10 | Loan Desk — `checkout` | Only `ACTIVE` counted as "on loan"; member limit forgotten | Define "open" once (`!isTerminal()`) and reuse it; tick every rule in the spec before running |
| 2026-10 | Loan Desk — `nextDue` | Best-so-far loop compared in the wrong direction | Use `stream().min(Comparator...)`; no hand-rolled min loops |
| 2026-10 | Loan Desk — `countByStatus` | `HashMap` + `Map.copyOf` lost order and zero counts | Enum keys → `EnumMap`, zero-fill first, then `merge(k, 1L, Long::sum)` |
| 2026-10 | Loan Desk — `overdueReport` | Ascending sort, no tie-break | `comparingInt((T t) -> ...).reversed().thenComparing(...)` |
| | | | |

## Recurring themes (update weekly)
- [ ] Thread-safety forgotten?
- [ ] Pattern forced where no requirement asked for it?
- [ ] Edge cases (null, empty, terminal state) skipped?
- [ ] Spec message/format not matched exactly?

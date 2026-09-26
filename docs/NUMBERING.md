# Gap-less sequential invoice numbering

## The requirement (legal, not negotiable)

- Numbers are **sequential**, **per year**, with **no gaps**.
- Two concurrent invoice creations must never receive the same number.
- A number must not be "burned" by a transaction that later rolls back.

## Options and trade-offs

| Approach | Gap-less? | Concurrency | Notes |
|----------|-----------|-------------|-------|
| DB `SEQUENCE` | ❌ can gap | excellent | Sequences advance outside the transaction; a rollback leaves a hole. Fine for surrogate keys, **wrong** for legal invoice numbers. |
| `SELECT … FOR UPDATE` on a per-year counter row | ✅ | serialised per year | Simple, correct. The counter row is locked for the duration of the tx; contention is bounded to one year's invoices. **Default choice.** |
| Postgres advisory lock keyed by year | ✅ | serialised per year | Similar guarantees without a counter table; lock is easy to leak if the tx boundary is wrong. |
| Assign number only at **commit** (outbox-driven) | ✅ | high | Decouples creation from numbering; more moving parts. |

**Chosen: `FOR UPDATE` on a `invoice_counter(year, last_number)` row**, incremented inside the
same transaction that inserts the invoice. If the insert rolls back, the increment rolls back
with it → no gap, no duplicate.

## The test that proves it

Spin up `N` threads that each create an invoice against Testcontainers Postgres; assert the set
of assigned numbers is exactly `{1..N}` — no duplicates, no gaps — and repeat across a rollback
scenario to prove a failed transaction does not consume a number.

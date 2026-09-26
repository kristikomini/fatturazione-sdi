package it.kristikomini.fatturapa.numbering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns the next invoice number for a year — <b>sequential, per year, no gaps</b>, which is a
 * legal requirement in Italy, not a preference.
 *
 * <p>How the guarantees are met:
 * <ul>
 *   <li><b>No duplicates / gaps under concurrency:</b> the year's counter row is locked
 *       {@code FOR UPDATE}; concurrent callers serialise on it.</li>
 *   <li><b>No burned numbers on failure:</b> the increment runs in the caller's transaction
 *       ({@code REQUIRED}); if the surrounding invoice insert rolls back, the increment rolls back
 *       with it, so a failed issue does not consume a number.</li>
 *   <li><b>Safe first-of-year:</b> the row is created with {@code INSERT … ON CONFLICT DO NOTHING}
 *       before locking, so two concurrent first invoices cannot collide.</li>
 * </ul>
 */
@Service
public class InvoiceNumberingService {

    private final InvoiceCounterRepository counters;

    public InvoiceNumberingService(InvoiceCounterRepository counters) {
        this.counters = counters;
    }

    /**
     * Must run inside the transaction that also persists the invoice ({@code REQUIRED} joins it),
     * so numbering and the invoice commit or roll back together.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int nextNumber(int year) {
        counters.insertIfAbsent(year);      // idempotent, concurrency-safe
        InvoiceCounter counter = counters.lockByYear(year)
                .orElseThrow(() -> new IllegalStateException("counter row missing after insert for year " + year));
        return counter.increment();          // dirty-checked; flushed on commit
    }
}

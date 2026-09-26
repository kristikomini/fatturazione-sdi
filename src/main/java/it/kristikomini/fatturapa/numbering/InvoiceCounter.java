package it.kristikomini.fatturapa.numbering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * The per-year invoice number counter. One row per year; {@code lastNumber} is the highest number
 * issued so far. Rows are locked {@code FOR UPDATE} during increment (see
 * {@link InvoiceCounterRepository#lockByYear(int)}), which is what serialises concurrent issuers
 * and keeps the sequence gap-less.
 */
@Entity
@Table(name = "invoice_counter")
public class InvoiceCounter {

    @Id
    @Column(name = "year")
    private int year;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    /** Optimistic version, kept as a second line of defence / audit even though the FOR UPDATE lock is primary. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InvoiceCounter() {
    }

    public InvoiceCounter(int year) {
        this.year = year;
        this.lastNumber = 0;
    }

    /** Advances and returns the next number. Called only while the row is pessimistically locked. */
    public int increment() {
        return ++this.lastNumber;
    }

    public int getYear() {
        return year;
    }

    public int getLastNumber() {
        return lastNumber;
    }
}

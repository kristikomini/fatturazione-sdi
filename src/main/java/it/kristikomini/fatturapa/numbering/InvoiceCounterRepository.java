package it.kristikomini.fatturapa.numbering;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, Integer> {

    /**
     * Locks the counter row for a year with {@code SELECT … FOR UPDATE}. Concurrent callers block
     * here until the holder's transaction commits, which is exactly what makes the increment
     * sequential and gap-less. (A plain DB {@code SEQUENCE} would be faster but can leave gaps on
     * rollback — illegal for invoice numbers.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM InvoiceCounter c WHERE c.year = :year")
    Optional<InvoiceCounter> lockByYear(@Param("year") int year);

    /**
     * Creates the year's counter row if it does not exist yet, concurrency-safe via Postgres
     * {@code ON CONFLICT DO NOTHING} — so two threads issuing the first invoice of a year cannot
     * both insert and collide. A no-op when the row already exists.
     */
    @Modifying
    @Query(value = "INSERT INTO invoice_counter(year, last_number, version) "
            + "VALUES (:year, 0, 0) ON CONFLICT (year) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("year") int year);
}

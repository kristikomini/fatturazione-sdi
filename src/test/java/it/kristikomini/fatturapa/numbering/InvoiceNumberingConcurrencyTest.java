package it.kristikomini.fatturapa.numbering;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the legal numbering guarantees against real PostgreSQL: fire many invoice-number requests
 * for the same year at once and assert the numbers assigned are exactly {@code 1..N} — no
 * duplicates, no gaps — and that a rolled-back issue does not consume a number.
 *
 * <p>Skips cleanly without Docker (local sandbox); runs for real in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InvoiceNumberingConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    InvoiceNumberingService numbering;

    @Autowired
    InvoiceCounterRepository counters;

    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void assignsGaplessUniqueNumbersUnderConcurrency() throws Exception {
        int year = 2026;
        int requests = 50;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Callable<Integer>> tasks = IntStream.range(0, requests)
                .<Callable<Integer>>mapToObj(i -> () -> {
                    startGate.await();               // all threads pause here…
                    return numbering.nextNumber(year); // …then fire at once
                })
                .toList();

        List<Future<Integer>> futures = tasks.stream().map(pool::submit).toList();
        startGate.countDown();                       // release the herd

        List<Integer> assigned = new java.util.ArrayList<>();
        for (Future<Integer> f : futures) {
            assigned.add(f.get());
        }
        pool.shutdown();

        assertThat(assigned).doesNotHaveDuplicates();
        assertThat(assigned).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, requests).boxed().toList());
        assertThat(counters.findById(year)).get()
                .extracting(InvoiceCounter::getLastNumber).isEqualTo(requests);
    }

    @Test
    void aRolledBackIssueDoesNotBurnANumber() {
        int year = 2027;
        TransactionTemplate tx = new TransactionTemplate(txManager);

        int first = tx.execute(status -> numbering.nextNumber(year));
        assertThat(first).isEqualTo(1);

        // A transaction that takes a number and then fails must not consume it.
        try {
            tx.execute(status -> {
                numbering.nextNumber(year); // would be 2
                throw new RuntimeException("invoice persistence failed after numbering");
            });
        } catch (RuntimeException expected) {
            // rolled back
        }

        // The next successful issue reuses number 2 — no gap.
        int next = tx.execute(status -> numbering.nextNumber(year));
        assertThat(next).isEqualTo(2);
    }
}

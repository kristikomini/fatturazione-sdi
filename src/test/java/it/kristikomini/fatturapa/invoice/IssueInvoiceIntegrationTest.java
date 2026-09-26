package it.kristikomini.fatturapa.invoice;

import it.kristikomini.fatturapa.outbox.OutboxRelay;
import it.kristikomini.fatturapa.sdi.SdiState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end issue → outbox → relay, against real PostgreSQL. Proves the transactional outbox: the
 * invoice and its dispatch event commit together; the relay publishes the event and moves the
 * invoice VALIDATA → INVIATA; and a second relay pass is idempotent (no re-send).
 *
 * <p>Skips cleanly without Docker (local sandbox); runs in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class IssueInvoiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired InvoiceIssueService issueService;
    @Autowired InvoiceRepository invoices;
    @Autowired it.kristikomini.fatturapa.outbox.OutboxEventRepository outbox;
    @Autowired OutboxRelay relay;

    private IssueInvoiceCommand command() {
        return new IssueInvoiceCommand(
                LocalDate.of(2026, 3, 10),
                new InvoiceDraft.Party("IT", "01234567890", "Officina Rossi S.r.l."),
                new InvoiceDraft.Party("IT", "09876543210", "Autotrasporti Bianchi S.p.A."),
                "SUBM70N",
                List.of(new InvoiceDraft.Line("Tagliando", new BigDecimal("1"), new BigDecimal("200.00"), new BigDecimal("22.00"))));
    }

    @Test
    void issuePersistsInvoiceAndOutboxThenRelayDispatches() {
        Invoice issued = issueService.issue(command());

        // Committed together: invoice VALIDATA + one pending outbox row.
        assertThat(issued.getState()).isEqualTo(SdiState.VALIDATA);
        assertThat(issued.getNumber()).isEqualTo(1);
        assertThat(outbox.findAll()).singleElement().satisfies(e -> assertThat(e.isPublished()).isFalse());

        // Relay publishes and advances the invoice to INVIATA.
        relay.publishPending();

        assertThat(invoices.findById(issued.getId())).get()
                .extracting(Invoice::getState).isEqualTo(SdiState.INVIATA);
        assertThat(outbox.findAll()).singleElement().satisfies(e -> assertThat(e.isPublished()).isTrue());

        // Idempotent: a second pass does not re-send or change state.
        relay.publishPending();
        assertThat(invoices.findById(issued.getId())).get()
                .extracting(Invoice::getState).isEqualTo(SdiState.INVIATA);
    }
}

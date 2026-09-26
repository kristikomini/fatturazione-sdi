package it.kristikomini.fatturapa.outbox;

import it.kristikomini.fatturapa.invoice.Invoice;
import it.kristikomini.fatturapa.invoice.InvoiceRepository;
import it.kristikomini.fatturapa.sdi.SdiClient;
import it.kristikomini.fatturapa.sdi.SdiStateMachine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes a single outbox event in its <b>own transaction</b>. It lives in a separate bean from
 * the relay on purpose: Spring's {@code @Transactional} works via proxies, so a relay calling its
 * own {@code @Transactional} method ({@code this.process(...)}) would bypass the proxy and run with
 * no transaction. Injecting this as a distinct bean makes the boundary real.
 *
 * <p>Idempotent: an already-published event is a no-op, so redelivery/replay is safe.
 */
@Component
public class OutboxEventProcessor {

    private final OutboxEventRepository outbox;
    private final InvoiceRepository invoices;
    private final SdiClient sdiClient;
    private final SdiStateMachine stateMachine;

    public OutboxEventProcessor(OutboxEventRepository outbox, InvoiceRepository invoices,
                                SdiClient sdiClient, SdiStateMachine stateMachine) {
        this.outbox = outbox;
        this.invoices = invoices;
        this.sdiClient = sdiClient;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public void process(Long eventId) {
        OutboxEvent event = outbox.findById(eventId).orElse(null);
        if (event == null || event.isPublished()) {
            return; // idempotent: nothing to do
        }
        event.recordAttempt();

        Invoice invoice = invoices.findById(Long.valueOf(event.getAggregateId()))
                .orElseThrow(() -> new IllegalStateException("invoice " + event.getAggregateId() + " not found"));

        sdiClient.send(invoice.getXml());                 // transmit
        invoice.moveTo(stateMachine.send(invoice.getState())); // VALIDATA -> INVIATA
        event.markPublished();                            // both flushed together on commit
    }
}

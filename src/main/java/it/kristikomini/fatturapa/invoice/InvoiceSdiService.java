package it.kristikomini.fatturapa.invoice;

import it.kristikomini.fatturapa.sdi.SdiNotification;
import it.kristikomini.fatturapa.sdi.SdiState;
import it.kristikomini.fatturapa.sdi.SdiStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies an inbound SDI notification to a persisted invoice, driving its lifecycle state. */
@Service
public class InvoiceSdiService {

    private final InvoiceRepository invoices;
    private final SdiStateMachine stateMachine;

    public InvoiceSdiService(InvoiceRepository invoices, SdiStateMachine stateMachine) {
        this.invoices = invoices;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public Invoice apply(Long invoiceId, SdiNotification notification) {
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        SdiState next = stateMachine.apply(invoice.getState(), notification); // validates the edge
        invoice.moveTo(next);
        return invoice;
    }

    @Transactional(readOnly = true)
    public Invoice get(Long invoiceId) {
        return invoices.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }
}

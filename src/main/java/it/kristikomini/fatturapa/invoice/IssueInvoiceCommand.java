package it.kristikomini.fatturapa.invoice;

import java.time.LocalDate;
import java.util.List;

/**
 * The command to issue a new invoice. Note it carries <b>no number</b> — the number is assigned by
 * the gap-less numbering service inside the issue transaction, never supplied by the caller.
 */
public record IssueInvoiceCommand(
        LocalDate date,
        InvoiceDraft.Party supplier,
        InvoiceDraft.Party customer,
        String codiceDestinatario,
        List<InvoiceDraft.Line> lines) {
}

package it.kristikomini.fatturapa.api;

import it.kristikomini.fatturapa.invoice.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Response view of an invoice — never the entity (standards rule 3). */
public record InvoiceResponse(
        Long id,
        String number,
        String state,
        LocalDate issueDate,
        BigDecimal totalAmount) {

    public static InvoiceResponse of(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getFormattedNumber(),
                invoice.getState().name(),
                invoice.getIssueDate(),
                invoice.getTotalAmount());
    }
}

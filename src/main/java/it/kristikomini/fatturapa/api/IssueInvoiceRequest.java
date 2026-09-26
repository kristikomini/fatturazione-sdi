package it.kristikomini.fatturapa.api;

import it.kristikomini.fatturapa.invoice.InvoiceDraft;
import it.kristikomini.fatturapa.invoice.IssueInvoiceCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request body to issue an invoice. Bean-validated at the controller boundary. No number field. */
public record IssueInvoiceRequest(
        @NotNull LocalDate date,
        @NotNull @Valid Party supplier,
        @NotNull @Valid Party customer,
        @NotBlank @Size(min = 6, max = 7) String codiceDestinatario,
        @NotEmpty @Valid List<Line> lines) {

    public record Party(
            @NotBlank @Size(min = 2, max = 2) String country,
            @NotBlank String vatCode,
            @NotBlank String name) {
    }

    public record Line(
            @NotBlank String description,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @PositiveOrZero BigDecimal unitPrice,
            @NotNull @PositiveOrZero BigDecimal vatRate) {
    }

    public IssueInvoiceCommand toCommand() {
        return new IssueInvoiceCommand(
                date,
                new InvoiceDraft.Party(supplier.country(), supplier.vatCode(), supplier.name()),
                new InvoiceDraft.Party(customer.country(), customer.vatCode(), customer.name()),
                codiceDestinatario,
                lines.stream()
                        .map(l -> new InvoiceDraft.Line(l.description(), l.quantity(), l.unitPrice(), l.vatRate()))
                        .toList());
    }
}

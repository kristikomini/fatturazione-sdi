package it.kristikomini.fatturapa.invoice;

import java.util.List;

/**
 * Raised when the generated FatturaPA XML fails XSD validation. Thrown from the issue transaction,
 * so it rolls back — and because numbering shares that transaction, a failed issue does not consume
 * an invoice number.
 */
public class InvoiceValidationException extends RuntimeException {

    private final transient List<String> errors;

    public InvoiceValidationException(List<String> errors) {
        super("Generated invoice failed FatturaPA validation: " + errors);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}

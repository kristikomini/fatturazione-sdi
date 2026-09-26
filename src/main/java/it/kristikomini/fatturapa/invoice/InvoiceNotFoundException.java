package it.kristikomini.fatturapa.invoice;

/** Raised when an invoice id does not exist — mapped to an RFC 7807 404. */
public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(Long id) {
        super("No invoice with id " + id);
    }
}

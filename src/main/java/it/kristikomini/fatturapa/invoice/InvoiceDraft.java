package it.kristikomini.fatturapa.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The business data needed to produce a FatturaPA document, independent of the XML shape and of
 * JPA. The XML builder turns this into the schema tree; the same model will back the persisted
 * entity. Records because it is immutable input.
 */
public record InvoiceDraft(
        String number,
        int year,
        LocalDate date,
        Party supplier,     // CedentePrestatore
        Party customer,     // CessionarioCommittente
        String codiceDestinatario,
        List<Line> lines) {

    /** A party (supplier or customer). {@code vatCode} is the P.IVA / partita IVA. */
    public record Party(String country, String vatCode, String name) {
    }

    /** One invoice line. VAT rate is a percentage, e.g. 22.00. */
    public record Line(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {

        /** Net line total = quantity × unit price, rounded to 2 dp (HALF_UP). */
        public BigDecimal netTotal() {
            return quantity.multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}

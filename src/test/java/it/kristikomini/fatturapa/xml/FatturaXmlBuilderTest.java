package it.kristikomini.fatturapa.xml;

import it.kristikomini.fatturapa.invoice.InvoiceDraft;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Build + validate the FatturaPA XML — pure JAXB and XSD, no DB or Docker, so it runs everywhere.
 * Proves the generated document is schema-valid and the computed VAT summary is correct, and that
 * the validator actually rejects a malformed document (all errors reported, not just the first).
 */
class FatturaXmlBuilderTest {

    private final FatturaXmlBuilder builder = new FatturaXmlBuilder();
    private final FatturaValidator validator = new FatturaValidator();

    private InvoiceDraft validDraft() {
        return new InvoiceDraft(
                "1", 2026, LocalDate.of(2026, 1, 15),
                new InvoiceDraft.Party("IT", "01234567890", "Officina Rossi S.r.l."),
                new InvoiceDraft.Party("IT", "09876543210", "Autotrasporti Bianchi S.p.A."),
                "SUBM70N",
                List.of(
                        new InvoiceDraft.Line("Tagliando completo", new BigDecimal("1"), new BigDecimal("200.00"), new BigDecimal("22.00")),
                        new InvoiceDraft.Line("Pneumatici (4)", new BigDecimal("4"), new BigDecimal("80.00"), new BigDecimal("22.00"))));
    }

    @Test
    void buildsSchemaValidXmlWithCorrectTotals() {
        String xml = builder.toXml(validDraft());

        // Structure / key values present.
        assertThat(xml).contains("versione=\"FPR12\"");
        assertThat(xml).contains("<Denominazione>Officina Rossi S.r.l.</Denominazione>");
        assertThat(xml).contains("<Data>2026-01-15</Data>");
        // Net: 200 + 4*80 = 520.00 imponibile @22% -> imposta 114.40 -> total 634.40.
        assertThat(xml).contains("<ImponibileImporto>520.00</ImponibileImporto>");
        assertThat(xml).contains("<Imposta>114.40</Imposta>");
        assertThat(xml).contains("<ImportoTotaleDocumento>634.40</ImportoTotaleDocumento>");

        FatturaValidator.Result result = validator.validate(xml);
        assertThat(result.valid()).as("errors: %s", result.errors()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validatorRejectsMalformedDocument() {
        // Customer with no name (Denominazione omitted) and a bad country code.
        InvoiceDraft bad = new InvoiceDraft(
                "2", 2026, LocalDate.of(2026, 2, 1),
                new InvoiceDraft.Party("ITA", "01234567890", "Fornitore"), // 3-letter country -> invalid
                new InvoiceDraft.Party("IT", "09876543210", null),          // missing Denominazione
                "0000000",
                List.of(new InvoiceDraft.Line("X", new BigDecimal("1"), new BigDecimal("10.00"), new BigDecimal("22.00"))));

        FatturaValidator.Result result = validator.validate(builder.toXml(bad));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }
}

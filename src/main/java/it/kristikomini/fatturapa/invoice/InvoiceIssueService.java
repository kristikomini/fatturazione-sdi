package it.kristikomini.fatturapa.invoice;

import it.kristikomini.fatturapa.numbering.InvoiceNumberingService;
import it.kristikomini.fatturapa.outbox.OutboxEvent;
import it.kristikomini.fatturapa.outbox.OutboxEventRepository;
import it.kristikomini.fatturapa.sdi.SdiState;
import it.kristikomini.fatturapa.sdi.SdiStateMachine;
import it.kristikomini.fatturapa.xml.FatturaElettronica;
import it.kristikomini.fatturapa.xml.FatturaValidator;
import it.kristikomini.fatturapa.xml.FatturaXmlBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Issues an invoice — the operation that ties the whole service together, in <b>one transaction</b>:
 *
 * <ol>
 *   <li>assign the next gap-less number for the year;</li>
 *   <li>build the FatturaPA XML and validate it against the XSD;</li>
 *   <li>if invalid → throw (rolls back → the number is not consumed);</li>
 *   <li>if valid → persist the invoice as {@code VALIDATA} and write an <b>outbox</b> row for SDI
 *       dispatch — both committed together, so an invoice can never exist without its dispatch
 *       intent, and vice versa.</li>
 * </ol>
 */
@Service
public class InvoiceIssueService {

    private final InvoiceNumberingService numbering;
    private final FatturaXmlBuilder xmlBuilder;
    private final FatturaValidator validator;
    private final SdiStateMachine stateMachine;
    private final InvoiceRepository invoices;
    private final OutboxEventRepository outbox;

    public InvoiceIssueService(InvoiceNumberingService numbering, FatturaXmlBuilder xmlBuilder,
                               FatturaValidator validator, SdiStateMachine stateMachine,
                               InvoiceRepository invoices, OutboxEventRepository outbox) {
        this.numbering = numbering;
        this.xmlBuilder = xmlBuilder;
        this.validator = validator;
        this.stateMachine = stateMachine;
        this.invoices = invoices;
        this.outbox = outbox;
    }

    @Transactional
    public Invoice issue(IssueInvoiceCommand command) {
        int year = command.date().getYear();
        int number = numbering.nextNumber(year); // gap-less, same transaction

        InvoiceDraft draft = new InvoiceDraft(String.valueOf(number), year, command.date(),
                command.supplier(), command.customer(), command.codiceDestinatario(), command.lines());

        FatturaElettronica document = xmlBuilder.build(draft);
        String xml = xmlBuilder.toXml(document);

        FatturaValidator.Result validation = validator.validate(xml);
        SdiState state = stateMachine.validate(SdiState.CREATA, validation.valid());
        if (!validation.valid()) {
            // Rolls back → the number is not burned (and no invoice/outbox row is written).
            throw new InvoiceValidationException(validation.errors());
        }

        BigDecimal total = document.getBody().getDatiGenerali()
                .getDatiGeneraliDocumento().getImportoTotaleDocumento();

        Invoice invoice = invoices.save(new Invoice(year, number, command.date(),
                command.supplier(), command.customer(), command.codiceDestinatario(), total, xml, state));

        // Transactional outbox: dispatch intent committed atomically with the invoice.
        outbox.save(new OutboxEvent("Invoice", invoice.getId().toString(),
                "InvoiceReadyForSdi", invoice.getFormattedNumber()));

        return invoice;
    }
}

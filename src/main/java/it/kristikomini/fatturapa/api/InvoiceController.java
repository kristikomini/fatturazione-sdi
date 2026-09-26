package it.kristikomini.fatturapa.api;

import it.kristikomini.fatturapa.invoice.Invoice;
import it.kristikomini.fatturapa.invoice.InvoiceIssueService;
import it.kristikomini.fatturapa.invoice.InvoiceSdiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** REST API for the e-invoicing service: issue an invoice, read it, and apply SDI notifications. */
@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceIssueService issueService;
    private final InvoiceSdiService sdiService;

    public InvoiceController(InvoiceIssueService issueService, InvoiceSdiService sdiService) {
        this.issueService = issueService;
        this.sdiService = sdiService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> issue(@Valid @RequestBody IssueInvoiceRequest request) {
        Invoice invoice = issueService.issue(request.toCommand());
        return ResponseEntity
                .created(URI.create("/invoices/" + invoice.getId()))
                .body(InvoiceResponse.of(invoice));
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) {
        return InvoiceResponse.of(sdiService.get(id));
    }

    /** Simulated SDI callback — drives the invoice's lifecycle (consegna / scarto / mancata consegna). */
    @PostMapping("/{id}/sdi-notifications")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceResponse applyNotification(@PathVariable Long id,
                                             @Valid @RequestBody SdiNotificationRequest request) {
        return InvoiceResponse.of(sdiService.apply(id, request.toNotification()));
    }
}

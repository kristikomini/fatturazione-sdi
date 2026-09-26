package it.kristikomini.fatturapa.api;

import it.kristikomini.fatturapa.invoice.Invoice;
import it.kristikomini.fatturapa.invoice.InvoiceDraft;
import it.kristikomini.fatturapa.invoice.InvoiceIssueService;
import it.kristikomini.fatturapa.invoice.InvoiceSdiService;
import it.kristikomini.fatturapa.sdi.SdiState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer slice test (no DB). Verifies the REST contract and bean-validation → 400. */
@WebMvcTest(InvoiceController.class)
@Import(ApiExceptionHandler.class)
class InvoiceControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    InvoiceIssueService issueService;
    @MockBean
    InvoiceSdiService sdiService;

    private Invoice sampleIssued() {
        return new Invoice(2026, 1, LocalDate.of(2026, 1, 15),
                new InvoiceDraft.Party("IT", "01234567890", "Officina Rossi S.r.l."),
                new InvoiceDraft.Party("IT", "09876543210", "Autotrasporti Bianchi S.p.A."),
                "SUBM70N", new BigDecimal("634.40"), "<xml/>", SdiState.VALIDATA);
    }

    private String validBody() {
        return """
            {
              "date": "2026-01-15",
              "supplier": {"country":"IT","vatCode":"01234567890","name":"Officina Rossi S.r.l."},
              "customer": {"country":"IT","vatCode":"09876543210","name":"Autotrasporti Bianchi S.p.A."},
              "codiceDestinatario": "SUBM70N",
              "lines": [{"description":"Tagliando","quantity":1,"unitPrice":200.00,"vatRate":22.00}]
            }
            """;
    }

    @Test
    void issuesInvoiceAndReturns201() throws Exception {
        when(issueService.issue(any())).thenReturn(sampleIssued());

        mvc.perform(post("/invoices").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("1/2026"))
                .andExpect(jsonPath("$.state").value("VALIDATA"))
                .andExpect(jsonPath("$.totalAmount").value(634.40));
    }

    @Test
    void rejectsInvalidBodyWith400() throws Exception {
        // Missing date, empty lines, bad codiceDestinatario.
        String bad = """
            {"supplier":{"country":"IT","vatCode":"1","name":"x"},
             "customer":{"country":"IT","vatCode":"2","name":"y"},
             "codiceDestinatario":"X","lines":[]}
            """;
        mvc.perform(post("/invoices").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appliesSdiNotification() throws Exception {
        when(sdiService.apply(any(), any())).thenReturn(sampleIssued());

        mvc.perform(post("/invoices/1/sdi-notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"DELIVERED\"}"))
                .andExpect(status().isOk());
    }
}

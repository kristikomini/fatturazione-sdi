package it.kristikomini.fatturapa.api;

import it.kristikomini.fatturapa.invoice.InvoiceNotFoundException;
import it.kristikomini.fatturapa.invoice.InvoiceValidationException;
import it.kristikomini.fatturapa.sdi.InvalidSdiTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/** Global RFC 7807 error handling (standards rule 4). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ProblemDetail notFound(InvoiceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Invoice not found");
        pd.setType(URI.create("https://kristikomini.it/problems/invoice-not-found"));
        return pd;
    }

    /** The generated document failed FatturaPA schema validation. 422 with the list of errors. */
    @ExceptionHandler(InvoiceValidationException.class)
    public ProblemDetail validation(InvoiceValidationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "The invoice does not conform to the FatturaPA schema");
        pd.setTitle("Invalid FatturaPA document");
        pd.setType(URI.create("https://kristikomini.it/problems/fatturapa-invalid"));
        pd.setProperty("errors", ex.getErrors());
        return pd;
    }

    /** An SDI notification asked for an illegal lifecycle transition. 409 Conflict. */
    @ExceptionHandler(InvalidSdiTransitionException.class)
    public ProblemDetail invalidTransition(InvalidSdiTransitionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Illegal SDI transition");
        pd.setType(URI.create("https://kristikomini.it/problems/sdi-illegal-transition"));
        return pd;
    }
}

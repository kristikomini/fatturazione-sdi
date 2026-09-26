package it.kristikomini.fatturapa.xml;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a FatturaPA XML document against {@code xsd/fatturapa-subset.xsd}. Returns a structured
 * {@link Result} listing every schema violation rather than throwing on the first — an invoice with
 * three problems reports all three, which is what an operator needs to fix it in one pass.
 *
 * <p>The {@link SchemaFactory} is configured to disable external DTDs/schemas (XXE hardening).
 */
@Component
public class FatturaValidator {

    private final Schema schema;

    public FatturaValidator() {
        this.schema = loadSchema();
    }

    public Result validate(String xml) {
        CollectingErrorHandler handler = new CollectingErrorHandler();
        try {
            Validator validator = schema.newValidator();
            validator.setErrorHandler(handler);
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (Exception e) {
            handler.errors.add("fatal: " + e.getMessage());
        }
        return new Result(handler.errors.isEmpty(), List.copyOf(handler.errors));
    }

    private Schema loadSchema() {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // XXE hardening: no external entity/schema resolution.
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(new StreamSource(new ClassPathResource("xsd/fatturapa-subset.xsd").getInputStream()));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load FatturaPA XSD", e);
        }
    }

    /** Validation outcome: valid flag plus the list of human-readable errors (empty when valid). */
    public record Result(boolean valid, List<String> errors) {
    }

    private static final class CollectingErrorHandler implements ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void warning(SAXParseException e) {
            // warnings are not failures
        }

        @Override
        public void error(SAXParseException e) {
            errors.add(format(e));
        }

        @Override
        public void fatalError(SAXParseException e) {
            errors.add(format(e));
        }

        private String format(SAXParseException e) {
            return "line " + e.getLineNumber() + ": " + e.getMessage();
        }
    }
}

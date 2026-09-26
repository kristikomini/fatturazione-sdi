package it.kristikomini.fatturapa.sdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A stand-in for the real SDI transport: accepts the document and returns a generated
 * IdentificativoSDI. Swap for a real SDICoop/PEC client without touching the relay or the outbox.
 */
@Component
public class SimulatedSdiClient implements SdiClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatedSdiClient.class);

    @Override
    public String send(String invoiceXml) {
        String idSdi = UUID.randomUUID().toString().substring(0, 8);
        log.info("Simulated SDI accept: {} bytes, IdentificativoSDI={}", invoiceXml.length(), idSdi);
        return idSdi;
    }
}

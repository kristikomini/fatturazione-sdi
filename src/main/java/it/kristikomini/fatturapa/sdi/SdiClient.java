package it.kristikomini.fatturapa.sdi;

/**
 * Sends an invoice document to the Sistema di Interscambio. A real implementation would transmit
 * over SDICoop/PEC and handle the transport-level acknowledgement; here it is a seam the outbox
 * relay calls, with a simulated implementation for local/test runs.
 */
public interface SdiClient {

    /** @return the SDI transmission identifier (IdentificativoSDI). */
    String send(String invoiceXml);
}

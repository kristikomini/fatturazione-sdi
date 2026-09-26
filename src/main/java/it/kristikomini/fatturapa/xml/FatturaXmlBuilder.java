package it.kristikomini.fatturapa.xml;

import it.kristikomini.fatturapa.invoice.InvoiceDraft;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a {@link FatturaElettronica} document from an {@link InvoiceDraft} and marshals it to XML.
 *
 * <p>It computes the two things the schema requires to be internally consistent: each line's
 * {@code PrezzoTotale} (net), and the {@code DatiRiepilogo} VAT summary grouped by rate
 * (imponibile + imposta), which also feeds {@code ImportoTotaleDocumento}. All money is rounded
 * to 2 decimals HALF_UP — the convention the Agenzia expects.
 */
@Component
public class FatturaXmlBuilder {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public FatturaElettronica build(InvoiceDraft draft) {
        FatturaElettronica f = new FatturaElettronica();

        // Header — transmission + the two parties.
        FatturaElettronica.DatiTrasmissione dt = f.getHeader().getDatiTrasmissione();
        dt.getIdTrasmittente().set(draft.supplier().country(), draft.supplier().vatCode());
        dt.setProgressivoInvio(draft.number());
        dt.setCodiceDestinatario(draft.codiceDestinatario());
        applyParty(f.getHeader().getCedentePrestatore(), draft.supplier());
        applyParty(f.getHeader().getCessionarioCommittente(), draft.customer());

        // Body — document header, lines, VAT summary.
        BigDecimal totalDocument = BigDecimal.ZERO;
        Map<BigDecimal, BigDecimal> imponibileByRate = new LinkedHashMap<>();

        var beni = f.getBody().getDatiBeniServizi();
        int lineNo = 1;
        for (InvoiceDraft.Line line : draft.lines()) {
            BigDecimal net = line.netTotal();
            FatturaElettronica.DettaglioLinee dl = new FatturaElettronica.DettaglioLinee();
            dl.set(lineNo++, line.description(), scale(line.quantity()), scale(line.unitPrice()), net,
                    scale(line.vatRate()));
            beni.getDettaglioLinee().add(dl);
            imponibileByRate.merge(scale(line.vatRate()), net, BigDecimal::add);
        }

        for (Map.Entry<BigDecimal, BigDecimal> e : imponibileByRate.entrySet()) {
            BigDecimal rate = e.getKey();
            BigDecimal imponibile = e.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal imposta = imponibile.multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            FatturaElettronica.DatiRiepilogo r = new FatturaElettronica.DatiRiepilogo();
            r.set(rate, imponibile, imposta);
            beni.getDatiRiepilogo().add(r);
            totalDocument = totalDocument.add(imponibile).add(imposta);
        }

        var doc = f.getBody().getDatiGenerali().getDatiGeneraliDocumento();
        doc.setData(draft.date().format(ISO));
        doc.setNumero(draft.number());
        doc.setImportoTotaleDocumento(totalDocument.setScale(2, RoundingMode.HALF_UP));
        return f;
    }

    public String toXml(InvoiceDraft draft) {
        return toXml(build(draft));
    }

    public String toXml(FatturaElettronica fattura) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(FatturaElettronica.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            m.marshal(fattura, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal FatturaElettronica", e);
        }
    }

    private void applyParty(FatturaElettronica.Soggetto soggetto, InvoiceDraft.Party party) {
        soggetto.getDatiAnagrafici().getIdFiscaleIVA().set(party.country(), party.vatCode());
        soggetto.getDatiAnagrafici().setDenominazione(party.name());
    }

    private BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

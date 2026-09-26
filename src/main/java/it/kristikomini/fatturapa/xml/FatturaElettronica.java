package it.kristikomini.fatturapa.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A JAXB model of the FatturaPA electronic-invoice document — a representative <b>subset</b> of the
 * Agenzia delle Entrate schema (the mandatory header/body blocks), not the full 1.2 schema. Element
 * names follow the real spec (PascalCase, Italian) so the shape is recognisable; the document is
 * validated against {@code src/main/resources/xsd/fatturapa-subset.xsd}, which matches this model.
 *
 * <p>Everything is expressed as nested static classes to keep the whole document shape readable in
 * one place. {@code propOrder} pins element ordering, which XSD sequence validation requires.
 */
@XmlRootElement(name = "FatturaElettronica")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"header", "body"})
public class FatturaElettronica {

    @XmlAttribute(name = "versione", required = true)
    private String versione = "FPR12";

    @XmlElement(name = "FatturaElettronicaHeader", required = true)
    private Header header = new Header();

    @XmlElement(name = "FatturaElettronicaBody", required = true)
    private Body body = new Body();

    public String getVersione() {
        return versione;
    }

    public void setVersione(String versione) {
        this.versione = versione;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    // ---- Header ----------------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"datiTrasmissione", "cedentePrestatore", "cessionarioCommittente"})
    public static class Header {
        @XmlElement(name = "DatiTrasmissione", required = true)
        private DatiTrasmissione datiTrasmissione = new DatiTrasmissione();
        @XmlElement(name = "CedentePrestatore", required = true)
        private Soggetto cedentePrestatore = new Soggetto();
        @XmlElement(name = "CessionarioCommittente", required = true)
        private Soggetto cessionarioCommittente = new Soggetto();

        public DatiTrasmissione getDatiTrasmissione() {
            return datiTrasmissione;
        }

        public Soggetto getCedentePrestatore() {
            return cedentePrestatore;
        }

        public Soggetto getCessionarioCommittente() {
            return cessionarioCommittente;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"idTrasmittente", "progressivoInvio", "formatoTrasmissione", "codiceDestinatario"})
    public static class DatiTrasmissione {
        @XmlElement(name = "IdTrasmittente", required = true)
        private IdFiscale idTrasmittente = new IdFiscale();
        @XmlElement(name = "ProgressivoInvio", required = true)
        private String progressivoInvio;
        @XmlElement(name = "FormatoTrasmissione", required = true)
        private String formatoTrasmissione = "FPR12";
        @XmlElement(name = "CodiceDestinatario", required = true)
        private String codiceDestinatario;

        public IdFiscale getIdTrasmittente() {
            return idTrasmittente;
        }

        public void setProgressivoInvio(String v) {
            this.progressivoInvio = v;
        }

        public void setCodiceDestinatario(String v) {
            this.codiceDestinatario = v;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"idPaese", "idCodice"})
    public static class IdFiscale {
        @XmlElement(name = "IdPaese", required = true)
        private String idPaese;
        @XmlElement(name = "IdCodice", required = true)
        private String idCodice;

        public void set(String paese, String codice) {
            this.idPaese = paese;
            this.idCodice = codice;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"datiAnagrafici"})
    public static class Soggetto {
        @XmlElement(name = "DatiAnagrafici", required = true)
        private DatiAnagrafici datiAnagrafici = new DatiAnagrafici();

        public DatiAnagrafici getDatiAnagrafici() {
            return datiAnagrafici;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"idFiscaleIVA", "denominazione"})
    public static class DatiAnagrafici {
        @XmlElement(name = "IdFiscaleIVA", required = true)
        private IdFiscale idFiscaleIVA = new IdFiscale();
        @XmlElement(name = "Denominazione", required = true)
        private String denominazione;

        public IdFiscale getIdFiscaleIVA() {
            return idFiscaleIVA;
        }

        public void setDenominazione(String v) {
            this.denominazione = v;
        }
    }

    // ---- Body ------------------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"datiGenerali", "datiBeniServizi"})
    public static class Body {
        @XmlElement(name = "DatiGenerali", required = true)
        private DatiGenerali datiGenerali = new DatiGenerali();
        @XmlElement(name = "DatiBeniServizi", required = true)
        private DatiBeniServizi datiBeniServizi = new DatiBeniServizi();

        public DatiGenerali getDatiGenerali() {
            return datiGenerali;
        }

        public DatiBeniServizi getDatiBeniServizi() {
            return datiBeniServizi;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"datiGeneraliDocumento"})
    public static class DatiGenerali {
        @XmlElement(name = "DatiGeneraliDocumento", required = true)
        private DatiGeneraliDocumento datiGeneraliDocumento = new DatiGeneraliDocumento();

        public DatiGeneraliDocumento getDatiGeneraliDocumento() {
            return datiGeneraliDocumento;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"tipoDocumento", "divisa", "data", "numero", "importoTotaleDocumento"})
    public static class DatiGeneraliDocumento {
        @XmlElement(name = "TipoDocumento", required = true)
        private String tipoDocumento = "TD01"; // fattura
        @XmlElement(name = "Divisa", required = true)
        private String divisa = "EUR";
        @XmlElement(name = "Data", required = true)
        private String data;
        @XmlElement(name = "Numero", required = true)
        private String numero;
        @XmlElement(name = "ImportoTotaleDocumento", required = true)
        private BigDecimal importoTotaleDocumento;

        public void setData(String v) {
            this.data = v;
        }

        public void setNumero(String v) {
            this.numero = v;
        }

        public void setImportoTotaleDocumento(BigDecimal v) {
            this.importoTotaleDocumento = v;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"dettaglioLinee", "datiRiepilogo"})
    public static class DatiBeniServizi {
        @XmlElement(name = "DettaglioLinee", required = true)
        private List<DettaglioLinee> dettaglioLinee = new ArrayList<>();
        @XmlElement(name = "DatiRiepilogo", required = true)
        private List<DatiRiepilogo> datiRiepilogo = new ArrayList<>();

        public List<DettaglioLinee> getDettaglioLinee() {
            return dettaglioLinee;
        }

        public List<DatiRiepilogo> getDatiRiepilogo() {
            return datiRiepilogo;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"numeroLinea", "descrizione", "quantita", "prezzoUnitario", "prezzoTotale", "aliquotaIVA"})
    public static class DettaglioLinee {
        @XmlElement(name = "NumeroLinea", required = true)
        private int numeroLinea;
        @XmlElement(name = "Descrizione", required = true)
        private String descrizione;
        @XmlElement(name = "Quantita", required = true)
        private BigDecimal quantita;
        @XmlElement(name = "PrezzoUnitario", required = true)
        private BigDecimal prezzoUnitario;
        @XmlElement(name = "PrezzoTotale", required = true)
        private BigDecimal prezzoTotale;
        @XmlElement(name = "AliquotaIVA", required = true)
        private BigDecimal aliquotaIVA;

        public void set(int linea, String desc, BigDecimal qta, BigDecimal prezzo,
                        BigDecimal totale, BigDecimal iva) {
            this.numeroLinea = linea;
            this.descrizione = desc;
            this.quantita = qta;
            this.prezzoUnitario = prezzo;
            this.prezzoTotale = totale;
            this.aliquotaIVA = iva;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"aliquotaIVA", "imponibileImporto", "imposta"})
    public static class DatiRiepilogo {
        @XmlElement(name = "AliquotaIVA", required = true)
        private BigDecimal aliquotaIVA;
        @XmlElement(name = "ImponibileImporto", required = true)
        private BigDecimal imponibileImporto;
        @XmlElement(name = "Imposta", required = true)
        private BigDecimal imposta;

        public void set(BigDecimal iva, BigDecimal imponibile, BigDecimal imposta) {
            this.aliquotaIVA = iva;
            this.imponibileImporto = imponibile;
            this.imposta = imposta;
        }
    }
}

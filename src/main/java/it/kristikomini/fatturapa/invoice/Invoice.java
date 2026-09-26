package it.kristikomini.fatturapa.invoice;

import it.kristikomini.fatturapa.sdi.SdiState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A persisted invoice. Its {@code number} is assigned by the gap-less numbering service and its
 * {@code (year, number)} uniqueness is enforced by the database — the legal invariant lives in the
 * schema, not just the code. The generated FatturaPA XML is stored verbatim (you keep the document
 * you sent), and {@link #state} tracks the SDI lifecycle.
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int number;

    @Column(name = "formatted_number", nullable = false, unique = true)
    private String formattedNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SdiState state;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "supplier_vat", nullable = false)
    private String supplierVat;
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;
    @Column(name = "customer_vat", nullable = false)
    private String customerVat;
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    @Column(name = "codice_destinatario", nullable = false, length = 7)
    private String codiceDestinatario;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "xml", nullable = false)
    private String xml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Invoice() {
    }

    public Invoice(int year, int number, LocalDate issueDate, InvoiceDraft.Party supplier,
                   InvoiceDraft.Party customer, String codiceDestinatario, BigDecimal totalAmount,
                   String xml, SdiState state) {
        this.year = year;
        this.number = number;
        this.formattedNumber = number + "/" + year;
        this.issueDate = issueDate;
        this.supplierVat = supplier.vatCode();
        this.supplierName = supplier.name();
        this.customerVat = customer.vatCode();
        this.customerName = customer.name();
        this.codiceDestinatario = codiceDestinatario;
        this.totalAmount = totalAmount;
        this.xml = xml;
        this.state = state;
        this.createdAt = LocalDateTime.now();
    }

    /** Moves the invoice to a new SDI state (the caller validates the transition first). */
    public void moveTo(SdiState newState) {
        this.state = newState;
    }

    public Long getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public int getNumber() {
        return number;
    }

    public String getFormattedNumber() {
        return formattedNumber;
    }

    public SdiState getState() {
        return state;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getXml() {
        return xml;
    }
}

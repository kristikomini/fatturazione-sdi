# The SDI state machine

The Sistema di Interscambio is asynchronous: you send an invoice, then receive notifications
back over time. The invoice is a **state machine**, and the notifications are the events that
drive transitions.

```
  CREATA
    │  build XML
    ▼
  VALIDATA ──(XSD invalid)──▶ ERRORE_VALIDAZIONE   (never sent; fix and re-issue)
    │  send to SDI (outbox relay)
    ▼
  INVIATA
    ├──(RicevutaConsegna)────────▶ CONSEGNATA        ✅ delivered
    ├──(NotificaScarto + codici)─▶ SCARTATA          ❌ rejected — must correct & resend
    └──(NotificaMancataConsegna)─▶ MANCATA_CONSEGNA  ⏳ SDI couldn't deliver; retry window
```

## Rejection (`scarto`) codes to model

A `NotificaScarto` carries a list of `Errore` entries (`Codice` + `Descrizione`). A realistic
subset to handle explicitly (illustrative):

- `00001` — nome file non valido
- `00003` — nome file duplicato
- `00200` — file non conforme al formato
- `00404` — fattura duplicata
- `00427` — codice destinatario non valido

Each maps to an operator-facing message and a decision: correct-and-resend vs manual review.

## Invariants

- Only `VALIDATA` invoices may be sent.
- A `SCARTATA` invoice keeps its number? **No** — per the rules, a rejected invoice was never
  legally issued; the resend uses the *next* number. This is captured in a test.
- Transitions are append-only events (audit trail), not in-place status overwrites.

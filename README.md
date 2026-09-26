# Fatturazione Elettronica — FatturaPA & the SDI lifecycle

A backend service for **Italian electronic invoicing**: it builds the **FatturaPA** XML,
validates it against the Agenzia delle Entrate XSD, assigns a **gap-less sequential invoice
number** safely under concurrency, and manages the invoice through the **Sistema di
Interscambio (SDI)** state machine — including the rejection (`scarto`) codes you have to
handle in real life.

This is a uniquely Italian problem with real depth: a **legal** numbering constraint, an XML
integration with validation and rejection rules, and a reliability requirement (an invoice
must never be silently lost between "saved" and "sent").

> Modelled on the public FatturaPA specification. It targets SDI **test/simulation**
> behaviour — it does not transmit real invoices to the Agenzia delle Entrate.

## What it does

- **Build** FatturaPA 1.2.x XML from a domain invoice (JAXB), with the mandatory blocks
  (`DatiTrasmissione`, `CedentePrestatore`, `CessionarioCommittente`, `DatiGenerali`,
  `DatiBeniServizi`, `DatiRiepilogo`).
- **Validate** against the official XSD before anything leaves the building; surface schema
  errors as structured, actionable problems (RFC 7807).
- **Number** invoices sequentially, per year, **no gaps** — enforced under concurrent requests.
- **Track SDI state**: `CREATA → VALIDATA → INVIATA → (CONSEGNATA | SCARTATA | MANCATA_CONSEGNA)`,
  consuming SDI notifications (`RicevutaConsegna`, `NotificaScarto` with error list).
- **Dispatch reliably** via the **transactional outbox** — the invoice and its "to-send" record
  commit in one transaction; a relay ships it and records the SDI response.

## Why the numbering is the hard part

Invoice numbering is a **legal constraint**, not a business preference: sequential, no gaps,
per year. It is one of the few places where "we'll just use a UUID" is actually illegal. Two
requests that both grab "next number" must not get the same one, and a rolled-back transaction
must not burn a number. The design and its trade-offs (DB sequence vs `SELECT … FOR UPDATE`
counter vs advisory lock) are in [`docs/NUMBERING.md`](docs/NUMBERING.md).

## What this demonstrates (CV bullets)

*Proven by tests in this repo (Testcontainers tests run in CI):*
- Built a FatturaPA e-invoicing service: JAXB XML generation + XSD validation (reports all errors)
  + the SDI state machine (consegna / scarto / mancata consegna) with a catalog of 5 real scarto
  codes; illegal lifecycle transitions are rejected.
- Implemented gap-less, per-year sequential invoice numbering with `SELECT … FOR UPDATE` +
  `INSERT … ON CONFLICT DO NOTHING`, proven under **50 concurrent requests** (numbers 1..50, no
  gaps, no duplicates) and proven not to burn a number on rollback.
- Guaranteed at-least-once SDI dispatch with a **transactional outbox** (invoice + event committed
  together) and an idempotent polling relay that advances the invoice VALIDATA → INVIATA;
  end-to-end verified against real Postgres.
- REST API (issue / read / apply SDI notification) with bean validation and RFC 7807 errors.

## Run it

```bash
docker compose up      # app + postgres (+ a stub SDI receiver)
```

## Course modules exercised

18 (API design & RFC 7807), 23 (transactions & rollback rules), 08 (concurrency), 20 (security),
26 (messaging & the outbox), 25 (SQL & migrations).

package it.kristikomini.fatturapa.sdi;

import java.time.Instant;
import java.util.List;

/**
 * A notification received from SDI. Modelled as a Java 21 <b>sealed interface</b>: the set of
 * notification kinds is closed and known, so handling them can be an exhaustive {@code switch}
 * with no {@code default} — the compiler flags a new kind that is not handled.
 *
 * <p>Each notification knows the {@link SdiState} it drives the invoice into; the
 * {@link SdiStateMachine} still checks the transition is legal from the current state.
 */
public sealed interface SdiNotification
        permits SdiNotification.RicevutaConsegna,
                SdiNotification.NotificaScarto,
                SdiNotification.MancataConsegna {

    String identificativoSdi();

    Instant timestamp();

    SdiState targetState();

    /** Delivery receipt — the happy path. */
    record RicevutaConsegna(String identificativoSdi, Instant timestamp) implements SdiNotification {
        @Override
        public SdiState targetState() {
            return SdiState.CONSEGNATA;
        }
    }

    /** Rejection — carries the list of errors (codice + descrizione) the invoice must be corrected for. */
    record NotificaScarto(String identificativoSdi, Instant timestamp, List<SdiError> errors)
            implements SdiNotification {
        @Override
        public SdiState targetState() {
            return SdiState.SCARTATA;
        }
    }

    /** SDI could not deliver to the recipient (but the invoice is legally issued). */
    record MancataConsegna(String identificativoSdi, Instant timestamp) implements SdiNotification {
        @Override
        public SdiState targetState() {
            return SdiState.MANCATA_CONSEGNA;
        }
    }
}

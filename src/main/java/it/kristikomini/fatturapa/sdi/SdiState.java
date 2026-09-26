package it.kristikomini.fatturapa.sdi;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The lifecycle of an electronic invoice through the Sistema di Interscambio (SDI).
 *
 * <p>SDI is asynchronous: you send an invoice, then receive notifications back over time. The
 * invoice is a <b>state machine</b>, and the notifications are the events that drive transitions.
 * The legal allowed-transitions are encoded here so an invalid move (e.g. sending an invoice that
 * failed validation) is impossible, not merely discouraged.
 *
 * <pre>
 *   CREATA ──validate ok──▶ VALIDATA ──send──▶ INVIATA ──RicevutaConsegna──▶ CONSEGNATA (final)
 *     │                                            ├──NotificaScarto───────▶ SCARTATA   (final)
 *     └──validate fail──▶ ERRORE_VALIDAZIONE       └──MancataConsegna─────▶ MANCATA_CONSEGNA
 *                              (final)                                           │
 *                                                        (SDI later delivers)────┘──▶ CONSEGNATA
 * </pre>
 */
public enum SdiState {

    CREATA,
    VALIDATA,
    ERRORE_VALIDAZIONE,
    INVIATA,
    CONSEGNATA,
    SCARTATA,
    MANCATA_CONSEGNA;

    private static final Map<SdiState, Set<SdiState>> ALLOWED = new EnumMap<>(SdiState.class);

    static {
        ALLOWED.put(CREATA, EnumSet.of(VALIDATA, ERRORE_VALIDAZIONE));
        ALLOWED.put(VALIDATA, EnumSet.of(INVIATA));
        ALLOWED.put(INVIATA, EnumSet.of(CONSEGNATA, SCARTATA, MANCATA_CONSEGNA));
        ALLOWED.put(MANCATA_CONSEGNA, EnumSet.of(CONSEGNATA));
        // Terminal states have no outgoing transitions.
        ALLOWED.put(CONSEGNATA, EnumSet.noneOf(SdiState.class));
        ALLOWED.put(SCARTATA, EnumSet.noneOf(SdiState.class));
        ALLOWED.put(ERRORE_VALIDAZIONE, EnumSet.noneOf(SdiState.class));
    }

    public boolean canTransitionTo(SdiState target) {
        return ALLOWED.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public boolean isTerminal() {
        return ALLOWED.getOrDefault(this, Collections.emptySet()).isEmpty();
    }
}

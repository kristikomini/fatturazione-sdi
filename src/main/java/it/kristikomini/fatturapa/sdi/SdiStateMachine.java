package it.kristikomini.fatturapa.sdi;

import org.springframework.stereotype.Component;

/**
 * Applies invoice lifecycle transitions, enforcing the legal edges declared on {@link SdiState}.
 * Stateless — it computes the next state and refuses illegal moves; persistence of the transition
 * (as an append-only event) is the caller's concern.
 */
@Component
public class SdiStateMachine {

    /** CREATA → VALIDATA if the XML passed XSD validation, else → ERRORE_VALIDAZIONE. */
    public SdiState validate(SdiState current, boolean schemaValid) {
        return transition(current, schemaValid ? SdiState.VALIDATA : SdiState.ERRORE_VALIDAZIONE);
    }

    /** VALIDATA → INVIATA. Only a validated invoice may be sent. */
    public SdiState send(SdiState current) {
        return transition(current, SdiState.INVIATA);
    }

    /** Applies an inbound SDI notification, driving the invoice to the notification's target state. */
    public SdiState apply(SdiState current, SdiNotification notification) {
        return transition(current, notification.targetState());
    }

    private SdiState transition(SdiState current, SdiState target) {
        if (!current.canTransitionTo(target)) {
            throw new InvalidSdiTransitionException(current, target);
        }
        return target;
    }
}

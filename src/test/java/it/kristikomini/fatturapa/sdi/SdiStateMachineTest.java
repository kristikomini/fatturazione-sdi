package it.kristikomini.fatturapa.sdi;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SdiStateMachineTest {

    private final SdiStateMachine machine = new SdiStateMachine();

    @Test
    void happyPathToDelivered() {
        SdiState s = SdiState.CREATA;
        s = machine.validate(s, true);
        assertThat(s).isEqualTo(SdiState.VALIDATA);
        s = machine.send(s);
        assertThat(s).isEqualTo(SdiState.INVIATA);
        s = machine.apply(s, new SdiNotification.RicevutaConsegna("SDI-1", Instant.now()));
        assertThat(s).isEqualTo(SdiState.CONSEGNATA);
        assertThat(s.isTerminal()).isTrue();
    }

    @Test
    void rejectionCarriesErrorCodesAndIsTerminal() {
        SdiState sent = SdiState.INVIATA;
        var scarto = new SdiNotification.NotificaScarto("SDI-2", Instant.now(),
                List.of(SdiError.of(SdiError.ScartoCode.CODICE_DESTINATARIO_NON_VALIDO)));

        SdiState result = machine.apply(sent, scarto);

        assertThat(result).isEqualTo(SdiState.SCARTATA);
        assertThat(result.isTerminal()).isTrue();
        assertThat(scarto.errors()).singleElement()
                .satisfies(e -> assertThat(e.codice()).isEqualTo("00427"));
    }

    @Test
    void validationFailureGoesToErrorState() {
        assertThat(machine.validate(SdiState.CREATA, false)).isEqualTo(SdiState.ERRORE_VALIDAZIONE);
    }

    @Test
    void cannotSendAnInvoiceThatWasNotValidated() {
        assertThatThrownBy(() -> machine.send(SdiState.CREATA))
                .isInstanceOf(InvalidSdiTransitionException.class);
    }

    @Test
    void cannotDeliverAnInvoiceThatWasNotSent() {
        assertThatThrownBy(() ->
                machine.apply(SdiState.VALIDATA, new SdiNotification.RicevutaConsegna("SDI-3", Instant.now())))
                .isInstanceOf(InvalidSdiTransitionException.class);
    }

    @Test
    void mancataConsegnaCanLaterBecomeDelivered() {
        SdiState s = machine.apply(SdiState.INVIATA, new SdiNotification.MancataConsegna("SDI-4", Instant.now()));
        assertThat(s).isEqualTo(SdiState.MANCATA_CONSEGNA);
        s = machine.apply(s, new SdiNotification.RicevutaConsegna("SDI-4", Instant.now()));
        assertThat(s).isEqualTo(SdiState.CONSEGNATA);
    }
}

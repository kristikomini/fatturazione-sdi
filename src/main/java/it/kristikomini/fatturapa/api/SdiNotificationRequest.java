package it.kristikomini.fatturapa.api;

import it.kristikomini.fatturapa.sdi.SdiError;
import it.kristikomini.fatturapa.sdi.SdiNotification;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A simulated inbound SDI notification (in production these arrive from SDICoop/PEC). {@code kind}
 * picks the notification type; {@code errorCodes} apply only to a rejection.
 */
public record SdiNotificationRequest(
        @NotNull Kind kind,
        String identificativoSdi,
        List<SdiError.ScartoCode> errorCodes) {

    public enum Kind {
        DELIVERED,       // RicevutaConsegna
        REJECTED,        // NotificaScarto
        NOT_DELIVERED    // MancataConsegna
    }

    public SdiNotification toNotification() {
        String idSdi = identificativoSdi != null ? identificativoSdi : UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();
        return switch (kind) {
            case DELIVERED -> new SdiNotification.RicevutaConsegna(idSdi, now);
            case NOT_DELIVERED -> new SdiNotification.MancataConsegna(idSdi, now);
            case REJECTED -> new SdiNotification.NotificaScarto(idSdi, now,
                    (errorCodes == null ? List.<SdiError.ScartoCode>of() : errorCodes).stream()
                            .map(SdiError::of).toList());
        };
    }
}

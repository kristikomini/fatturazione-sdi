package it.kristikomini.fatturapa.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls the outbox and publishes pending events. Each event is handled in its own transaction by
 * {@link OutboxEventProcessor}, so one failing event does not roll back the others — it is simply
 * left unpublished and retried on the next poll. (A production-grade alternative is Debezium CDC
 * tailing the outbox table; polling needs no extra infrastructure.)
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH = 50;

    private final OutboxEventRepository outbox;
    private final OutboxEventProcessor processor;

    public OutboxRelay(OutboxEventRepository outbox, OutboxEventProcessor processor) {
        this.outbox = outbox;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:2000}")
    public void publishPending() {
        List<OutboxEvent> pending = outbox.findByPublishedAtIsNullOrderByCreatedAtAsc(Limit.of(BATCH));
        for (OutboxEvent event : pending) {
            try {
                processor.process(event.getId());
            } catch (RuntimeException e) {
                // Left unpublished; retried next poll. Logged, not fatal to the batch.
                log.warn("Outbox event {} failed to publish (attempt logged): {}", event.getId(), e.getMessage());
            }
        }
    }
}

package com.medrdv.praticiens.kafka;

import com.medrdv.common.events.SlotRejectedEvent;
import com.medrdv.common.events.SlotReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PraticiensKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_SLOT_RESERVED = "slot-reserved";
    public static final String TOPIC_SLOT_REJECTED = "slot-rejected";

    public void sendSlotReserved(SlotReservedEvent event) {
        log.info("Publication sur {}: {}", TOPIC_SLOT_RESERVED, event);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC_SLOT_RESERVED, String.valueOf(event.getAppointmentId()), event);
            } catch (Exception e) {
                log.warn("Erreur envoi Kafka (topic {}): {}", TOPIC_SLOT_RESERVED, e.getMessage());
            }
        });
    }

    public void sendSlotRejected(SlotRejectedEvent event) {
        log.info("Publication sur {}: {}", TOPIC_SLOT_REJECTED, event);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC_SLOT_REJECTED, String.valueOf(event.getAppointmentId()), event);
            } catch (Exception e) {
                log.warn("Erreur envoi Kafka (topic {}): {}", TOPIC_SLOT_REJECTED, e.getMessage());
            }
        });
    }
}

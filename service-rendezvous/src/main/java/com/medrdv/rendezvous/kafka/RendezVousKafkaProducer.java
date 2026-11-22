package com.medrdv.rendezvous.kafka;

import com.medrdv.common.events.AppointmentBookedEvent;
import com.medrdv.common.events.AppointmentCancelledEvent;
import com.medrdv.common.events.AppointmentRequestedEvent;
import com.medrdv.common.events.ConsultationCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class RendezVousKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_APPOINTMENT_REQUESTED = "appointment-requested";
    public static final String TOPIC_APPOINTMENT_BOOKED = "appointment-booked";
    public static final String TOPIC_APPOINTMENT_CANCELLED = "appointment-cancelled";
    public static final String TOPIC_CONSULTATION_COMPLETED = "consultation-completed";

    public void sendAppointmentRequested(AppointmentRequestedEvent event) {
        log.info("Émission topic {}: {}", TOPIC_APPOINTMENT_REQUESTED, event);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC_APPOINTMENT_REQUESTED, String.valueOf(event.getAppointmentId()), event);
            } catch (Exception e) {
                log.warn("Erreur envoi Kafka (topic {}): {}", TOPIC_APPOINTMENT_REQUESTED, e.getMessage());
            }
        });
    }

    public void sendAppointmentBooked(AppointmentBookedEvent event) {
        log.info("Émission topic {}: {}", TOPIC_APPOINTMENT_BOOKED, event);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC_APPOINTMENT_BOOKED, String.valueOf(event.getAppointmentId()), event);
            } catch (Exception e) {
                log.warn("Erreur envoi Kafka (topic {}): {}", TOPIC_APPOINTMENT_BOOKED, e.getMessage());
            }
        });
    }

    public void sendAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Émission topic {}: {}", TOPIC_APPOINTMENT_CANCELLED, event);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC_APPOINTMENT_CANCELLED, String.valueOf(event.getAppointmentId()), event);
            } catch (Exception e) {
                log.warn("Erreur envoi Kafka (topic {}): {}", TOPIC_APPOINTMENT_CANCELLED, e.getMessage());
            }
        });
    }

    public void sendConsultationCompleted(ConsultationCompletedEvent event) {
        log.info("Émission topic {}: {}", TOPIC_CONSULTATION_COMPLETED, event);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(TOPIC_CONSULTATION_COMPLETED, String.valueOf(event.getAppointmentId()), event);
            } catch (Exception e) {
                log.warn("Erreur envoi Kafka (topic {}): {}", TOPIC_CONSULTATION_COMPLETED, e.getMessage());
            }
        });
    }
}

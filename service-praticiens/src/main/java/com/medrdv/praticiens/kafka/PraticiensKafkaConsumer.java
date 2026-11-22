package com.medrdv.praticiens.kafka;

import com.medrdv.common.events.AppointmentCancelledEvent;
import com.medrdv.common.events.AppointmentRequestedEvent;
import com.medrdv.common.events.SlotRejectedEvent;
import com.medrdv.common.events.SlotReservedEvent;
import com.medrdv.praticiens.entity.Availability;
import com.medrdv.praticiens.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PraticiensKafkaConsumer {

    private final AvailabilityService availabilityService;
    private final PraticiensKafkaProducer producer;

    @KafkaListener(topics = "appointment-requested", groupId = "praticiens-service-group")
    public void handleAppointmentRequested(AppointmentRequestedEvent event) {
        log.info("Réception de l'événement appointment-requested: {}", event);

        try {
            Availability reservedSlot = availabilityService.reserveSlotAtomic(
                    event.getAvailabilityId(),
                    event.getDoctorId());

            SlotReservedEvent successEvent = SlotReservedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .appointmentId(event.getAppointmentId())
                    .availabilityId(reservedSlot.getId())
                    .doctorId(event.getDoctorId())
                    .patientId(event.getPatientId())
                    .slotDate(reservedSlot.getDateHeure())
                    .timestamp(LocalDateTime.now())
                    .build();

            producer.sendSlotReserved(successEvent);

        } catch (Exception ex) {
            log.error("Échec de réservation du créneau {}: {}", event.getAvailabilityId(), ex.getMessage());

            SlotRejectedEvent rejectedEvent = SlotRejectedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .appointmentId(event.getAppointmentId())
                    .availabilityId(event.getAvailabilityId())
                    .doctorId(event.getDoctorId())
                    .reason(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            producer.sendSlotRejected(rejectedEvent);
        }
    }

    @KafkaListener(topics = "appointment-cancelled", groupId = "praticiens-service-group")
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Réception de appointment-cancelled pour le créneau: {}", event.getAvailabilityId());
        if (event.getAvailabilityId() != null) {
            availabilityService.releaseSlot(event.getAvailabilityId());
        }
    }
}

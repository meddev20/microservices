package com.medrdv.rendezvous.kafka;

import com.medrdv.common.events.SlotRejectedEvent;
import com.medrdv.common.events.SlotReservedEvent;
import com.medrdv.rendezvous.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RendezVousKafkaConsumer {

    private final AppointmentService appointmentService;

    @KafkaListener(topics = "slot-reserved", groupId = "rendezvous-service-group")
    public void handleSlotReserved(SlotReservedEvent event) {
        log.info("Réception de l'événement slot-reserved pour le RDV {}: {}", event.getAppointmentId(), event);
        appointmentService.confirmAppointment(event.getAppointmentId());
    }

    @KafkaListener(topics = "slot-rejected", groupId = "rendezvous-service-group")
    public void handleSlotRejected(SlotRejectedEvent event) {
        log.warn("Réception de l'événement slot-rejected pour le RDV {}: {}", event.getAppointmentId(),
                event.getReason());
        appointmentService.rejectAppointment(event.getAppointmentId(), event.getReason());
    }
}

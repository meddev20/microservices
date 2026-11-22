package com.medrdv.patients.kafka;

import com.medrdv.common.events.ConsultationCompletedEvent;
import com.medrdv.patients.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientsKafkaConsumer {

    private final MedicalRecordService medicalRecordService;

    @KafkaListener(topics = "consultation-completed", groupId = "patients-service-group")
    public void handleConsultationCompleted(ConsultationCompletedEvent event) {
        log.info("Réception de l'événement consultation-completed pour le patient {}: {}", event.getPatientId(), event);

        medicalRecordService.updateConsultationHistory(
                event.getPatientId(),
                event.getConsultationDate(),
                event.getMotif()
        );
    }
}


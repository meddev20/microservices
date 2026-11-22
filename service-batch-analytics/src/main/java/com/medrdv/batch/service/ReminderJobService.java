package com.medrdv.batch.service;

import com.medrdv.batch.client.RendezVousBatchClient;
import com.medrdv.common.dto.EnrichedAppointmentDTO;
import com.medrdv.common.enums.AppointmentStatus;
import com.medrdv.common.events.ReminderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderJobService {

    private final RendezVousBatchClient rendezVousClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_APPOINTMENT_REMINDER = "appointment-reminder";

    /**
     * Tâche planifiée quotidienne exécutée à 08h00.
     * Envoie des rappels par email pour tous les RDV CONFIRME ayant lieu le lendemain (J-1).
     */
    @Scheduled(cron = "0 0 8 * * *")
    public int runDailyReminders() {
        log.info("Démarrage du Job Batch : Rappels J-1 de consultations...");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int remindersSent = 0;

        try {
            List<Map<String, Object>> appointments = rendezVousClient.getAllAppointments();

            for (Map<String, Object> rawApp : appointments) {
                Long id = Long.valueOf(rawApp.get("id").toString());
                String statusStr = rawApp.get("statut") != null ? rawApp.get("statut").toString() : "";

                if (AppointmentStatus.CONFIRME.name().equalsIgnoreCase(statusStr)) {
                    EnrichedAppointmentDTO enriched = rendezVousClient.getEnrichedAppointment(id);

                    if (enriched.getCreneau() != null && enriched.getCreneau().getDateHeure() != null) {
                        LocalDate slotDate = enriched.getCreneau().getDateHeure().toLocalDate();

                        if (slotDate.equals(tomorrow)) {
                            ReminderNotificationEvent reminder = ReminderNotificationEvent.builder()
                                    .eventId(UUID.randomUUID().toString())
                                    .appointmentId(id)
                                    .patientId(enriched.getPatient() != null ? enriched.getPatient().getId() : null)
                                    .doctorId(enriched.getPraticien() != null ? enriched.getPraticien().getId() : null)
                                    .appointmentTime(enriched.getCreneau().getDateHeure())
                                    .motif(enriched.getMotif())
                                    .timestamp(LocalDateTime.now())
                                    .build();

                            try {
                                kafkaTemplate.send(TOPIC_APPOINTMENT_REMINDER, String.valueOf(id), reminder);
                            } catch (Exception e) {
                                log.warn("Erreur envoi Kafka reminder: {}", e.getMessage());
                            }
                            log.info("Rappel J-1 émis pour le RDV #{} (Patient: {})", id, reminder.getPatientId());
                            remindersSent++;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Erreur durant l'exécution du batch de rappels J-1: {}", ex.getMessage());
        }

        log.info("Fin du Job Batch Rappels J-1. Total rappels émis : {}", remindersSent);
        return remindersSent;
    }
}


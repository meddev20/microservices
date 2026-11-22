package com.medrdv.notification.kafka;

import com.medrdv.common.events.AppointmentBookedEvent;
import com.medrdv.common.events.AppointmentCancelledEvent;
import com.medrdv.common.events.ReminderNotificationEvent;
import com.medrdv.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "appointment-booked", groupId = "notification-service-group")
    public void handleAppointmentBooked(AppointmentBookedEvent event) {
        log.info("Réception de appointment-booked pour le RDV: {}", event.getAppointmentId());

        String subject = "Confirmation de votre rendez-vous Med-RDV #" + event.getAppointmentId();
        String body = String.format(
                "Bonjour,\n\nVotre rendez-vous #%d pour le motif '%s' a bien été confirmé.\n" +
                "Le praticien a réservé votre créneau.\n\nCordialement,\nL'équipe Med-RDV",
                event.getAppointmentId(), event.getMotif()
        );

        // Simulation d'envoi vers l'adresse patient
        emailService.sendEmail("patient-" + event.getPatientId() + "@medrdv.fr", subject, body, "CONFIRMATION");
    }

    @KafkaListener(topics = "appointment-cancelled", groupId = "notification-service-group")
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Réception de appointment-cancelled pour le RDV: {}", event.getAppointmentId());

        String subject = "Annulation de votre rendez-vous Med-RDV #" + event.getAppointmentId();
        String body = String.format(
                "Bonjour,\n\nVotre rendez-vous #%d a été annulé.\nMotif: %s\n\n" +
                "N'hésitez pas à choisir un autre créneau sur la plateforme.\n\nCordialement,\nL'équipe Med-RDV",
                event.getAppointmentId(), event.getReason()
        );

        emailService.sendEmail("patient-" + event.getPatientId() + "@medrdv.fr", subject, body, "ANNULATION");
    }

    @KafkaListener(topics = "appointment-reminder", groupId = "notification-service-group")
    public void handleAppointmentReminder(ReminderNotificationEvent event) {
        log.info("Réception de appointment-reminder pour le RDV: {}", event.getAppointmentId());

        String subject = "Rappel J-1 : Votre rendez-vous Med-RDV demain #" + event.getAppointmentId();
        String body = String.format(
                "Bonjour,\n\nCeci est un rappel pour votre consultation prévue le %s pour le motif '%s'.\n" +
                "En cas d'empêchement, merci d'annuler votre rendez-vous dès que possible sur la plateforme.\n\n" +
                "Cordialement,\nL'équipe Med-RDV",
                event.getAppointmentTime() != null ? event.getAppointmentTime().toString() : "demain",
                event.getMotif()
        );

        emailService.sendEmail("patient-" + event.getPatientId() + "@medrdv.fr", subject, body, "RAPPEL_J1");
    }
}


package com.medrdv.rendezvous.service;

import com.medrdv.common.dto.*;
import com.medrdv.common.enums.AppointmentStatus;
import com.medrdv.common.enums.SlotStatus;
import com.medrdv.common.events.AppointmentBookedEvent;
import com.medrdv.common.events.AppointmentCancelledEvent;
import com.medrdv.common.events.AppointmentRequestedEvent;
import com.medrdv.common.events.ConsultationCompletedEvent;
import com.medrdv.rendezvous.client.ResilientRequests;
import com.medrdv.rendezvous.entity.Appointment;
import com.medrdv.rendezvous.kafka.RendezVousKafkaProducer;
import com.medrdv.rendezvous.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ResilientRequests resilientRequests;
    private final RendezVousKafkaProducer producer;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Initialisation de la demande de RDV (Validation stricte avec Retry + CircuitBreaker)
     */
    @Transactional
    public Appointment requestAppointment(Appointment request) {
        log.info("Validation externe stricte du patient {}...", request.getPatientId());
        PatientDTO patient = resilientRequests.getPatientByIdStrict(request.getPatientId());
        if (patient == null) {
            throw new IllegalArgumentException("Patient non trouvé avec l'id: " + request.getPatientId());
        }

        log.info("Validation externe stricte du médecin {}...", request.getDoctorId());
        DoctorDTO doctor = resilientRequests.getDoctorByIdStrict(request.getDoctorId());
        if (doctor == null) {
            throw new IllegalArgumentException("Médecin non trouvé avec l'id: " + request.getDoctorId());
        }

        log.info("Validation externe stricte du créneau {}...", request.getAvailabilityId());
        AvailabilityDTO slot = resilientRequests.getAvailabilityByIdStrict(request.getAvailabilityId());
        if (slot == null) {
            throw new IllegalArgumentException("Créneau non trouvé avec l'id: " + request.getAvailabilityId());
        }
        if (!slot.getDoctorId().equals(request.getDoctorId())) {
            throw new IllegalArgumentException("Le créneau n'appartient pas au praticien sélectionné");
        }
        if (slot.getStatut() != SlotStatus.DISPONIBLE) {
            throw new IllegalStateException("Le créneau n'est plus disponible pour la réservation");
        }

        request.setStatut(AppointmentStatus.DEMANDE);
        Appointment savedAppointment = appointmentRepository.save(request);

        // Déclenchement de la Saga distribuée via Kafka
        AppointmentRequestedEvent event = AppointmentRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .appointmentId(savedAppointment.getId())
                .patientId(savedAppointment.getPatientId())
                .doctorId(savedAppointment.getDoctorId())
                .availabilityId(savedAppointment.getAvailabilityId())
                .motif(savedAppointment.getMotif())
                .timestamp(LocalDateTime.now())
                .build();

        producer.sendAppointmentRequested(event);
        log.info("Demande de rendez-vous enregistrée (id: {}). En attente de réservation créneau via Kafka.", savedAppointment.getId());

        return savedAppointment;
    }

    @Transactional
    public void confirmAppointment(Long appointmentId) {
        appointmentRepository.findById(appointmentId).ifPresent(app -> {
            app.setStatut(AppointmentStatus.CONFIRME);
            appointmentRepository.save(app);
            log.info("Rendez-vous {} CONFIRME avec succès.", appointmentId);

            AppointmentBookedEvent bookedEvent = AppointmentBookedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .appointmentId(app.getId())
                    .patientId(app.getPatientId())
                    .doctorId(app.getDoctorId())
                    .availabilityId(app.getAvailabilityId())
                    .motif(app.getMotif())
                    .timestamp(LocalDateTime.now())
                    .build();

            producer.sendAppointmentBooked(bookedEvent);
        });
    }

    @Transactional
    public void rejectAppointment(Long appointmentId, String reason) {
        appointmentRepository.findById(appointmentId).ifPresent(app -> {
            app.setStatut(AppointmentStatus.REJETE);
            appointmentRepository.save(app);
            log.warn("Rendez-vous {} REJETE: {}", appointmentId, reason);

            AppointmentCancelledEvent cancelledEvent = AppointmentCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .appointmentId(app.getId())
                    .patientId(app.getPatientId())
                    .doctorId(app.getDoctorId())
                    .availabilityId(app.getAvailabilityId())
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();

            producer.sendAppointmentCancelled(cancelledEvent);
        });
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String reason) {
        Appointment app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Rendez-vous non trouvé: " + appointmentId));

        app.setStatut(AppointmentStatus.ANNULE);
        Appointment saved = appointmentRepository.save(app);

        AppointmentCancelledEvent cancelledEvent = AppointmentCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .appointmentId(app.getId())
                .patientId(app.getPatientId())
                .doctorId(app.getDoctorId())
                .availabilityId(app.getAvailabilityId())
                .reason(reason != null ? reason : "Annulation demandée par l'utilisateur")
                .timestamp(LocalDateTime.now())
                .build();

        producer.sendAppointmentCancelled(cancelledEvent);
        return saved;
    }

    @Transactional
    public Appointment completeConsultation(Long appointmentId, String notes) {
        Appointment app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Rendez-vous non trouvé: " + appointmentId));

        app.setStatut(AppointmentStatus.REALISE);
        app.setNotesConsultation(notes);
        Appointment saved = appointmentRepository.save(app);

        ConsultationCompletedEvent completedEvent = ConsultationCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .appointmentId(app.getId())
                .patientId(app.getPatientId())
                .doctorId(app.getDoctorId())
                .consultationDate(LocalDateTime.now())
                .motif(app.getMotif())
                .notesConsultation(notes)
                .timestamp(LocalDateTime.now())
                .build();

        producer.sendConsultationCompleted(completedEvent);
        return saved;
    }

    /**
     * Consultation enrichie en mode dégradé (Cache Caffeine + Fallbacks sécurisés)
     */
    public EnrichedAppointmentDTO getEnrichedAppointment(Long id) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rendez-vous introuvable: " + id));

        // Appels avec repli dégradé (cache ou null)
        PatientDTO patient = resilientRequests.getPatientByIdFb(app.getPatientId());
        MedicalRecordDTO medicalRecord = resilientRequests.getMedicalRecordByPatientIdFb(app.getPatientId());
        DoctorDTO doctor = resilientRequests.getDoctorByIdFb(app.getDoctorId());

        return EnrichedAppointmentDTO.builder()
                .id(app.getId())
                .motif(app.getMotif())
                .statut(app.getStatut())
                .dateCreation(app.getDateCreation())
                .patient(patient)
                .dossierMedical(medicalRecord)
                .praticien(doctor)
                .build();
    }
}

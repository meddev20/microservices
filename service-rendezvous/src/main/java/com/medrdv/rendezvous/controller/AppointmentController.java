package com.medrdv.rendezvous.controller;

import com.medrdv.common.dto.EnrichedAppointmentDTO;
import com.medrdv.rendezvous.entity.Appointment;
import com.medrdv.rendezvous.service.AppointmentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint de consultation enrichie avec RateLimiter et mode dégradé (Jalon V6)
     */
    @GetMapping("/{id}/enriched")
    @RateLimiter(name = "consultation", fallbackMethod = "consultationLimitee")
    public ResponseEntity<EnrichedAppointmentDTO> getEnrichedAppointment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(appointmentService.getEnrichedAppointment(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Méthode de repli du RateLimiter (HTTP 429)
     */
    public ResponseEntity<EnrichedAppointmentDTO> consultationLimitee(Long id, Throwable t) {
        log.warn("RateLimiter déclenché : limitation de consultation pour RDV #{}", id);
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de consultations simultanées. Veuillez réessayer dans quelques instants.");
    }

    /**
     * Demande d'un nouveau rendez-vous (démarre la Saga distribuée)
     */
    @PostMapping
    public ResponseEntity<?> requestAppointment(@Valid @RequestBody Appointment request) {
        try {
            Appointment created = appointmentService.requestAppointment(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Clôture de la consultation (statut REALISE)
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeConsultation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : "";
        try {
            Appointment completed = appointmentService.completeConsultation(id, notes);
            return ResponseEntity.ok(completed);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Annulation du rendez-vous (statut ANNULE)
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Annulé par le demandeur";
        try {
            Appointment cancelled = appointmentService.cancelAppointment(id, reason);
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}

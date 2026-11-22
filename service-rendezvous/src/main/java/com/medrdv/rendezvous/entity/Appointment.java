package com.medrdv.rendezvous.entity;

import com.medrdv.common.enums.AppointmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'identifiant du patient est obligatoire")
    @Column(nullable = false)
    private Long patientId;

    @NotNull(message = "L'identifiant du médecin est obligatoire")
    @Column(nullable = false)
    private Long doctorId;

    @NotNull(message = "L'identifiant du créneau est obligatoire")
    @Column(nullable = false)
    private Long availabilityId;

    @NotBlank(message = "Le motif du rendez-vous est obligatoire")
    @Column(nullable = false)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus statut = AppointmentStatus.DEMANDE;

    private String notesConsultation;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    private LocalDateTime dateDerniereModification;

    @PreUpdate
    public void onUpdate() {
        this.dateDerniereModification = LocalDateTime.now();
    }
}

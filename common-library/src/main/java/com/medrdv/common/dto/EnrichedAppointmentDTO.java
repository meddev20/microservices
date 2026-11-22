package com.medrdv.common.dto;

import com.medrdv.common.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedAppointmentDTO {
    private Long id;
    private String motif;
    private AppointmentStatus statut;
    private LocalDateTime dateCreation;

    // Enriched Patient Data & Medical Record
    private PatientDTO patient;
    private MedicalRecordDTO dossierMedical;

    // Enriched Doctor & Slot Data
    private DoctorDTO praticien;
    private AvailabilityDTO creneau;
}

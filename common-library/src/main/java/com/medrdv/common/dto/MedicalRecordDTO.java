package com.medrdv.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordDTO {
    private Long id;
    private Long patientId;
    private String allergies;
    private String antecedents;
    private LocalDateTime derniereConsultation;
    private String dernierMotif;
}


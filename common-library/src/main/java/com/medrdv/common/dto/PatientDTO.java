package com.medrdv.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
    private Long id;
    private String nom;
    private String email;
    private LocalDate dateNaissance;
    private String telephone;
    private String numeroSecu;
}


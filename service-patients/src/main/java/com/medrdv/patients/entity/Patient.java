package com.medrdv.patients.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du patient est obligatoire")
    @Column(nullable = false)
    private String nom;

    @Email(message = "Format d'email invalide")
    private String email;

    private LocalDate dateNaissance;

    private String telephone;

    @Column(unique = true)
    private String numeroSecu;
}


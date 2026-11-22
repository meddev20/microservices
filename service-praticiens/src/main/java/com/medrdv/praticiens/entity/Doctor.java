package com.medrdv.praticiens.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "doctors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du médecin est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "La spécialité est obligatoire")
    @Column(nullable = false)
    private String specialite;

    private String telephone;

    private String email;
}

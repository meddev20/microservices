package com.medrdv.praticiens.entity;

import com.medrdv.common.enums.SlotStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "availabilities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'identifiant du médecin est obligatoire")
    @Column(nullable = false)
    private Long doctorId;

    @NotNull(message = "La date et l'heure sont obligatoires")
    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Builder.Default
    private Integer dureeMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SlotStatus statut = SlotStatus.DISPONIBLE;

    @Version
    private Long version;
}

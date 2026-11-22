package com.medrdv.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationCompletedEvent implements Serializable {
    private String eventId;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private LocalDateTime consultationDate;
    private String motif;
    private String notesConsultation;
    private LocalDateTime timestamp;
}


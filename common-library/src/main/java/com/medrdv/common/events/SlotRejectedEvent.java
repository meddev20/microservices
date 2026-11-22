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
public class SlotRejectedEvent implements Serializable {
    private String eventId;
    private Long appointmentId;
    private Long availabilityId;
    private Long doctorId;
    private String reason;
    private LocalDateTime timestamp;
}


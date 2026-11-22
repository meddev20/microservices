package com.medrdv.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecord {
    private String id;
    private String recipient;
    private String subject;
    private String content;
    private String type; // CONFIRMATION, ANNULATION, RAPPEL
    private LocalDateTime timestamp;
    private boolean sentSuccessfully;
}


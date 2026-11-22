package com.medrdv.notification.controller;

import com.medrdv.notification.model.NotificationRecord;
import com.medrdv.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<List<NotificationRecord>> getNotificationHistory() {
        return ResponseEntity.ok(emailService.getNotificationHistory());
    }

    @PostMapping
    public ResponseEntity<Void> triggerNotification(@RequestBody NotificationRecord record) {
        emailService.sendEmail(record.getRecipient(), record.getSubject(), record.getContent(), record.getType());
        return ResponseEntity.ok().build();
    }
}


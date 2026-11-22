package com.medrdv.notification.service;

import com.medrdv.notification.model.NotificationRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final List<NotificationRecord> history = Collections.synchronizedList(new ArrayList<>());

    public void sendEmail(String to, String subject, String body, String type) {
        boolean success = false;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@medrdv.fr");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            success = true;
            log.info("Email envoyé avec succès à {} : {}", to, subject);
        } catch (Exception ex) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", to, ex.getMessage());
        }

        NotificationRecord record = NotificationRecord.builder()
                .id(UUID.randomUUID().toString())
                .recipient(to)
                .subject(subject)
                .content(body)
                .type(type)
                .timestamp(LocalDateTime.now())
                .sentSuccessfully(success)
                .build();

        history.add(record);
    }

    public List<NotificationRecord> getNotificationHistory() {
        return new ArrayList<>(history);
    }
}


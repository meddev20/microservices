package com.medrdv.notification.kafka;

import com.medrdv.notification.config.NotificationRabbitConfig;
import com.medrdv.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRabbitConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = NotificationRabbitConfig.QUEUE_EMAIL)
    public void handleRabbitNotification(Map<String, Object> message) {
        log.info("[RABBITMQ] Notification reçue: {}", message);

        String email = message.get("email") != null ? message.get("email").toString() : "patient@medrdv.fr";
        String subject = message.get("subject") != null ? message.get("subject").toString() : "Notification Med-RDV";
        String body = message.get("body") != null ? message.get("body").toString() : "";
        String type = message.get("type") != null ? message.get("type").toString() : "RABBITMQ_ALERT";

        emailService.sendEmail(email, subject, body, type);
    }
}

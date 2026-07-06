package com.cybernode.ai.distributed_codeforge.account_service.consumer;

import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.account_service.repository.UserRepository;
import com.cybernode.ai.distributed_codeforge.common_lib.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @KafkaListener(topics = "notification-events", groupId = "codeforge-group")
    public void consumeNotificationEvent(NotificationEvent event) {
        log.info("Received notification event from Kafka - Type: {}, UserId: {}, Message: {}",
                event.type(), event.userId(), event.message());

        if (event.userId() != null) {
            userRepository.findById(event.userId()).ifPresentOrElse(user -> {
                String email = user.getUsername(); // username stores user email
                sendNotificationEmail(email, event.type(), event.message());
            }, () -> log.warn("User not found for notification event: {}", event.userId()));
        } else {
            log.warn("Notification event has no userId: {}", event);
        }
    }

    private void sendNotificationEmail(String toEmail, String subjectType, String messageContent) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("CodeForge Notification: " + subjectType);
            mailMessage.setText("Hello,\n\nWe have a new update regarding your account:\n\n" +
                    messageContent + "\n\nBest regards,\nCodeForge Team");
            mailSender.send(mailMessage);
            log.info("Notification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send notification email to: {}", toEmail, e);
        }
    }
}

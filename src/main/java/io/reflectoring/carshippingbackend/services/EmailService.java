package io.reflectoring.carshippingbackend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a verification email asynchronously to avoid blocking the main thread.
     */
    @Async
    public void sendVerificationEmail(String to, String code) {
        try {
            String subject = "Verify your email address - f-carshipping.com";
            String content = """
                    Dear user,

                    Thank you for signing up with f-carshipping.com.
                    Your verification code is: %s

                    Please enter this code to verify your account.

                    Regards,
                    The f-carshipping Team
                    """.formatted(code);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("info@f-carshipping.com");
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("✅ Verification email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("❌ Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Generic method to send any simple email (like password reset)
     */
    @Async
    public void sendSimpleMessage(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("info@f-carshipping.com");
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

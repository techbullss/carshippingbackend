package io.reflectoring.carshippingbackend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String code) {
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
    }
}


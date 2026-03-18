package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.ItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Add this

    // Your existing methods remain the same
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
            log.info("Verification email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendApprovalEmail(String to, String firstName) {
        try {
            String subject = "Your Account Has Been Approved - f-carshipping.com";

            String content = """
                Dear %s,

                Congratulations! 

                Your account has been successfully approved.
                You can now log in and start using all features of f-carshipping.com.

                Login here:
                https://f-carshipping.com/Login

                If you have any questions, feel free to contact us.

                Regards,
                The f-carshipping Team
                """.formatted(firstName);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("info@f-carshipping.com");
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Approval email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", to, e.getMessage());
        }
    }

    // NEW: Send review request email with HTML template
    @Async
    public void sendReviewRequestEmail(ItemRequest order) {
        try {
            // Generate secure token
            String token = generateSecureToken(order);

            // URL encode parameters for safe transmission
            String encodedItemName = URLEncoder.encode(order.getItemName(), StandardCharsets.UTF_8);
            String encodedClientName = URLEncoder.encode(order.getClientName(), StandardCharsets.UTF_8);
            String encodedEmail = URLEncoder.encode(order.getClientEmail(), StandardCharsets.UTF_8);

            // Create review link with all parameters
            String reviewUrl = String.format(
                    "https://f-carshipping.com/reviews?orderId=%d&token=%s&item=%s&client=%s&email=%s",
                    order.getId(),
                    token,
                    encodedItemName,
                    encodedClientName,
                    encodedEmail
            );

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("itemName", order.getItemName());
            variables.put("requestId", order.getRequestId());
            variables.put("orderId", order.getId());
            variables.put("orderDate", order.getCreatedAt().toLocalDate().toString());
            variables.put("reviewUrl", reviewUrl);

            String subject = String.format("How was your experience with %s? - f-carshipping.com",
                    order.getItemName());

            // Send HTML email
            sendHtmlEmail(order.getClientEmail(), subject, "review-request", variables);

            log.info("Review request email sent to {} for order {}",
                    order.getClientEmail(), order.getRequestId());

        } catch (Exception e) {
            log.error("Failed to send review request email: {}", e.getMessage());
        }
    }

    // NEW: Send order cancelled by client notification
    @Async
    public void sendOrderCancelledByClientEmail(ItemRequest order) {
        try {
            // Email to client
            Map<String, Object> clientVariables = new HashMap<>();
            clientVariables.put("clientName", order.getClientName());
            clientVariables.put("requestId", order.getRequestId());
            clientVariables.put("itemName", order.getItemName());
            clientVariables.put("cancelledDate", java.time.LocalDate.now().toString());

            String clientSubject = String.format("Order Cancelled - Request ID: %s", order.getRequestId());
            sendHtmlEmail(order.getClientEmail(), clientSubject, "order-cancelled-client", clientVariables);

            // Email to admin
            Map<String, Object> adminVariables = new HashMap<>();
            adminVariables.put("clientName", order.getClientName());
            adminVariables.put("clientEmail", order.getClientEmail());
            adminVariables.put("requestId", order.getRequestId());
            adminVariables.put("itemName", order.getItemName());
            adminVariables.put("cancelledDate", java.time.LocalDate.now().toString());
            adminVariables.put("adminUrl", "https://f-carshipping.com/admin/orders/" + order.getId());

            String adminSubject = String.format("[ADMIN] Order Cancelled by Client - %s", order.getRequestId());
            sendHtmlEmail("admin@f-carshipping.com", adminSubject, "order-cancelled-admin", adminVariables);

        } catch (Exception e) {
            log.error("Failed to send cancellation emails: {}", e.getMessage());
        }
    }

    // NEW: Send order cancelled by admin notification
    @Async
    public void sendOrderCancelledByAdminEmail(ItemRequest order) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("cancelledDate", java.time.LocalDate.now().toString());
            variables.put("cancellationReason",
                    order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified");

            String subject = String.format("Your Order Has Been Cancelled - Request ID: %s", order.getRequestId());
            sendHtmlEmail(order.getClientEmail(), subject, "order-cancelled-admin", variables);

        } catch (Exception e) {
            log.error("Failed to send admin cancellation email: {}", e.getMessage());
        }
    }

    // NEW: Send order edited by client notification
    @Async
    public void sendOrderEditedByClientEmail(ItemRequest order, Map<String, String> changes) {
        try {
            // Email to client
            Map<String, Object> clientVariables = new HashMap<>();
            clientVariables.put("clientName", order.getClientName());
            clientVariables.put("requestId", order.getRequestId());
            clientVariables.put("itemName", order.getItemName());
            clientVariables.put("updatedDate", java.time.LocalDate.now().toString());
            clientVariables.put("orderUrl", "https://f-carshipping.com/dashboard/orders/" + order.getId());

            String clientSubject = String.format("Order Updated - Request ID: %s", order.getRequestId());
            sendHtmlEmail(order.getClientEmail(), clientSubject, "order-edited-client", clientVariables);

            // Email to admin
            Map<String, Object> adminVariables = new HashMap<>();
            adminVariables.put("clientName", order.getClientName());
            adminVariables.put("clientEmail", order.getClientEmail());
            adminVariables.put("requestId", order.getRequestId());
            adminVariables.put("itemName", order.getItemName());
            adminVariables.put("updatedDate", java.time.LocalDate.now().toString());
            adminVariables.put("adminUrl", "https://f-carshipping.com/admin/orders/" + order.getId());
            adminVariables.put("changes", changes);

            String adminSubject = String.format("[ADMIN] Order Updated by Client - %s", order.getRequestId());
            sendHtmlEmail("admin@f-carshipping.com", adminSubject, "order-edited-admin", adminVariables);

        } catch (Exception e) {
            log.error("Failed to send order edit emails: {}", e.getMessage());
        }
    }

    // NEW: Send order edited by admin notification
    @Async
    public void sendOrderEditedByAdminEmail(ItemRequest order) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("newStatus", order.getStatus());
            variables.put("updatedDate", java.time.LocalDate.now().toString());
            variables.put("orderUrl", "https://f-carshipping.com/dashboard/orders/" + order.getId());

            String subject = String.format("Your Order Has Been Updated - Request ID: %s", order.getRequestId());
            sendHtmlEmail(order.getClientEmail(), subject, "order-edited-by-admin", variables);

        } catch (Exception e) {
            log.error("Failed to send admin edit email: {}", e.getMessage());
        }
    }

    // NEW: Send thank you email after review
    @Async
    public void sendReviewThankYouEmail(ItemRequest order, int rating) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("itemName", order.getItemName());
            variables.put("rating", rating);
            variables.put("dashboardUrl", "https://f-carshipping.com/dashboard/orders/" + order.getId());

            String subject = "Thank You for Your Review! - f-carshipping.com";
            sendHtmlEmail(order.getClientEmail(), subject, "review-thank-you", variables);

        } catch (Exception e) {
            log.error("Failed to send thank you email: {}", e.getMessage());
        }
    }

    // Helper method to send HTML emails
    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom("reviews@f-carshipping.com");
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("HTML email sent successfully to {} using template {}", to, templateName);

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());

            // Fallback to plain text if HTML fails
            try {
                String fallbackContent = "Please visit https://f-carshipping.com to view this message.";
                SimpleMailMessage fallback = new SimpleMailMessage();
                fallback.setTo(to);
                fallback.setFrom("info@f-carshipping.com");
                fallback.setSubject(subject);
                fallback.setText(fallbackContent);
                mailSender.send(fallback);
            } catch (Exception ex) {
                log.error("Even fallback email failed: {}", ex.getMessage());
            }
        }
    }

    // Your existing sendSimpleMessage method
    @Async
    public void sendSimpleMessage(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("info@f-carshipping.com");
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // Generate secure token for review links
    private String generateSecureToken(ItemRequest order) {
        String data = order.getId() + order.getClientEmail() + order.getRequestId() + "your-secret-salt-2024";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            log.warn("Failed to generate secure token, using UUID: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }
}
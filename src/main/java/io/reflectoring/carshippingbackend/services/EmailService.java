package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.ItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.domain:https://f-carshipping.com}")
    private String appDomain;

    @Value("${app.company.name:F-Car Shipping}")
    private String companyName;

    // Single email address for all communications
    private static final String FROM_EMAIL = "info@f-carshipping.com";
    private static final String FROM_NAME = "F-Car Shipping";

    // Admin email for notifications
    @Value("${app.admin.email:nduatifrancis43@gmail.com}")
    private String adminEmail;

    // Rate limiting to prevent spam flags (max 5 emails per hour per recipient)
    private final Map<String, AtomicInteger> rateLimiter = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> rateLimiterReset = new ConcurrentHashMap<>();
    private static final int MAX_EMAILS_PER_HOUR = 5;

    // Track failed emails for monitoring
    private final Map<String, Integer> failedEmailCount = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES_BEFORE_ALERT = 10;

    // ============= EXISTING METHODS =============

    @Async
    public void sendVerificationEmail(String to, String code) {
        if (!checkRateLimit(to)) {
            log.warn("Rate limit exceeded for verification email to {}", to);
            return;
        }

        try {
            String subject = "Verify your email address - " + companyName;
            String content = String.format("""
                    Dear user,

                    Thank you for signing up with %s.
                    Your verification code is: %s

                    Please enter this code to verify your account.

                    If you didn't request this, please ignore this email.

                    Regards,
                    The %s Team
                    
                    ---
                    %s
                    """, companyName, code, companyName, appDomain);

            sendPlainTextEmail(to, subject, content);
            log.info("Verification email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            trackFailure(to);
        }
    }

    @Async
    public void sendApprovalEmail(String to, String firstName) {
        if (!checkRateLimit(to)) {
            log.warn("Rate limit exceeded for approval email to {}", to);
            return;
        }

        try {
            String subject = "Your Account Has Been Approved - " + companyName;
            String content = String.format("""
                Dear %s,

                Congratulations! 

                Your account has been successfully approved.
                You can now log in and start using all features of %s.

                Login here:
                %s/Login

                If you have any questions, feel free to contact us at %s.

                Regards,
                The %s Team
                
                ---
                %s
                """, firstName, companyName, appDomain, FROM_EMAIL, companyName, appDomain);

            sendPlainTextEmail(to, subject, content);
            log.info("Approval email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", to, e.getMessage());
            trackFailure(to);
        }
    }

    @Async
    public void sendSimpleMessage(String to, String subject, String content) {
        if (!checkRateLimit(to)) {
            log.warn("Rate limit exceeded for simple message to {}", to);
            return;
        }

        sendPlainTextEmail(to, subject, content);
    }

    // ============= ORDER EMAILS =============

    @Async
    public void sendOrderConfirmationEmail(ItemRequest order) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for order confirmation to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("orderDate", formatDate(order.getCreatedAt()));
            variables.put("appDomain", appDomain);
            variables.put("companyName", companyName);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            // Add review link
            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);

            String subject = String.format("Order Confirmed - %s", order.getRequestId());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "order-confirmation", variables);
            if (!sent) {
                sendOrderConfirmationPlainText(order);
            }

            log.info("Order confirmation sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send order confirmation: {}", e.getMessage());
            sendOrderConfirmationPlainText(order);
            trackFailure(order.getClientEmail());
        }
    }

    @Async
    public void sendStatusUpdateEmail(ItemRequest order) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for status update to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("newStatus", formatStatus(order.getStatus()));
            variables.put("updatedDate", formatDate(order.getUpdatedAt()));
            variables.put("appDomain", appDomain);
            variables.put("companyName", companyName);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            // Add review link only for delivered orders
            if ("DELIVERED".equals(order.getStatus())) {
                String reviewLink = generateCleanReviewLink(order);
                variables.put("reviewLink", reviewLink);
                variables.put("showReviewLink", true);
            }

            String subject = String.format("Order Update - %s", order.getRequestId());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "order-status-update", variables);
            if (!sent) {
                sendStatusUpdatePlainText(order);
            }

            log.info("Status update sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send status update: {}", e.getMessage());
            sendStatusUpdatePlainText(order);
            trackFailure(order.getClientEmail());
        }
    }

    @Async
    public void sendReviewRequestEmail(ItemRequest order) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for review request to {}", order.getClientEmail());
            return;
        }

        try {
            String token = generateShortToken(order);
            String reviewUrl = String.format("%s/reviews/%s", appDomain, token);

            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("itemName", order.getItemName());
            variables.put("requestId", order.getRequestId());
            variables.put("orderDate", formatDate(order.getCreatedAt()));
            variables.put("reviewUrl", reviewUrl);
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("supportEmail", FROM_EMAIL);

            String subject = String.format("Share your experience with %s", order.getItemName());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "review-request", variables);
            if (!sent) {
                sendReviewRequestPlainText(order, reviewUrl);
            }

            log.info("Review request sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send review request: {}", e.getMessage());
            String token = generateShortToken(order);
            String reviewUrl = String.format("%s/reviews/%s", appDomain, token);
            sendReviewRequestPlainText(order, reviewUrl);
            trackFailure(order.getClientEmail());
        }
    }

    @Async
    public void sendReviewThankYouEmail(ItemRequest order, int rating) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for thank you email to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("itemName", order.getItemName());
            variables.put("rating", rating);
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("dashboardUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            String subject = "Thank You for Your Review!";

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "review-thankyou", variables);
            if (!sent) {
                sendReviewThankYouPlainText(order, rating);
            }

            log.info("Thank you email sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send thank you: {}", e.getMessage());
            sendReviewThankYouPlainText(order, rating);
            trackFailure(order.getClientEmail());
        }
    }

    // ============= CANCELLATION EMAILS =============

    @Async
    public void sendOrderCancelledByClientEmail(ItemRequest order) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for cancellation email to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("cancellationDate", formatDate(order.getUpdatedAt()));
            variables.put("cancellationReason", order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified");
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            String subject = String.format("Order Cancelled - %s", order.getRequestId());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "order-cancelled-client", variables);

            if (!sent) {
                sendOrderCancelledByClientPlainText(order);
            }

            // Notify admin
            String adminContent = String.format("""
                Client %s (%s) has cancelled order %s.
                
                Order Details:
                - Item: %s
                - Cancellation reason: %s
                - Date: %s
                
                View in admin panel: %s/admin/requests
                """,
                    order.getClientName(),
                    order.getClientEmail(),
                    order.getRequestId(),
                    order.getItemName(),
                    order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                    formatDate(order.getUpdatedAt()),
                    appDomain
            );

            sendPlainTextEmail(adminEmail,
                    String.format("[ADMIN] Order Cancelled - %s", order.getRequestId()),
                    adminContent);

            log.info("Cancellation emails sent for order {}", order.getRequestId());

        } catch (Exception e) {
            log.error("Failed to send cancellation emails: {}", e.getMessage(), e);
            sendOrderCancelledByClientPlainText(order);
            trackFailure(order.getClientEmail());
        }
    }

    @Async
    public void sendOrderCancelledByAdminEmail(ItemRequest order) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for admin cancellation to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("cancellationDate", formatDate(order.getUpdatedAt()));
            variables.put("cancellationReason", order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified");
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            String subject = String.format("Order Cancelled - %s", order.getRequestId());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "order-cancelled-admin", variables);

            if (!sent) {
                sendOrderCancelledByAdminPlainText(order);
            }

            log.info("Admin cancellation email sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send admin cancellation email: {}", e.getMessage(), e);
            sendOrderCancelledByAdminPlainText(order);
            trackFailure(order.getClientEmail());
        }
    }

    // ============= EDIT EMAILS =============

    @Async
    public void sendOrderEditedByClientEmail(ItemRequest order, Map<String, String> changes) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for edit email to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("updatedDate", formatDate(order.getUpdatedAt()));
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            // Format changes for display
            if (changes != null && !changes.isEmpty()) {
                StringBuilder changesHtml = new StringBuilder("<ul style='margin: 15px 0; padding-left: 20px;'>");
                changes.forEach((field, change) ->
                        changesHtml.append("<li><strong>").append(field).append(":</strong> ").append(change).append("</li>"));
                changesHtml.append("</ul>");
                variables.put("changesHtml", changesHtml.toString());

                StringBuilder changesText = new StringBuilder();
                changes.forEach((field, change) ->
                        changesText.append("- ").append(field).append(": ").append(change).append("\n"));
                variables.put("changesText", changesText.toString());
            } else {
                variables.put("changesHtml", "<p>No significant changes recorded.</p>");
                variables.put("changesText", "No significant changes recorded.");
            }

            String subject = String.format("Order Updated - %s", order.getRequestId());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "order-edited-client", variables);

            if (!sent) {
                sendOrderEditedByClientPlainText(order, changes);
            }

            // Notify admin
            StringBuilder adminChanges = new StringBuilder();
            if (changes != null && !changes.isEmpty()) {
                changes.forEach((field, change) ->
                        adminChanges.append("- ").append(field).append(": ").append(change).append("\n"));
            }

            String adminContent = String.format("""
                Client %s (%s) has updated order %s.
                
                Updated Order:
                - Item: %s
                - Updated: %s
                %s
                
                View in admin panel: %s/admin/requests/%d
                """,
                    order.getClientName(),
                    order.getClientEmail(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatDate(order.getUpdatedAt()),
                    adminChanges.toString(),
                    appDomain,
                    order.getId()
            );

            sendPlainTextEmail(adminEmail,
                    String.format("[ADMIN] Order Updated - %s", order.getRequestId()),
                    adminContent);

            log.info("Order edit emails sent for order {}", order.getRequestId());

        } catch (Exception e) {
            log.error("Failed to send order edit emails: {}", e.getMessage(), e);
            sendOrderEditedByClientPlainText(order, changes);
            trackFailure(order.getClientEmail());
        }
    }

    @Async
    public void sendOrderEditedByAdminEmail(ItemRequest order) {
        if (!checkRateLimit(order.getClientEmail())) {
            log.warn("Rate limit exceeded for admin edit email to {}", order.getClientEmail());
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("newStatus", formatStatus(order.getStatus()));
            variables.put("updatedDate", formatDate(order.getUpdatedAt()));
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));

            String subject = String.format("Order Updated - %s", order.getRequestId());

            boolean sent = sendSpamSafeHtmlEmail(order.getClientEmail(), subject, "order-edited-admin", variables);

            if (!sent) {
                sendOrderEditedByAdminPlainText(order);
            }

            log.info("Admin edit email sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send admin edit email: {}", e.getMessage(), e);
            sendOrderEditedByAdminPlainText(order);
            trackFailure(order.getClientEmail());
        }
    }

    // ============= CORE EMAIL SENDING METHODS =============

    private boolean sendSpamSafeHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(FROM_EMAIL, FROM_NAME);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Add essential headers for deliverability
            message.setHeader("X-Mailer", FROM_NAME);
            message.setHeader("X-Entity-Ref-ID", UUID.randomUUID().toString());
            message.setHeader("Message-ID", String.format("<%s@%s>", UUID.randomUUID(), getDomain()));

            mailSender.send(message);
            log.info("HTML email sent to {} using template {}", to, templateName);
            return true;

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private void sendPlainTextEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setFrom(FROM_EMAIL, FROM_NAME);
            helper.setSubject(subject);
            helper.setText(content);

            // Add essential headers
            message.setHeader("X-Mailer", FROM_NAME);
            message.setHeader("X-Entity-Ref-ID", UUID.randomUUID().toString());

            mailSender.send(message);
            log.info("Plain text email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send plain text email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }

    // ============= FALLBACK PLAIN TEXT METHODS =============

    private void sendOrderConfirmationPlainText(ItemRequest order) {
        try {
            String content = String.format("""
                Dear %s,
                
                Your order has been confirmed!
                
                Order #: %s
                Item: %s
                Date: %s
                
                We'll notify you when your order status changes.
                
                Thank you for choosing %s!
                
                View your order: %s/dashboard/UserOrdersPage/
                
                Questions? Contact us: %s
                
                ---
                %s
                """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatDate(order.getCreatedAt()),
                    companyName,
                    appDomain,
                    FROM_EMAIL,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Order Confirmed - %s", order.getRequestId()),
                    content);

        } catch (Exception e) {
            log.error("Order confirmation plain text failed: {}", e.getMessage());
        }
    }

    private void sendStatusUpdatePlainText(ItemRequest order) {
        try {
            String reviewSection = "";
            if ("DELIVERED".equals(order.getStatus())) {
                String reviewLink = generateCleanReviewLink(order);
                reviewSection = String.format("\n\nShare your experience: %s", reviewLink);
            }

            String content = String.format("""
                Dear %s,
                
                Your order status has been updated.
                
                Order #: %s
                Item: %s
                New Status: %s
                Date: %s
                
                View your order: %s/dashboard/UserOrdersPage/
                %s
                
                Best regards,
                %s Team
                
                ---
                %s
                """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatStatus(order.getStatus()),
                    formatDate(order.getUpdatedAt()),
                    appDomain,
                    reviewSection,
                    companyName,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Order Update - %s", order.getRequestId()),
                    content);

        } catch (Exception e) {
            log.error("Status update plain text failed: {}", e.getMessage());
        }
    }

    private void sendReviewRequestPlainText(ItemRequest order, String reviewUrl) {
        try {
            String content = String.format("""
                Dear %s,
                
                Thank you for ordering %s (Order #%s).
                
                We'd love to hear about your experience!
                
                Please share your feedback here:
                %s
                
                Your feedback helps us improve and helps other customers make informed decisions.
                
                Thank you,
                %s Team
                
                ---
                Questions? Contact us: %s
                If you didn't request this review, please ignore this email.
                """,
                    order.getClientName(),
                    order.getItemName(),
                    order.getRequestId(),
                    reviewUrl,
                    companyName,
                    FROM_EMAIL
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Share your experience with %s", order.getItemName()),
                    content);

        } catch (Exception e) {
            log.error("Review request plain text failed: {}", e.getMessage());
        }
    }

    private void sendReviewThankYouPlainText(ItemRequest order, int rating) {
        try {
            String content = String.format("""
                Dear %s,
                
                Thank you for your %d-star review of %s!
                
                Your feedback helps us serve you better and helps other customers make informed decisions.
                
                Best regards,
                %s Team
                
                ---
                Need help? Contact us: %s
                Visit us: %s
                """,
                    order.getClientName(),
                    rating,
                    order.getItemName(),
                    companyName,
                    FROM_EMAIL,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(), "Thank You for Your Review!", content);

        } catch (Exception e) {
            log.error("Review thank you plain text failed: {}", e.getMessage());
        }
    }

    private void sendOrderCancelledByClientPlainText(ItemRequest order) {
        try {
            String content = String.format("""
                Dear %s,
                
                Your order %s has been cancelled.
                
                Order Details:
                - Order #: %s
                - Item: %s
                - Cancellation date: %s
                - Reason: %s
                
                If you have any questions, please contact us at %s.
                
                Best regards,
                %s Team
                
                ---
                %s
                """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatDate(order.getUpdatedAt()),
                    order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                    FROM_EMAIL,
                    companyName,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Order Cancelled - %s", order.getRequestId()),
                    content);
        } catch (Exception e) {
            log.error("Failed to send cancellation plain text: {}", e.getMessage());
        }
    }

    private void sendOrderCancelledByAdminPlainText(ItemRequest order) {
        try {
            String content = String.format("""
                Dear %s,
                
                Your order %s has been cancelled by our team.
                
                Order Details:
                - Order #: %s
                - Item: %s
                - Cancellation date: %s
                - Reason: %s
                
                If you have any questions or concerns, please contact us at %s.
                
                Best regards,
                %s Team
                
                ---
                %s
                """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatDate(order.getUpdatedAt()),
                    order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                    FROM_EMAIL,
                    companyName,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Order Cancelled - %s", order.getRequestId()),
                    content);
        } catch (Exception e) {
            log.error("Failed to send admin cancellation plain text: {}", e.getMessage());
        }
    }

    private void sendOrderEditedByClientPlainText(ItemRequest order, Map<String, String> changes) {
        try {
            StringBuilder changesText = new StringBuilder();
            if (changes != null && !changes.isEmpty()) {
                changesText.append("\nChanges made:\n");
                changes.forEach((field, change) ->
                        changesText.append("- ").append(field).append(": ").append(change).append("\n"));
            }

            String content = String.format("""
                Dear %s,
                
                Your order %s has been updated successfully.
                
                Updated Order:
                - Order #: %s
                - Item: %s
                - Updated: %s%s
                
                View your order: %s/dashboard/UserOrdersPage/
                
                If you need to make further changes, please contact us at %s.
                
                Best regards,
                %s Team
                
                ---
                %s
                """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatDate(order.getUpdatedAt()),
                    changesText.toString(),
                    appDomain,
                    FROM_EMAIL,
                    companyName,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Order Updated - %s", order.getRequestId()),
                    content);
        } catch (Exception e) {
            log.error("Failed to send edit plain text: {}", e.getMessage());
        }
    }

    private void sendOrderEditedByAdminPlainText(ItemRequest order) {
        try {
            String content = String.format("""
                Dear %s,
                
                Your order %s has been updated by our team.
                
                Updated Order:
                - Order #: %s
                - Item: %s
                - New Status: %s
                - Updated: %s
                
                View your order: %s/dashboard/UserOrdersPage/
                
                If you have any questions, please contact us at %s.
                
                Best regards,
                %s Team
                
                ---
                %s
                """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatStatus(order.getStatus()),
                    formatDate(order.getUpdatedAt()),
                    appDomain,
                    FROM_EMAIL,
                    companyName,
                    appDomain
            );

            sendPlainTextEmail(order.getClientEmail(),
                    String.format("Order Updated - %s", order.getRequestId()),
                    content);
        } catch (Exception e) {
            log.error("Failed to send admin edit plain text: {}", e.getMessage());
        }
    }

    // ============= HELPER METHODS =============

    private boolean checkRateLimit(String email) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetTime = rateLimiterReset.get(email);

        if (resetTime == null || now.isAfter(resetTime)) {
            // Reset counter
            rateLimiter.put(email, new AtomicInteger(1));
            rateLimiterReset.put(email, now.plusHours(1));
            return true;
        }

        AtomicInteger counter = rateLimiter.get(email);
        if (counter != null && counter.incrementAndGet() > MAX_EMAILS_PER_HOUR) {
            log.warn("Rate limit exceeded for {}", email);
            return false;
        }

        return true;
    }

    private void trackFailure(String email) {
        int failures = failedEmailCount.getOrDefault(email, 0) + 1;
        failedEmailCount.put(email, failures);

        if (failures >= MAX_FAILURES_BEFORE_ALERT) {
            log.error("High failure rate for email {}: {} failures", email, failures);
            failedEmailCount.remove(email); // Reset after alert
        }
    }

    private String generateCleanReviewLink(ItemRequest order) {
        String token = generateShortToken(order);
        return String.format("%s/reviews/%s", appDomain, token);
    }

    private String generateShortToken(ItemRequest order) {
        String data = order.getId() + order.getClientEmail() + order.getRequestId() + System.currentTimeMillis();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            String fullHash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return fullHash.substring(0, 12).toLowerCase();
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 12);
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "Processing";
        return switch (status) {
            case "PENDING" -> "Pending Review";
            case "SOURCING" -> "Sourcing";
            case "IN_TRANSIT" -> "In Transit";
            case "DELIVERED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    private String getDomain() {
        return appDomain.replace("https://", "").replace("http://", "");
    }
}
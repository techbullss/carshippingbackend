package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.ItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private static final String FROM_EMAIL = "info@f-carshipping.com";
    private static final String FROM_NAME = "F-Car Shipping";

    @Value("${app.admin.email:nduatifrancis43@gmail.com}")
    private String adminEmail;

    // ============= AUTHENTICATION EMAILS =============

    public void sendVerificationEmail(String to, String code) {
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
        }
    }

    public void sendApprovalEmail(String to, String firstName) {
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
        }
    }

    public void sendSimpleMessage(String to, String subject, String content) {
        sendPlainTextEmail(to, subject, content);
    }

    // ============= ORDER EMAILS =============

    public void sendOrderConfirmationEmail(ItemRequest order) {
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

            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);
            variables.put("showReviewLink", true);

            String subject = String.format("Order Confirmed - %s", order.getRequestId());

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "order-confirmation", variables);
            if (!sent) {
                sendOrderConfirmationPlainText(order, reviewLink);
            }

            log.info("Order confirmation sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send order confirmation: {}", e.getMessage());
            String reviewLink = generateCleanReviewLink(order);
            sendOrderConfirmationPlainText(order, reviewLink);
        }
    }

    public void sendStatusUpdateEmail(ItemRequest order) {
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

            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);
            variables.put("showReviewLink", true);

            String subject = String.format("Order Update - %s", order.getRequestId());

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "order-status-update", variables);
            if (!sent) {
                sendStatusUpdatePlainText(order, reviewLink);
            }

            log.info("Status update sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send status update: {}", e.getMessage());
            String reviewLink = generateCleanReviewLink(order);
            sendStatusUpdatePlainText(order, reviewLink);
        }
    }

    public void sendReviewRequestEmail(ItemRequest order) {
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

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "review-request", variables);
            if (!sent) {
                sendReviewRequestPlainText(order, reviewUrl);
            }

            log.info("Review request sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send review request: {}", e.getMessage());
            String token = generateShortToken(order);
            String reviewUrl = String.format("%s/reviews/%s", appDomain, token);
            sendReviewRequestPlainText(order, reviewUrl);
        }
    }

    public void sendReviewThankYouEmail(ItemRequest order, int rating) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("itemName", order.getItemName());
            variables.put("rating", rating);
            variables.put("companyName", companyName);
            variables.put("appDomain", appDomain);
            variables.put("dashboardUrl", String.format("%s/dashboard/UserOrdersPage/", appDomain));
            variables.put("supportEmail", FROM_EMAIL);

            String subject = "Thank You for Your Review!";

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "review-thankyou", variables);
            if (!sent) {
                sendReviewThankYouPlainText(order, rating);
            }

            log.info("Thank you email sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send thank you: {}", e.getMessage());
            sendReviewThankYouPlainText(order, rating);
        }
    }

    public void sendOrderCancelledByClientEmail(ItemRequest order) {
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

            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);
            variables.put("showReviewLink", true);

            String subject = String.format("Order Cancelled - %s", order.getRequestId());

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "order-cancelled-client", variables);
            if (!sent) {
                sendOrderCancelledByClientPlainText(order, reviewLink);
            }

            // Notify admin
            String adminContent = String.format("""
                Client %s (%s) cancelled order %s.
                Item: %s | Reason: %s | Date: %s
                """,
                    order.getClientName(), order.getClientEmail(), order.getRequestId(),
                    order.getItemName(),
                    order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                    formatDate(order.getUpdatedAt())
            );
            sendPlainTextEmail(adminEmail, String.format("[ADMIN] Order Cancelled - %s", order.getRequestId()), adminContent);

            log.info("Cancellation emails sent for order {}", order.getRequestId());

        } catch (Exception e) {
            log.error("Failed to send cancellation emails: {}", e.getMessage());
            String reviewLink = generateCleanReviewLink(order);
            sendOrderCancelledByClientPlainText(order, reviewLink);
        }
    }

    public void sendOrderCancelledByAdminEmail(ItemRequest order) {
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

            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);
            variables.put("showReviewLink", true);

            String subject = String.format("Order Cancelled - %s", order.getRequestId());

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "order-cancelled-admin", variables);
            if (!sent) {
                sendOrderCancelledByAdminPlainText(order, reviewLink);
            }

            log.info("Admin cancellation email sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send admin cancellation email: {}", e.getMessage());
            String reviewLink = generateCleanReviewLink(order);
            sendOrderCancelledByAdminPlainText(order, reviewLink);
        }
    }

    public void sendOrderEditedByClientEmail(ItemRequest order, Map<String, String> changes) {
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

            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);
            variables.put("showReviewLink", true);

            if (changes != null && !changes.isEmpty()) {
                StringBuilder changesHtml = new StringBuilder("<ul>");
                changes.forEach((field, change) ->
                        changesHtml.append("<li><strong>").append(field).append(":</strong> ").append(change).append("</li>"));
                changesHtml.append("</ul>");
                variables.put("changesHtml", changesHtml.toString());
            } else {
                variables.put("changesHtml", "<p>No significant changes recorded.</p>");
            }

            String subject = String.format("Order Updated - %s", order.getRequestId());

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "order-edited-client", variables);
            if (!sent) {
                sendOrderEditedByClientPlainText(order, changes, reviewLink);
            }

            // Notify admin
            StringBuilder adminChanges = new StringBuilder();
            if (changes != null && !changes.isEmpty()) {
                changes.forEach((field, change) -> adminChanges.append("- ").append(field).append(": ").append(change).append("\n"));
            }

            String adminContent = String.format("""
                Client %s (%s) updated order %s.
                Item: %s | Updated: %s
                %s
                """,
                    order.getClientName(), order.getClientEmail(), order.getRequestId(),
                    order.getItemName(), formatDate(order.getUpdatedAt()), adminChanges.toString()
            );
            sendPlainTextEmail(adminEmail, String.format("[ADMIN] Order Updated - %s", order.getRequestId()), adminContent);

            log.info("Order edit emails sent for order {}", order.getRequestId());

        } catch (Exception e) {
            log.error("Failed to send order edit emails: {}", e.getMessage());
            String reviewLink = generateCleanReviewLink(order);
            sendOrderEditedByClientPlainText(order, changes, reviewLink);
        }
    }

    public void sendOrderEditedByAdminEmail(ItemRequest order) {
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

            String reviewLink = generateCleanReviewLink(order);
            variables.put("reviewLink", reviewLink);
            variables.put("showReviewLink", true);

            String subject = String.format("Order Updated - %s", order.getRequestId());

            boolean sent = sendHtmlEmail(order.getClientEmail(), subject, "order-edited-admin", variables);
            if (!sent) {
                sendOrderEditedByAdminPlainText(order, reviewLink);
            }

            log.info("Admin edit email sent to {}", order.getClientEmail());

        } catch (Exception e) {
            log.error("Failed to send admin edit email: {}", e.getMessage());
            String reviewLink = generateCleanReviewLink(order);
            sendOrderEditedByAdminPlainText(order, reviewLink);
        }
    }

    // ============= CORE EMAIL METHODS =============

    private boolean sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
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

            mailSender.send(message);
            log.info("Email sent to {} using template {}", to, templateName);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
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
            mailSender.send(message);
            log.info("Plain text email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send plain text email to {}: {}", to, e.getMessage());
        }
    }

    // ============= PLAIN TEXT FALLBACKS =============

    private void sendOrderConfirmationPlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Confirmed - %s", order.getRequestId()),
                String.format("""
                Dear %s,
                
                Order Confirmed! #%s - %s
                Date: %s
                
                Track your order: %s/dashboard/UserOrdersPage/
                Share your experience: %s
                
                Questions? %s
                """,
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatDate(order.getCreatedAt()), appDomain, reviewLink, FROM_EMAIL));
    }

    private void sendStatusUpdatePlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Update - %s", order.getRequestId()),
                String.format("""
                Dear %s,
                
                Order #%s - %s
                Status: %s
                Date: %s
                
                Track: %s/dashboard/UserOrdersPage/
                Feedback: %s
                """,
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatStatus(order.getStatus()), formatDate(order.getUpdatedAt()),
                        appDomain, reviewLink));
    }

    private void sendReviewRequestPlainText(ItemRequest order, String reviewUrl) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Share your experience with %s", order.getItemName()),
                String.format("""
                Dear %s,
                
                Thank you for ordering %s (#%s)
                
                Share your feedback: %s
                
                Your feedback helps us improve!
                """,
                        order.getClientName(), order.getItemName(), order.getRequestId(), reviewUrl));
    }

    private void sendReviewThankYouPlainText(ItemRequest order, int rating) {
        sendPlainTextEmail(order.getClientEmail(), "Thank You for Your Review!",
                String.format("Dear %s,\n\nThank you for your %d-star review of %s!\n\nBest regards,\n%s Team",
                        order.getClientName(), rating, order.getItemName(), companyName));
    }

    private void sendOrderCancelledByClientPlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Cancelled - %s", order.getRequestId()),
                String.format("""
                Dear %s,
                
                Order #%s - %s cancelled.
                Reason: %s
                Date: %s
                
                Feedback: %s
                """,
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                        formatDate(order.getUpdatedAt()), reviewLink));
    }

    private void sendOrderCancelledByAdminPlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Cancelled - %s", order.getRequestId()),
                String.format("""
                Dear %s,
                
                Order #%s - %s cancelled by admin.
                Reason: %s
                
                Questions? Contact us: %s
                Feedback: %s
                """,
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                        FROM_EMAIL, reviewLink));
    }

    private void sendOrderEditedByClientPlainText(ItemRequest order, Map<String, String> changes, String reviewLink) {
        StringBuilder changesText = new StringBuilder();
        if (changes != null && !changes.isEmpty()) {
            changes.forEach((field, change) -> changesText.append("\n- ").append(field).append(": ").append(change));
        }

        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Updated - %s", order.getRequestId()),
                String.format("""
                Dear %s,
                
                Order #%s - %s updated.
                Date: %s%s
                
                Track: %s/dashboard/UserOrdersPage/
                Feedback: %s
                """,
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatDate(order.getUpdatedAt()), changesText.toString(),
                        appDomain, reviewLink));
    }

    private void sendOrderEditedByAdminPlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Updated - %s", order.getRequestId()),
                String.format("""
                Dear %s,
                
                Order #%s - %s updated by admin.
                New Status: %s
                Date: %s
                
                Track: %s/dashboard/UserOrdersPage/
                Feedback: %s
                """,
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatStatus(order.getStatus()), formatDate(order.getUpdatedAt()),
                        appDomain, reviewLink));
    }

    // ============= HELPER METHODS =============

    private String generateCleanReviewLink(ItemRequest order) {
        return String.format("%s/reviews/%s", appDomain, generateShortToken(order));
    }

    private String generateShortToken(ItemRequest order) {
        String data = order.getId() + order.getClientEmail() + order.getRequestId() + System.currentTimeMillis();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12).toLowerCase();
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
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) : "N/A";
    }

    private String getDomain() {
        return appDomain.replace("https://", "").replace("http://", "");
    }
}
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

    public void sendOrderConfirmationEmail(ItemRequest order, String reviewToken) {
        try {
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);

            String htmlContent = buildHtmlEmail(
                    "#059669",
                    "Order Confirmed! 🎉",
                    order,
                    null,
                    null,
                    reviewLink,
                    true
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Order Confirmed - %s", order.getRequestId()),
                    htmlContent);

        } catch (Exception e) {
            log.error("Failed to send order confirmation: {}", e.getMessage());
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendOrderConfirmationPlainText(order, reviewLink);
        }
    }

    public void sendStatusUpdateEmail(ItemRequest order, String reviewToken) {
        try {
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);

            String htmlContent = buildHtmlEmail(
                    "#3b82f6",
                    "Order Status Updated",
                    order,
                    order.getStatus(),
                    null,
                    reviewLink,
                    true
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Order Update - %s", order.getRequestId()),
                    htmlContent);

        } catch (Exception e) {
            log.error("Failed to send status update: {}", e.getMessage());
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendStatusUpdatePlainText(order, reviewLink);
        }
    }

    public void sendReviewRequestEmail(ItemRequest order, String reviewToken) {
        try {
            String reviewUrl = String.format("%s/Reviews/%s", appDomain, reviewToken);

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;">
                <div style="max-width:600px;margin:0 auto;padding:20px;">
                    <div style="background-color:#059669;padding:30px;text-align:center;">
                        <h1 style="margin:0;color:white;">How was your experience? 🌟</h1>
                    </div>
                    <div style="padding:30px;border:1px solid #e5e7eb;border-top:none;">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Thank you for your recent order of <strong>%s</strong>.</p>
                        <div style="background-color:#f9fafb;padding:20px;margin:20px 0;">
                            <p><strong>Order #:</strong> %s</p>
                            <p><strong>Item:</strong> %s</p>
                            <p><strong>Order Date:</strong> %s</p>
                        </div>
                        <p>We'd love to hear about your experience!</p>
                        <div style="text-align:center;margin:30px 0;">
                            <a href="%s" style="display:inline-block;background-color:#059669;color:white;padding:15px 40px;text-decoration:none;border-radius:8px;">Share Your Experience →</a>
                        </div>
                        <div style="margin-top:30px;padding-top:20px;border-top:1px solid #e5e7eb;text-align:center;font-size:12px;color:#6b7280;">
                            <p>Questions? Contact us at <a href="mailto:%s" style="color:#6b7280;">%s</a></p>
                            <p><a href="%s" style="color:#6b7280;">f-carshipping.com</a></p>
                        </div>
                    </div>
                </div>
                </body>
                </html>
                """,
                    order.getClientName(),
                    order.getItemName(),
                    order.getRequestId(),
                    order.getItemName(),
                    formatDate(order.getCreatedAt()),
                    reviewUrl,
                    FROM_EMAIL, FROM_EMAIL,
                    appDomain
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Share your experience with %s", order.getItemName()),
                    htmlContent);

        } catch (Exception e) {
            log.error("Failed to send review request: {}", e.getMessage());
            String reviewUrl = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendReviewRequestPlainText(order, reviewUrl);
        }
    }

    public void sendReviewThankYouEmail(ItemRequest order, int rating) {
        try {
            String stars = "★".repeat(rating) + "☆".repeat(5 - rating);

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;">
                <div style="max-width:600px;margin:0 auto;padding:20px;">
                    <div style="background:linear-gradient(135deg,#10b981,#059669);padding:40px;text-align:center;">
                        <h1 style="margin:0;color:white;">Thank You! 🎉</h1>
                    </div>
                    <div style="padding:30px;text-align:center;">
                        <div style="font-size:48px;color:#fbbf24;margin:20px 0;">%s</div>
                        <div style="background-color:#f0fdf4;padding:20px;border-radius:8px;">
                            <p style="font-size:18px;">Dear <strong>%s</strong>,</p>
                            <p>Thank you for your %d-star review of <strong>%s</strong>!</p>
                        </div>
                        <p>Your feedback helps us improve our service!</p>
                        <a href="%s/dashboard/UserOrdersPage/" style="display:inline-block;background-color:#059669;color:white;padding:12px 30px;text-decoration:none;border-radius:5px;margin-top:20px;">View Your Orders</a>
                    </div>
                </div>
                </body>
                </html>
                """,
                    stars,
                    order.getClientName(),
                    rating,
                    order.getItemName(),
                    appDomain
            );

            sendHtmlEmail(order.getClientEmail(), "Thank You for Your Review!", htmlContent);

        } catch (Exception e) {
            log.error("Failed to send thank you: {}", e.getMessage());
            sendReviewThankYouPlainText(order, rating);
        }
    }

    public void sendOrderCancelledByClientEmail(ItemRequest order, String reviewToken) {
        try {
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);

            String htmlContent = buildHtmlEmail(
                    "#dc2626",
                    "Order Cancelled",
                    order,
                    null,
                    order.getCancellationReason(),
                    reviewLink,
                    true
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Order Cancelled - %s", order.getRequestId()),
                    htmlContent);

            // Notify admin
            sendPlainTextEmail(adminEmail,
                    String.format("[ADMIN] Order Cancelled - %s", order.getRequestId()),
                    String.format("Client %s (%s) cancelled order %s\nItem: %s\nReason: %s",
                            order.getClientName(), order.getClientEmail(), order.getRequestId(),
                            order.getItemName(),
                            order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified"));

        } catch (Exception e) {
            log.error("Failed to send cancellation emails: {}", e.getMessage());
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendOrderCancelledByClientPlainText(order, reviewLink);
        }
    }

    public void sendOrderCancelledByAdminEmail(ItemRequest order, String reviewToken) {
        try {
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);

            String htmlContent = buildHtmlEmail(
                    "#dc2626",
                    "Order Cancelled by Admin",
                    order,
                    null,
                    order.getCancellationReason(),
                    reviewLink,
                    true
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Order Cancelled - %s", order.getRequestId()),
                    htmlContent);

        } catch (Exception e) {
            log.error("Failed to send admin cancellation email: {}", e.getMessage());
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendOrderCancelledByAdminPlainText(order, reviewLink);
        }
    }

    public void sendOrderEditedByClientEmail(ItemRequest order, Map<String, String> changes, String reviewToken) {
        try {
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);

            StringBuilder changesHtml = new StringBuilder();
            if (changes != null && !changes.isEmpty()) {
                changesHtml.append("<div style='background-color:#fef3c7;padding:15px;border-radius:8px;margin:15px 0;'><strong>Changes made:</strong><ul>");
                changes.forEach((field, change) ->
                        changesHtml.append("<li><strong>").append(field).append(":</strong> ").append(change).append("</li>"));
                changesHtml.append("</ul></div>");
            }

            String htmlContent = buildHtmlEmail(
                    "#3b82f6",
                    "Order Updated",
                    order,
                    null,
                    null,
                    reviewLink,
                    true,
                    changesHtml.toString()
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Order Updated - %s", order.getRequestId()),
                    htmlContent);

            // Notify admin
            StringBuilder changesText = new StringBuilder();
            if (changes != null && !changes.isEmpty()) {
                changes.forEach((field, change) -> changesText.append("\n- ").append(field).append(": ").append(change));
            }
            sendPlainTextEmail(adminEmail,
                    String.format("[ADMIN] Order Updated - %s", order.getRequestId()),
                    String.format("Client %s (%s) updated order %s\nItem: %s\nUpdated: %s%s",
                            order.getClientName(), order.getClientEmail(), order.getRequestId(),
                            order.getItemName(), formatDate(order.getUpdatedAt()), changesText.toString()));

        } catch (Exception e) {
            log.error("Failed to send order edit emails: {}", e.getMessage());
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendOrderEditedByClientPlainText(order, changes, reviewLink);
        }
    }

    public void sendOrderEditedByAdminEmail(ItemRequest order, String reviewToken) {
        try {
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);

            String htmlContent = buildHtmlEmail(
                    "#3b82f6",
                    "Order Updated by Admin",
                    order,
                    order.getStatus(),
                    null,
                    reviewLink,
                    true
            );

            sendHtmlEmail(order.getClientEmail(),
                    String.format("Order Updated - %s", order.getRequestId()),
                    htmlContent);

        } catch (Exception e) {
            log.error("Failed to send admin edit email: {}", e.getMessage());
            String reviewLink = String.format("%s/Reviews/%s", appDomain, reviewToken);
            sendOrderEditedByAdminPlainText(order, reviewLink);
        }
    }

    // ============= CORE EMAIL METHODS =============

    private String buildHtmlEmail(String headerColor, String title, ItemRequest order,
                                  String status, String cancelReason, String reviewLink,
                                  boolean showReview, String... extraContent) {
        String statusHtml = "";
        if (status != null) {
            statusHtml = String.format("""
                <p><strong>New Status:</strong> <span style="display:inline-block;background-color:%s;color:white;padding:8px 16px;border-radius:20px;">%s</span></p>
                """, headerColor, formatStatus(status));
        }

        String cancelHtml = "";
        if (cancelReason != null && !cancelReason.isEmpty()) {
            cancelHtml = String.format("<p><strong>Reason:</strong> %s</p>", cancelReason);
        }

        String reviewHtml = "";
        if (showReview && reviewLink != null && !reviewLink.isEmpty()) {
            reviewHtml = String.format("""
                <div style="background-color:#f0fdf4;padding:20px;border-radius:8px;margin:20px 0;text-align:center;">
                    <p><strong>📝 Share your experience!</strong></p>
                    <a href="%s" style="display:inline-block;background-color:#059669;color:white;padding:12px 30px;text-decoration:none;border-radius:5px;">Leave a Review →</a>
                </div>
                """, reviewLink);
        }

        String extraHtml = extraContent.length > 0 ? extraContent[0] : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f9fafb;">
            <div style="max-width:600px;margin:0 auto;padding:20px;">
                <div style="background-color:%s;padding:30px;text-align:center;">
                    <h1 style="margin:0;color:white;">%s</h1>
                </div>
                <div style="padding:30px;background-color:#ffffff;border:1px solid #e5e7eb;border-top:none;">
                    <p>Dear <strong>%s</strong>,</p>
                    <div style="background-color:#f9fafb;padding:20px;border-radius:8px;margin:20px 0;">
                        <p><strong>Order #:</strong> %s</p>
                        <p><strong>Item:</strong> %s</p>
                        %s
                        %s
                        <p><strong>Date:</strong> %s</p>
                    </div>
                    %s
                    %s
                    <div style="text-align:center;margin-top:20px;">
                        <a href="%s/dashboard/UserOrdersPage/" style="display:inline-block;background-color:%s;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;">View Order Details</a>
                    </div>
                    <div style="margin-top:30px;padding-top:20px;border-top:1px solid #e5e7eb;text-align:center;font-size:12px;color:#6b7280;">
                        <p>Thank you for choosing %s!</p>
                        <p>Questions? <a href="mailto:%s" style="color:#6b7280;">%s</a></p>
                        <p><a href="%s" style="color:#6b7280;">f-carshipping.com</a></p>
                    </div>
                </div>
            </div>
            </body>
            </html>
            """,
                headerColor, title,
                order.getClientName(),
                order.getRequestId(),
                order.getItemName(),
                statusHtml,
                cancelHtml,
                formatDate(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()),
                reviewHtml,
                extraHtml,
                appDomain, headerColor,
                companyName,
                FROM_EMAIL, FROM_EMAIL,
                appDomain
        );
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(FROM_EMAIL, FROM_NAME);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent to {} - Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email sending failed", e);
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
                String.format("Dear %s,\n\nOrder Confirmed! #%s - %s\nDate: %s\n\nTrack: %s/dashboard/UserOrdersPage/\nFeedback: %s\n\nQuestions? %s",
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatDate(order.getCreatedAt()), appDomain, reviewLink, FROM_EMAIL));
    }

    private void sendStatusUpdatePlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Update - %s", order.getRequestId()),
                String.format("Dear %s,\n\nOrder #%s - %s\nStatus: %s\nDate: %s\n\nTrack: %s/dashboard/UserOrdersPage/\nFeedback: %s",
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatStatus(order.getStatus()), formatDate(order.getUpdatedAt()),
                        appDomain, reviewLink));
    }

    private void sendReviewRequestPlainText(ItemRequest order, String reviewUrl) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Share your experience with %s", order.getItemName()),
                String.format("Dear %s,\n\nThank you for ordering %s (#%s)\n\nShare your feedback: %s\n\nYour feedback helps us improve!",
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
                String.format("Dear %s,\n\nOrder #%s - %s cancelled.\nReason: %s\nDate: %s\n\nFeedback: %s",
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified",
                        formatDate(order.getUpdatedAt()), reviewLink));
    }

    private void sendOrderCancelledByAdminPlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Cancelled - %s", order.getRequestId()),
                String.format("Dear %s,\n\nOrder #%s - %s cancelled by admin.\nReason: %s\n\nQuestions? %s\nFeedback: %s",
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
                String.format("Dear %s,\n\nOrder #%s - %s updated.\nDate: %s%s\n\nTrack: %s/dashboard/UserOrdersPage/\nFeedback: %s",
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatDate(order.getUpdatedAt()), changesText.toString(),
                        appDomain, reviewLink));
    }

    private void sendOrderEditedByAdminPlainText(ItemRequest order, String reviewLink) {
        sendPlainTextEmail(order.getClientEmail(),
                String.format("Order Updated - %s", order.getRequestId()),
                String.format("Dear %s,\n\nOrder #%s - %s updated by admin.\nNew Status: %s\nDate: %s\n\nTrack: %s/dashboard/UserOrdersPage/\nFeedback: %s",
                        order.getClientName(), order.getRequestId(), order.getItemName(),
                        formatStatus(order.getStatus()), formatDate(order.getUpdatedAt()),
                        appDomain, reviewLink));
    }

    // ============= HELPER METHODS =============

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
}
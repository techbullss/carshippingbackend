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

    // ================= ORDER EMAILS =================

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
            variables.put("orderUrl", appDomain + "/dashboard/UserOrdersPage/");

            String subject = "Order Update - " + order.getRequestId();

            boolean sent = sendHtmlEmail(
                    order.getClientEmail(),
                    subject,
                    "order-status-update",
                    variables
            );

            if (!sent) {
                sendStatusUpdatePlainText(order);
            }

        } catch (Exception e) {
            log.error("Failed to send status update: {}", e.getMessage());
            sendStatusUpdatePlainText(order);
        }
    }

    public void sendOrderCancelledByAdminEmail(ItemRequest order) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("clientName", order.getClientName());
            variables.put("requestId", order.getRequestId());
            variables.put("itemName", order.getItemName());
            variables.put("cancellationDate", formatDate(order.getUpdatedAt()));
            variables.put("cancellationReason",
                    order.getCancellationReason() != null ? order.getCancellationReason() : "Not specified");
            variables.put("appDomain", appDomain);
            variables.put("supportEmail", FROM_EMAIL);
            variables.put("orderUrl", appDomain + "/dashboard/UserOrdersPage/");

            String subject = "Order Cancelled - " + order.getRequestId();

            boolean sent = sendHtmlEmail(
                    order.getClientEmail(),
                    subject,
                    "order-cancelled-admin",
                    variables
            );

            if (!sent) {
                sendOrderCancelledByAdminPlainText(order);
            }

        } catch (Exception e) {
            log.error("Failed to send admin cancellation email: {}", e.getMessage());
            sendOrderCancelledByAdminPlainText(order);
        }
    }

    // ================= CORE EMAIL =================

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
            helper.setText(htmlContent, true); // IMPORTANT (HTML)

            mailSender.send(message);

            log.info("HTML email sent to {}", to);
            return true;

        } catch (Exception e) {
            log.error("HTML email failed: {}", e.getMessage());
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

        } catch (Exception e) {
            log.error("Plain email failed: {}", e.getMessage());
        }
    }

    // ================= FALLBACK =================

    private void sendStatusUpdatePlainText(ItemRequest order) {
        String content = String.format("""
                Dear %s,

                Your order status has been updated.

                Order #: %s
                Item: %s
                Status: %s

                View: %s/dashboard/UserOrdersPage/

                %s Team
                """,
                order.getClientName(),
                order.getRequestId(),
                order.getItemName(),
                formatStatus(order.getStatus()),
                appDomain,
                companyName
        );

        sendPlainTextEmail(order.getClientEmail(),
                "Order Update - " + order.getRequestId(),
                content);
    }

    private void sendOrderCancelledByAdminPlainText(ItemRequest order) {
        String content = String.format("""
                Dear %s,

                Your order has been cancelled.

                Order #: %s
                Item: %s
                Reason: %s

                %s Team
                """,
                order.getClientName(),
                order.getRequestId(),
                order.getItemName(),
                order.getCancellationReason(),
                companyName
        );

        sendPlainTextEmail(order.getClientEmail(),
                "Order Cancelled - " + order.getRequestId(),
                content);
    }

    // ================= HELPERS =================

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
}
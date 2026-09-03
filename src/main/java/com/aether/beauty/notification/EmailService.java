package com.aether.beauty.notification;

import com.aether.beauty.order.CustomerOrder;
import com.aether.beauty.order.OrderLine;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends order confirmation emails once payment succeeds.
 *
 * Spring Boot only creates a JavaMailSender bean when spring.mail.host is
 * set, so this depends on it through an ObjectProvider rather than a
 * direct constructor argument — the same "gracefully do nothing until
 * real credentials exist" approach already used for Razorpay and
 * Cloudinary elsewhere in this codebase. Until SMTP env vars are
 * configured, this quietly no-ops and logs instead of failing the order.
 */
@Service
public class EmailService {
  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final String fromAddress;
  private final String supportEmail;
  private final String supportPhone;

  public EmailService(
    ObjectProvider<JavaMailSender> mailSenderProvider,
    @Value("${aether.mail.from:}") String fromAddress,
    @Value("${aether.mail.support-email:help@amaraeformulations.com}") String supportEmail,
    @Value("${aether.mail.support-phone:+91 95792 22532}") String supportPhone
  ) {
    this.mailSenderProvider = mailSenderProvider;
    this.fromAddress = fromAddress;
    this.supportEmail = supportEmail;
    this.supportPhone = supportPhone;
  }

  public void sendOrderConfirmation(CustomerOrder order) {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null || fromAddress == null || fromAddress.isBlank()) {
      log.info("Order confirmation email skipped for order {} — SMTP is not configured yet.", order.getId());
      return;
    }
    if (order.getEmail() == null || order.getEmail().isBlank()) {
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(order.getEmail());
      helper.setSubject("Your AMARA\u00c8 order #" + order.getId() + " is confirmed");
      helper.setText(buildBody(order), false);
      mailSender.send(message);
    } catch (Exception ex) {
      // A failed send should never undo or block a successful payment —
      // the order itself already went through.
      log.warn("Could not send order confirmation email for order {}: {}", order.getId(), ex.getMessage());
    }
  }

  private String buildBody(CustomerOrder order) {
    StringBuilder sb = new StringBuilder();
    sb.append("Hi ").append(order.getCustomerName()).append(",\n\n");
    sb.append("Thanks for your order \u2014 here's your confirmation.\n\n");
    sb.append("Order #").append(order.getId()).append("\n");
    sb.append("--------------------------------\n");
    for (OrderLine line : order.getLines()) {
      sb
        .append(line.getQuantity())
        .append(" x ")
        .append(line.getProductName())
        .append(" - ")
        .append(formatMoney(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))))
        .append("\n");
    }
    sb.append("--------------------------------\n");
    sb.append("Subtotal: ").append(formatMoney(order.getSubtotal())).append("\n");
    if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
      sb
        .append("Discount")
        .append(order.getCouponCode() != null ? " (" + order.getCouponCode() + ")" : "")
        .append(": -")
        .append(formatMoney(order.getDiscountAmount()))
        .append("\n");
    }
    sb.append("Shipping: ").append(formatMoney(order.getShippingFee())).append("\n");
    sb.append("Total paid: ").append(formatMoney(order.getTotal())).append("\n\n");
    sb.append("Shipping to:\n");
    sb.append(order.getShippingAddressLine()).append("\n");
    sb
      .append(order.getShippingCity())
      .append(", ")
      .append(order.getShippingState())
      .append(" ")
      .append(order.getShippingPinCode())
      .append("\n");
    sb.append(order.getShippingCountry()).append("\n\n");
    sb.append("We'll pack and ship your order soon.\n\n");
    sb
      .append("Delayed, missing, or something wrong with an item? Message us on WhatsApp at ")
      .append(supportPhone)
      .append(" or email ")
      .append(supportEmail)
      .append(" with this order number.\n\n");
    sb.append("With scent,\nAMARA\u00c8 Formulations\n");
    return sb.toString();
  }

  private String formatMoney(BigDecimal amount) {
    return "\u20b9" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}

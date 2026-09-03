package com.aether.beauty.payment;

import com.aether.beauty.order.CustomerOrder;
import com.aether.beauty.order.CustomerOrderRepository;
import com.aether.beauty.order.OrderStatus;
import com.aether.beauty.notification.EmailService;
import com.aether.beauty.realtime.RealtimeEventService;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
  private final PaymentProperties paymentProperties;
  private final DemoPaymentGateway demoPaymentGateway;
  private final RazorpayPaymentGateway razorpayPaymentGateway;
  private final PaymentTransactionRepository paymentTransactionRepository;
  private final CustomerOrderRepository customerOrderRepository;
  private final RealtimeEventService realtimeEventService;
  private final EmailService emailService;

  public PaymentService(
    PaymentProperties paymentProperties,
    DemoPaymentGateway demoPaymentGateway,
    RazorpayPaymentGateway razorpayPaymentGateway,
    PaymentTransactionRepository paymentTransactionRepository,
    CustomerOrderRepository customerOrderRepository,
    RealtimeEventService realtimeEventService,
    EmailService emailService
  ) {
    this.paymentProperties = paymentProperties;
    this.demoPaymentGateway = demoPaymentGateway;
    this.razorpayPaymentGateway = razorpayPaymentGateway;
    this.paymentTransactionRepository = paymentTransactionRepository;
    this.customerOrderRepository = customerOrderRepository;
    this.realtimeEventService = realtimeEventService;
    this.emailService = emailService;
  }

  @Transactional
  public PaymentSession createPayment(CustomerOrder order) {
    PaymentSession session = "razorpay".equalsIgnoreCase(paymentProperties.getGateway())
      ? razorpayPaymentGateway.createPayment(order)
      : demoPaymentGateway.createPayment(order);

    PaymentTransaction transaction = new PaymentTransaction();
    transaction.setOrder(order);
    transaction.setProvider(session.provider());
    transaction.setProviderReference(session.providerReference());
    transaction.setAmount(order.getTotal());
    transaction.setCurrency(paymentProperties.getCurrency());
    paymentTransactionRepository.save(transaction);

    order.setStatus(OrderStatus.PAYMENT_PENDING);
    order.setPaymentProvider(session.provider());
    order.setPaymentReference(session.providerReference());
    order.setPaymentUrl(session.paymentUrl());
    customerOrderRepository.save(order);
    realtimeEventService.publish("orders", order.getId());
    return session;
  }

  @Transactional
  public CustomerOrder markPayment(String providerReference, PaymentStatus status) {
    PaymentTransaction transaction = paymentTransactionRepository
      .findByProviderReference(providerReference)
      .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
    transaction.setStatus(status);

    CustomerOrder order = transaction.getOrder();
    order.setStatus(status == PaymentStatus.PAID ? OrderStatus.PAID : OrderStatus.FAILED);
    customerOrderRepository.save(order);
    realtimeEventService.publish("orders", order.getId());
    if (status == PaymentStatus.PAID) {
      emailService.sendOrderConfirmation(order);
    }
    return order;
  }

  @Transactional
  public CustomerOrder verifyRazorpay(
    String razorpayOrderId,
    String razorpayPaymentId,
    String razorpaySignature
  ) {
    String secret = paymentProperties.getRazorpay().getKeySecret();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("Razorpay secret is not configured");
    }

    String payload = razorpayOrderId + "|" + razorpayPaymentId;
    String expectedSignature = hmacSha256(payload, secret);
    if (!MessageDigest.isEqual(
      expectedSignature.getBytes(StandardCharsets.UTF_8),
      razorpaySignature.getBytes(StandardCharsets.UTF_8)
    )) {
      throw new IllegalStateException("Razorpay signature verification failed");
    }

    return markPayment(razorpayOrderId, PaymentStatus.PAID);
  }

  private String hmacSha256(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte value : hash) {
        hex.append(String.format("%02x", value));
      }
      return hex.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to verify payment signature", ex);
    }
  }
}

package com.aether.beauty.payment;

import com.aether.beauty.order.CustomerOrder;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DemoPaymentGateway implements PaymentGateway {
  @Override
  public PaymentSession createPayment(CustomerOrder order) {
    String reference = "demo_" + UUID.randomUUID();
    return new PaymentSession(
      "demo",
      reference,
      "/api/payments/demo-success?reference=" + reference
    );
  }
}

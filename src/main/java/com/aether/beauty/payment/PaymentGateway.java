package com.aether.beauty.payment;

import com.aether.beauty.order.CustomerOrder;

public interface PaymentGateway {
  PaymentSession createPayment(CustomerOrder order);
}

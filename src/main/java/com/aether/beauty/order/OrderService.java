package com.aether.beauty.order;

import com.aether.beauty.api.dto.CheckoutRequest;
import com.aether.beauty.cart.CartItem;
import com.aether.beauty.cart.CartService;
import com.aether.beauty.payment.PaymentService;
import com.aether.beauty.product.Product;
import com.aether.beauty.realtime.RealtimeEventService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private final CustomerOrderRepository customerOrderRepository;
  private final CartService cartService;
  private final PaymentService paymentService;
  private final RealtimeEventService realtimeEventService;

  public OrderService(
    CustomerOrderRepository customerOrderRepository,
    CartService cartService,
    PaymentService paymentService,
    RealtimeEventService realtimeEventService
  ) {
    this.customerOrderRepository = customerOrderRepository;
    this.cartService = cartService;
    this.paymentService = paymentService;
    this.realtimeEventService = realtimeEventService;
  }

  public List<CustomerOrder> latestOrders() {
    return customerOrderRepository.findTop25ByOrderByCreatedAtDesc();
  }

  @Transactional
  public CustomerOrder checkout(CheckoutRequest request) {
    List<CartItem> cartItems = cartService.getCart(request.sessionId());
    if (cartItems.isEmpty()) {
      throw new IllegalStateException("Cart is empty");
    }

    CustomerOrder order = new CustomerOrder();
    order.setSessionId(request.sessionId());
    order.setCustomerName(request.customerName());
    order.setEmail(request.email());
    order.setDeliveryCity(request.deliveryCity());

    BigDecimal total = BigDecimal.ZERO;
    for (CartItem cartItem : cartItems) {
      Product product = cartItem.getProduct();
      if (product.getStock() < cartItem.getQuantity()) {
        throw new IllegalStateException(product.getName() + " is out of stock");
      }

      OrderLine line = new OrderLine();
      line.setOrder(order);
      line.setProduct(product);
      line.setProductName(product.getName());
      line.setUnitPrice(product.getPrice());
      line.setQuantity(cartItem.getQuantity());
      order.getLines().add(line);

      total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
      product.setStock(product.getStock() - cartItem.getQuantity());
    }
    order.setTotal(total);

    CustomerOrder saved = customerOrderRepository.save(order);
    paymentService.createPayment(saved);
    cartService.clearCart(request.sessionId());
    realtimeEventService.publish("orders", saved.getId());
    return saved;
  }
}

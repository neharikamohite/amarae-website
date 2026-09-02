package com.aether.beauty.order;

import com.aether.beauty.api.dto.CheckoutRequest;
import com.aether.beauty.cart.CartItem;
import com.aether.beauty.cart.CartService;
import com.aether.beauty.payment.PaymentService;
import com.aether.beauty.product.Product;
import com.aether.beauty.product.ProductService;
import com.aether.beauty.realtime.RealtimeEventService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private static final int GIFT_ELIGIBLE_SIZE_ML = 100;

  private final CustomerOrderRepository customerOrderRepository;
  private final CartService cartService;
  private final PaymentService paymentService;
  private final RealtimeEventService realtimeEventService;
  private final ProductService productService;

  public OrderService(
    CustomerOrderRepository customerOrderRepository,
    CartService cartService,
    PaymentService paymentService,
    RealtimeEventService realtimeEventService,
    ProductService productService
  ) {
    this.customerOrderRepository = customerOrderRepository;
    this.cartService = cartService;
    this.paymentService = paymentService;
    this.realtimeEventService = realtimeEventService;
    this.productService = productService;
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
    boolean giftEligible = false;
    for (CartItem cartItem : cartItems) {
      Product product = cartItem.getProduct();
      if (product.getStock() < cartItem.getQuantity()) {
        throw new IllegalStateException(product.getName() + " is out of stock");
      }
      if (product.getSizeMl() == GIFT_ELIGIBLE_SIZE_ML) {
        giftEligible = true;
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

    // Launch offer: buy one 100 ml fragrance, get a complimentary different
    // 100 ml fragrance free. Enforced here (not just in the UI) so the free
    // bottle is only ever granted when the cart actually earns it, and is
    // always recorded as a real order line with unit price 0.
    if (giftEligible) {
      if (request.complimentaryProductId() == null) {
        throw new IllegalStateException("Choose your complimentary different 100 ml fragrance before proceeding to payment.");
      }
      Product giftProduct = productService.requireProduct(request.complimentaryProductId());
      if (giftProduct.getSizeMl() != GIFT_ELIGIBLE_SIZE_ML) {
        throw new IllegalStateException("The complimentary launch gift must be a 100 ml fragrance.");
      }
      boolean alreadyInCart = cartItems
        .stream()
        .anyMatch(item -> item.getProduct().getId().equals(giftProduct.getId()));
      if (alreadyInCart) {
        throw new IllegalStateException("Choose a complimentary fragrance different from the ones already in your cart.");
      }
      if (giftProduct.getStock() < 1) {
        throw new IllegalStateException(giftProduct.getName() + " is out of stock for the complimentary gift right now.");
      }

      OrderLine giftLine = new OrderLine();
      giftLine.setOrder(order);
      giftLine.setProduct(giftProduct);
      giftLine.setProductName(giftProduct.getName() + " (complimentary launch gift)");
      giftLine.setUnitPrice(BigDecimal.ZERO);
      giftLine.setQuantity(1);
      order.getLines().add(giftLine);
      giftProduct.setStock(giftProduct.getStock() - 1);
    } else if (request.complimentaryProductId() != null) {
      throw new IllegalStateException("Add a 100 ml fragrance to your cart to unlock the complimentary gift.");
    }

    order.setTotal(total);

    CustomerOrder saved = customerOrderRepository.save(order);
    paymentService.createPayment(saved);
    cartService.clearCart(request.sessionId());
    realtimeEventService.publish("orders", saved.getId());
    return saved;
  }
}

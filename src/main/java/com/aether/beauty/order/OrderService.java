package com.aether.beauty.order;

import com.aether.beauty.api.dto.CheckoutRequest;
import com.aether.beauty.auth.User;
import com.aether.beauty.cart.CartItem;
import com.aether.beauty.cart.CartService;
import com.aether.beauty.coupon.CouponResult;
import com.aether.beauty.coupon.CouponService;
import com.aether.beauty.payment.PaymentService;
import com.aether.beauty.product.Product;
import com.aether.beauty.product.ProductService;
import com.aether.beauty.realtime.RealtimeEventService;
import com.aether.beauty.shipping.ShippingService;
import jakarta.persistence.EntityNotFoundException;
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
  private final ShippingService shippingService;
  private final CouponService couponService;

  public OrderService(
    CustomerOrderRepository customerOrderRepository,
    CartService cartService,
    PaymentService paymentService,
    RealtimeEventService realtimeEventService,
    ProductService productService,
    ShippingService shippingService,
    CouponService couponService
  ) {
    this.customerOrderRepository = customerOrderRepository;
    this.cartService = cartService;
    this.paymentService = paymentService;
    this.realtimeEventService = realtimeEventService;
    this.productService = productService;
    this.shippingService = shippingService;
    this.couponService = couponService;
  }

  // Used only by the admin dashboard (authenticated) — every order, not
  // just the most recent, since an owner reviewing their store needs the
  // full list.
  public List<CustomerOrder> allOrdersMostRecentFirst() {
    return customerOrderRepository.findAllByOrderByCreatedAtDesc();
  }

  @Transactional
  public CustomerOrder updateFulfillment(
    Long orderId,
    OrderStatus status,
    String trackingCourier,
    String trackingNumber,
    String trackingUrl
  ) {
    CustomerOrder order = customerOrderRepository
      .findById(orderId)
      .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    if (status != null) {
      order.setStatus(status);
    }
    // Blank strings clear a field (e.g. correcting a typo'd AWB); null
    // means "leave this field as it was" so the admin can update just one
    // field at a time without resending the others.
    if (trackingCourier != null) {
      order.setTrackingCourier(trackingCourier.isBlank() ? null : trackingCourier.trim());
    }
    if (trackingNumber != null) {
      order.setTrackingNumber(trackingNumber.isBlank() ? null : trackingNumber.trim());
    }
    if (trackingUrl != null) {
      order.setTrackingUrl(trackingUrl.isBlank() ? null : trackingUrl.trim());
    }
    CustomerOrder saved = customerOrderRepository.save(order);
    realtimeEventService.publish("orders", saved.getId());
    return saved;
  }

  @Transactional
  public CustomerOrder checkout(CheckoutRequest request, User user) {
    List<CartItem> cartItems = cartService.getCart(request.sessionId());
    if (cartItems.isEmpty()) {
      throw new IllegalStateException("Cart is empty");
    }

    // We only ship inside India right now — reject before payment rather
    // than accepting an order we can't fulfil.
    shippingService.requireValidIndianAddress(request.shippingPinCode(), request.phone());

    CustomerOrder order = new CustomerOrder();
    order.setUser(user);
    order.setSessionId(request.sessionId());
    order.setCustomerName(request.customerName());
    order.setEmail(request.email());
    order.setShippingAddressLine(request.shippingAddressLine());
    order.setShippingCity(request.shippingCity());
    order.setShippingState(request.shippingState());
    order.setShippingPinCode(request.shippingPinCode());
    order.setPhone(request.phone());

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

    // Shipping is charged on the paid subtotal only — the complimentary
    // gift line above is unit price 0 so it never inflates delivery cost.
    BigDecimal shippingFee = shippingService.computeShippingFee(total);

    // Coupon discount, if any, is validated authoritatively here — never
    // trusted from whatever the cart preview showed the customer. It's
    // applied to the product subtotal only, computed on the pre-discount
    // subtotal so a coupon can't be used to sneak past the free-shipping
    // threshold.
    BigDecimal discountAmount = BigDecimal.ZERO;
    String appliedCouponCode = null;
    if (request.couponCode() != null && !request.couponCode().isBlank()) {
      CouponResult couponResult = couponService.validate(request.couponCode(), total);
      discountAmount = couponResult.discount();
      appliedCouponCode = couponResult.code();
    }

    order.setSubtotal(total);
    order.setCouponCode(appliedCouponCode);
    order.setDiscountAmount(discountAmount);
    order.setShippingFee(shippingFee);
    order.setTotal(total.subtract(discountAmount).add(shippingFee));

    CustomerOrder saved = customerOrderRepository.save(order);
    paymentService.createPayment(saved);
    cartService.clearCart(request.sessionId());
    realtimeEventService.publish("orders", saved.getId());
    return saved;
  }
}

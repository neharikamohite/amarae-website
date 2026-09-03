package com.aether.beauty.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.aether.beauty.auth.User;

@Entity
public class CustomerOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Null for guest checkouts — only set when the shopper was signed in at
  // checkout, so order history has something to query against.
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  private User user;

  @NotBlank
  private String sessionId;

  @NotBlank
  private String customerName;

  @Email
  @NotBlank
  private String email;

  @NotBlank
  private String shippingAddressLine;

  @NotBlank
  private String shippingCity;

  @NotBlank
  private String shippingState;

  @NotBlank
  private String shippingPinCode;

  @NotBlank
  private String phone;

  // Fixed for now: AMARAÈ currently ships only within India.
  private String shippingCountry = "India";

  @Enumerated(EnumType.STRING)
  private OrderStatus status = OrderStatus.CREATED;

  private BigDecimal subtotal = BigDecimal.ZERO;

  private String couponCode;

  private BigDecimal discountAmount = BigDecimal.ZERO;

  private BigDecimal shippingFee = BigDecimal.ZERO;

  private BigDecimal total = BigDecimal.ZERO;

  private String paymentProvider;

  private String paymentReference;

  private String paymentUrl;

  // Set by the admin dashboard once an order ships — this is the whole
  // point of Level 2 tracking: a place to type in the courier/AWB so the
  // customer's order history can show it, instead of it living only in a
  // WhatsApp message.
  private String trackingCourier;

  private String trackingNumber;

  private String trackingUrl;

  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderLine> lines = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getShippingAddressLine() {
    return shippingAddressLine;
  }

  public void setShippingAddressLine(String shippingAddressLine) {
    this.shippingAddressLine = shippingAddressLine;
  }

  public String getShippingCity() {
    return shippingCity;
  }

  public void setShippingCity(String shippingCity) {
    this.shippingCity = shippingCity;
  }

  public String getShippingState() {
    return shippingState;
  }

  public void setShippingState(String shippingState) {
    this.shippingState = shippingState;
  }

  public String getShippingPinCode() {
    return shippingPinCode;
  }

  public void setShippingPinCode(String shippingPinCode) {
    this.shippingPinCode = shippingPinCode;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getShippingCountry() {
    return shippingCountry;
  }

  public void setShippingCountry(String shippingCountry) {
    this.shippingCountry = shippingCountry;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public String getCouponCode() {
    return couponCode;
  }

  public void setCouponCode(String couponCode) {
    this.couponCode = couponCode;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount;
  }

  public BigDecimal getShippingFee() {
    return shippingFee;
  }

  public void setShippingFee(BigDecimal shippingFee) {
    this.shippingFee = shippingFee;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public String getPaymentProvider() {
    return paymentProvider;
  }

  public void setPaymentProvider(String paymentProvider) {
    this.paymentProvider = paymentProvider;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public void setPaymentReference(String paymentReference) {
    this.paymentReference = paymentReference;
  }

  public String getPaymentUrl() {
    return paymentUrl;
  }

  public void setPaymentUrl(String paymentUrl) {
    this.paymentUrl = paymentUrl;
  }

  public String getTrackingCourier() {
    return trackingCourier;
  }

  public void setTrackingCourier(String trackingCourier) {
    this.trackingCourier = trackingCourier;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  public String getTrackingUrl() {
    return trackingUrl;
  }

  public void setTrackingUrl(String trackingUrl) {
    this.trackingUrl = trackingUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<OrderLine> getLines() {
    return lines;
  }

  public void setLines(List<OrderLine> lines) {
    this.lines = lines;
  }
}

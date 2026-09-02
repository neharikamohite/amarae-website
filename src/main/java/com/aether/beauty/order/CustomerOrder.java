package com.aether.beauty.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CustomerOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  private BigDecimal shippingFee = BigDecimal.ZERO;

  private BigDecimal total = BigDecimal.ZERO;

  private String paymentProvider;

  private String paymentReference;

  private String paymentUrl;

  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderLine> lines = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

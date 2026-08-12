package com.aether.beauty.payment;

import com.aether.beauty.order.CustomerOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class RazorpayPaymentGateway {
  private final PaymentProperties paymentProperties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public RazorpayPaymentGateway(
    PaymentProperties paymentProperties,
    ObjectMapper objectMapper
  ) {
    this.paymentProperties = paymentProperties;
    this.objectMapper = objectMapper;
  }

  public PaymentSession createPayment(CustomerOrder order) {
    try {
      String keyId = paymentProperties.getRazorpay().getKeyId();
      String keySecret = paymentProperties.getRazorpay().getKeySecret();
      if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
        throw new IllegalStateException("Razorpay credentials are not configured");
      }

      BigDecimal paise = order
        .getTotal()
        .multiply(BigDecimal.valueOf(100))
        .setScale(0, RoundingMode.HALF_UP);
      String body = objectMapper
        .createObjectNode()
        .put("amount", paise.longValueExact())
        .put("currency", paymentProperties.getCurrency())
        .put("receipt", "aether-order-" + order.getId())
        .toString();

      String token = Base64
        .getEncoder()
        .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
      HttpRequest request = HttpRequest
        .newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
        .header("Authorization", "Basic " + token)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

      HttpResponse<String> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      );
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Razorpay rejected payment request");
      }

      JsonNode json = objectMapper.readTree(response.body());
      String reference = json.path("id").asText();
      String url = "/checkout.html?provider=razorpay&orderId=" + reference;
      return new PaymentSession("razorpay", reference, url);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to create Razorpay payment", ex);
    }
  }
}

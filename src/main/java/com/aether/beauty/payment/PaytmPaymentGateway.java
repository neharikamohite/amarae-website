package com.aether.beauty.payment;

import com.aether.beauty.order.CustomerOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Paytm's flow, in order:
 *  1. Server-to-server "Initiate Transaction" call (this class) — returns
 *     a short-lived txnToken.
 *  2. The customer's browser is sent to our own /api/payments/paytm/redirect
 *     endpoint, which auto-submits a form POST to Paytm's hosted payment
 *     page using that token (Paytm requires this be a real browser POST,
 *     not something we can fetch server-side).
 *  3. Paytm POSTs the result back to our callback endpoint, signed with
 *     the same checksum algorithm, which we verify before trusting it.
 *
 * NOT YET TESTED against Paytm's servers — this environment has no
 * network access to verify it. Test with the sandbox/test credentials
 * from your Paytm dashboard before this ever sees a real transaction.
 */
@Component
public class PaytmPaymentGateway {
  private final PaymentProperties paymentProperties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final String siteUrl;

  public PaytmPaymentGateway(
    PaymentProperties paymentProperties,
    ObjectMapper objectMapper,
    @Value("${aether.site-url}") String siteUrl
  ) {
    this.paymentProperties = paymentProperties;
    this.objectMapper = objectMapper;
    this.siteUrl = siteUrl;
  }

  public PaymentSession createPayment(CustomerOrder order) {
    try {
      PaymentProperties.Paytm config = paymentProperties.getPaytm();
      String mid = config.getMerchantId();
      String merchantKey = config.getMerchantKey();
      if (mid == null || mid.isBlank() || merchantKey == null || merchantKey.isBlank()) {
        throw new IllegalStateException("Paytm credentials are not configured");
      }

      String paytmOrderId = "AMARAE" + order.getId() + "T" + System.currentTimeMillis();
      String callbackUrl = siteUrl + "/api/payments/paytm/callback";

      var body = objectMapper.createObjectNode();
      body.put("requestType", "Payment");
      body.put("mid", mid);
      body.put("websiteName", config.getWebsite());
      body.put("orderId", paytmOrderId);
      body.put("callbackUrl", callbackUrl);
      var txnAmount = body.putObject("txnAmount");
      txnAmount.put("value", order.getTotal().setScale(2).toString());
      txnAmount.put("currency", paymentProperties.getCurrency());
      var userInfo = body.putObject("userInfo");
      userInfo.put("custId", "GUEST" + order.getId());

      String bodyJson = objectMapper.writeValueAsString(body);
      String signature = PaytmChecksumUtil.generateSignature(bodyJson, merchantKey);

      var requestRoot = objectMapper.createObjectNode();
      requestRoot.set("body", body);
      requestRoot.putObject("head").put("signature", signature);

      String baseUrl = "live".equalsIgnoreCase(config.getEnvironment())
        ? "https://secure.paytmpayments.com"
        : "https://securegw-stage.paytmpayments.com";
      String initiateUrl = baseUrl
        + "/theia/api/v1/initiateTransaction?mid="
        + URLEncoder.encode(mid, StandardCharsets.UTF_8)
        + "&orderId="
        + URLEncoder.encode(paytmOrderId, StandardCharsets.UTF_8);

      HttpRequest request = HttpRequest
        .newBuilder(URI.create(initiateUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestRoot)))
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Paytm rejected the initiate-transaction request");
      }

      JsonNode json = objectMapper.readTree(response.body());
      JsonNode resultInfo = json.path("body").path("resultInfo");
      if (!"S".equalsIgnoreCase(resultInfo.path("resultStatus").asText())) {
        throw new IllegalStateException(
          "Paytm did not return success: " + resultInfo.path("resultMsg").asText("unknown error")
        );
      }
      String txnToken = json.path("body").path("txnToken").asText();

      String redirectUrl = "/api/payments/paytm/redirect?orderId="
        + URLEncoder.encode(paytmOrderId, StandardCharsets.UTF_8)
        + "&txnToken="
        + URLEncoder.encode(txnToken, StandardCharsets.UTF_8);

      return new PaymentSession("paytm", paytmOrderId, redirectUrl);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to create Paytm payment", ex);
    }
  }

  /** The literal HTML page that auto-submits the browser into Paytm's hosted checkout. */
  public String buildRedirectHtml(String orderId, String txnToken) {
    PaymentProperties.Paytm config = paymentProperties.getPaytm();
    String baseUrl = "live".equalsIgnoreCase(config.getEnvironment())
      ? "https://secure.paytmpayments.com"
      : "https://securegw-stage.paytmpayments.com";
    String showPaymentPageUrl = baseUrl + "/theia/api/v1/showPaymentPage";
    return "<!doctype html><html><head><title>Redirecting to Paytm…</title></head><body>"
      + "<p>Redirecting to Paytm, please wait…</p>"
      + "<form method=\"post\" id=\"paytmRedirectForm\" action=\""
      + showPaymentPageUrl
      + "\">"
      + "<input type=\"hidden\" name=\"mid\" value=\""
      + escapeHtml(config.getMerchantId())
      + "\" />"
      + "<input type=\"hidden\" name=\"orderId\" value=\""
      + escapeHtml(orderId)
      + "\" />"
      + "<input type=\"hidden\" name=\"txnToken\" value=\""
      + escapeHtml(txnToken)
      + "\" />"
      + "</form>"
      + "<script>document.getElementById('paytmRedirectForm').submit();</script>"
      + "</body></html>";
  }

  private String escapeHtml(String value) {
    return value == null
      ? ""
      : value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}

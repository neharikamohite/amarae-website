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
 *     endpoint, which loads Paytm's own JS Checkout script and opens the
 *     payment form directly on that page using the token — there is no
 *     full-page redirect or form POST involved (that pattern belongs to
 *     Paytm's older, deprecated Standard Checkout flow).
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

      // Field order matches Paytm's own published example exactly, not
      // just "any valid JSON" — a standard parser shouldn't care about
      // key order, but there's no upside to being the one to find out
      // their backend disagrees.
      var body = objectMapper.createObjectNode();
      body.put("requestType", "Payment");
      body.put("mid", mid);
      body.put("websiteName", config.getWebsite());
      body.put("orderId", paytmOrderId);
      var txnAmount = body.putObject("txnAmount");
      txnAmount.put("value", order.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toString());
      txnAmount.put("currency", paymentProperties.getCurrency());
      var userInfo = body.putObject("userInfo");
      userInfo.put("custId", "GUEST" + order.getId());
      body.put("callbackUrl", callbackUrl);

      String bodyJson = objectMapper.writeValueAsString(body);
      String signature = PaytmChecksumUtil.generateSignature(bodyJson, merchantKey);

      var requestRoot = objectMapper.createObjectNode();
      requestRoot.set("body", body);
      requestRoot.putObject("head").put("signature", signature);

      // Confirmed against Paytm's actual, current official documentation
      // (matching the exact curl examples they publish, not just prose
      // descriptions) — securestage.paytmpayments.com for staging,
      // secure.paytmpayments.com for production. Paytm has an older,
      // separate "paytm.in" domain family documented elsewhere that looks
      // equally plausible but is a different, legacy system — that's what
      // this code used briefly before, which connected successfully but
      // was rejected with "System Error" since it wasn't the same system
      // this merchant account (onboarded via paytmpayments.com) lives on.
      String baseUrl = "live".equalsIgnoreCase(config.getEnvironment())
        ? "https://secure.paytmpayments.com"
        : "https://securestage.paytmpayments.com";
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
        throw new IllegalStateException(
          "Paytm rejected the initiate-transaction request (HTTP " + response.statusCode() + "): " + response.body()
        );
      }

      JsonNode json = objectMapper.readTree(response.body());
      JsonNode resultInfo = json.path("body").path("resultInfo");
      if (!"S".equalsIgnoreCase(resultInfo.path("resultStatus").asText())) {
        // Surfacing the full raw response, not just resultMsg — Paytm's
        // "System Error" alone gives no clue which field it didn't like;
        // resultCode and any extra fields in the full body usually do.
        throw new IllegalStateException(
          "Paytm did not return success (code "
            + resultInfo.path("resultCode").asText("?")
            + ", "
            + resultInfo.path("resultMsg").asText("unknown error")
            + "). Full response: "
            + response.body()
            + " | Request sent: "
            + objectMapper.writeValueAsString(requestRoot)
        );
      }
      String txnToken = json.path("body").path("txnToken").asText();

      String redirectUrl = "/api/payments/paytm/redirect?orderId="
        + URLEncoder.encode(paytmOrderId, StandardCharsets.UTF_8)
        + "&txnToken="
        + URLEncoder.encode(txnToken, StandardCharsets.UTF_8)
        + "&amount="
        + URLEncoder.encode(order.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toString(), StandardCharsets.UTF_8);

      return new PaymentSession("paytm", paytmOrderId, redirectUrl);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to create Paytm payment", ex);
    }
  }

  /**
   * The page the customer's browser lands on after Initiate Transaction
   * succeeds. This uses Paytm's actual current "JS Checkout" method — a
   * script tag scoped to this merchant's MID, then init()/invoke() calls
   * that render the payment form directly on this page (Paytm calls it
   * an iframe, though from the outside it just looks like a modal).
   * There is NO full-page redirect or form POST to Paytm's site — that
   * pattern belongs to their older, deprecated "Standard Checkout" flow,
   * and mixing it with a JS-Checkout-issued txnToken is a real bug this
   * replaced (confirmed against Paytm's official "Invoke Payment Page"
   * documentation, including their exact published script/config shape).
   */
  public String buildRedirectHtml(String orderId, String txnToken, String amount) {
    PaymentProperties.Paytm config = paymentProperties.getPaytm();
    String baseUrl = "live".equalsIgnoreCase(config.getEnvironment())
      ? "https://secure.paytmpayments.com"
      : "https://securestage.paytmpayments.com";
    String scriptUrl = baseUrl + "/merchantpgpui/checkoutjs/merchants/" + escapeJs(config.getMerchantId()) + ".js";

    return "<!doctype html><html><head><title>Redirecting to Paytm…</title>"
      + "<meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0\"/>"
      + "<script type=\"application/javascript\" src=\""
      + scriptUrl
      + "\" onload=\"onScriptLoad();\" crossorigin=\"anonymous\"></script>"
      + "<script>"
      + "function onScriptLoad(){"
      + "var config={"
      + "\"root\":\"\","
      + "\"flow\":\"DEFAULT\","
      + "\"data\":{"
      + "\"orderId\":\"" + escapeJs(orderId) + "\","
      + "\"token\":\"" + escapeJs(txnToken) + "\","
      + "\"tokenType\":\"TXN_TOKEN\","
      + "\"amount\":\"" + escapeJs(amount) + "\""
      + "},"
      + "\"handler\":{"
      + "\"notifyMerchant\":function(eventName,data){"
      // "APP_CLOSED" is my best guess at Paytm's event name for "user
      // closed the checkout without paying" — their docs list the
      // handler's existence but not the exact event name values, so
      // this is unconfirmed. Harmless if wrong: it just won't redirect,
      // the actual payment flow above doesn't depend on it.
      + "if(eventName==='APP_CLOSED'){window.location.href='/collections.html';}"
      + "}"
      + "}"
      + "};"
      + "if(window.Paytm&&window.Paytm.CheckoutJS){"
      + "window.Paytm.CheckoutJS.onLoad(function(){"
      + "window.Paytm.CheckoutJS.init(config).then(function(){"
      + "window.Paytm.CheckoutJS.invoke();"
      + "}).catch(function(error){"
      + "document.body.innerHTML='<p>Could not open Paytm checkout. Please go back and try again.</p>';"
      + "console.log('Paytm init error',error);"
      + "});"
      + "});"
      + "}"
      + "}"
      + "</script>"
      + "</head><body><p>Loading payment…</p></body></html>";
  }

  private String escapeJs(String value) {
    return value == null
      ? ""
      : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003C").replace(">", "\\u003E");
  }

}

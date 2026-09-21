package com.aether.beauty.api;

import com.aether.beauty.api.dto.OrderDto;
import com.aether.beauty.api.dto.PaymentCallbackRequest;
import com.aether.beauty.api.dto.PaymentConfigDto;
import com.aether.beauty.api.dto.RazorpayVerifyRequest;
import com.aether.beauty.payment.PaymentProperties;
import com.aether.beauty.payment.PaymentService;
import com.aether.beauty.payment.PaymentStatus;
import com.aether.beauty.payment.PaytmChecksumUtil;
import com.aether.beauty.payment.PaytmPaymentGateway;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final PaymentService paymentService;
  private final PaymentProperties paymentProperties;
  private final PaytmPaymentGateway paytmPaymentGateway;
  private final ApiMapper apiMapper;

  public PaymentController(
    PaymentService paymentService,
    PaymentProperties paymentProperties,
    PaytmPaymentGateway paytmPaymentGateway,
    ApiMapper apiMapper
  ) {
    this.paymentService = paymentService;
    this.paymentProperties = paymentProperties;
    this.paytmPaymentGateway = paytmPaymentGateway;
    this.apiMapper = apiMapper;
  }

  @GetMapping("/config")
  public PaymentConfigDto config() {
    return new PaymentConfigDto(
      paymentProperties.getGateway(),
      paymentProperties.getRazorpay().getKeyId(),
      paymentProperties.getCurrency()
    );
  }

  @PostMapping("/callback")
  public OrderDto paymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
    PaymentStatus status = PaymentStatus.valueOf(request.status().toUpperCase());
    return apiMapper.toOrderDto(paymentService.markPayment(request.providerReference(), status));
  }

  // Step 2 of the Paytm flow — the customer's browser lands here after we
  // initiate the transaction server-side; this page loads Paytm's JS
  // Checkout script and opens the payment form directly, using the token
  // from that call.
  @GetMapping(value = "/paytm/redirect", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> paytmRedirect(
    @RequestParam String orderId,
    @RequestParam String txnToken,
    @RequestParam String amount
  ) {
    return ResponseEntity.ok(paytmPaymentGateway.buildRedirectHtml(orderId, txnToken, amount));
  }

  // Step 3 — Paytm posts the result back here once the customer finishes
  // paying. NOT YET VERIFIED against a real Paytm callback: the checksum
  // check below follows Paytm's documented NVP (sorted key=value) format,
  // the most consistently documented approach across their SDKs, but the
  // *exact* field names/shape Paytm's v2 API sends can only be confirmed
  // by running one real test transaction — check the actual received
  // fields against this if a genuine test payment doesn't come through
  // as PAID here.
  @PostMapping(value = "/paytm/callback", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> paytmCallback(@RequestParam Map<String, String> allParams) {
    String orderId = allParams.get("ORDERID");
    String receivedChecksum = allParams.get("CHECKSUMHASH");
    String txnStatus = allParams.getOrDefault("STATUS", "");

    Map<String, String> forVerification = new TreeMap<>(allParams);
    forVerification.remove("CHECKSUMHASH");
    String dataToVerify = forVerification
      .entrySet()
      .stream()
      .map(e -> e.getKey() + "=" + e.getValue())
      .collect(Collectors.joining("&"));

    boolean valid = orderId != null
      && receivedChecksum != null
      && PaytmChecksumUtil.verifySignature(
        dataToVerify,
        paymentProperties.getPaytm().getMerchantKey(),
        receivedChecksum
      );

    String redirectTarget;
    if (valid && "TXN_SUCCESS".equalsIgnoreCase(txnStatus)) {
      paymentService.markPayment(orderId, PaymentStatus.PAID);
      redirectTarget = paymentProperties.getSuccessUrl();
    } else {
      // Checksum failed, or Paytm itself reported a non-success status —
      // never mark an order PAID unless both checks pass.
      if (valid && orderId != null) {
        paymentService.markPayment(orderId, PaymentStatus.FAILED);
      }
      redirectTarget = "/collections.html?payment=failed";
    }

    return ResponseEntity
      .status(302)
      .header(HttpHeaders.LOCATION, redirectTarget)
      .body("");
  }

  @GetMapping("/demo-success")
  public ResponseEntity<String> demoSuccess(@RequestParam String reference) {
    paymentService.markPayment(reference, PaymentStatus.PAID);
    return ResponseEntity.ok("Demo payment completed. You can return to the AETHER shop.");
  }

  @PostMapping("/razorpay/verify")
  public OrderDto verifyRazorpay(@Valid @RequestBody RazorpayVerifyRequest request) {
    return apiMapper.toOrderDto(
      paymentService.verifyRazorpay(
        request.razorpayOrderId(),
        request.razorpayPaymentId(),
        request.razorpaySignature()
      )
    );
  }
}

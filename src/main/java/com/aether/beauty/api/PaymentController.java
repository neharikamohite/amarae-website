package com.aether.beauty.api;

import com.aether.beauty.api.dto.OrderDto;
import com.aether.beauty.api.dto.PaymentCallbackRequest;
import com.aether.beauty.api.dto.PaymentConfigDto;
import com.aether.beauty.api.dto.RazorpayVerifyRequest;
import com.aether.beauty.payment.PaymentProperties;
import com.aether.beauty.payment.PaymentService;
import com.aether.beauty.payment.PaymentStatus;
import jakarta.validation.Valid;
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
  private final ApiMapper apiMapper;

  public PaymentController(
    PaymentService paymentService,
    PaymentProperties paymentProperties,
    ApiMapper apiMapper
  ) {
    this.paymentService = paymentService;
    this.paymentProperties = paymentProperties;
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

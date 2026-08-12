package com.aether.beauty.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aether.payment")
public class PaymentProperties {
  private String gateway = "demo";
  private String currency = "INR";
  private String successUrl;
  private Razorpay razorpay = new Razorpay();

  public String getGateway() {
    return gateway;
  }

  public void setGateway(String gateway) {
    this.gateway = gateway;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getSuccessUrl() {
    return successUrl;
  }

  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }

  public Razorpay getRazorpay() {
    return razorpay;
  }

  public void setRazorpay(Razorpay razorpay) {
    this.razorpay = razorpay;
  }

  public static class Razorpay {
    private String keyId;
    private String keySecret;

    public String getKeyId() {
      return keyId;
    }

    public void setKeyId(String keyId) {
      this.keyId = keyId;
    }

    public String getKeySecret() {
      return keySecret;
    }

    public void setKeySecret(String keySecret) {
      this.keySecret = keySecret;
    }
  }
}

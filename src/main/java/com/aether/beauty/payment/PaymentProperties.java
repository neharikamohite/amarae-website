package com.aether.beauty.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aether.payment")
public class PaymentProperties {
  private String gateway = "demo";
  private String currency = "INR";
  private String successUrl;
  private Razorpay razorpay = new Razorpay();
  private Paytm paytm = new Paytm();

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

  public Paytm getPaytm() {
    return paytm;
  }

  public void setPaytm(Paytm paytm) {
    this.paytm = paytm;
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

  public static class Paytm {
    private String merchantId;
    private String merchantKey;
    private String website = "WEBSTAGING";
    // "test" hits Paytm's staging environment (securegw-stage...), "live"
    // hits production (secure...) — this is a separate switch from
    // aether.payment.gateway so a real Paytm merchant ID never
    // accidentally goes live before you're ready to.
    private String environment = "test";

    public String getMerchantId() {
      return merchantId;
    }

    public void setMerchantId(String merchantId) {
      this.merchantId = merchantId;
    }

    public String getMerchantKey() {
      return merchantKey;
    }

    public void setMerchantKey(String merchantKey) {
      this.merchantKey = merchantKey;
    }

    public String getWebsite() {
      return website;
    }

    public void setWebsite(String website) {
      this.website = website;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }
  }
}

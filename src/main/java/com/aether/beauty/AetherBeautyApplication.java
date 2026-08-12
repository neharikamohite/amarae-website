package com.aether.beauty;

import com.aether.beauty.payment.PaymentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PaymentProperties.class)
public class AetherBeautyApplication {
  public static void main(String[] args) {
    SpringApplication.run(AetherBeautyApplication.class, args);
  }
}

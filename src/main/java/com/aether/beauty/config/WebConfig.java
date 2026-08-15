package com.aether.beauty.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
      .addMapping("/api/**")
      .allowedOriginPatterns(
        "http://localhost:*",
        "https://localhost:*",
        "http://127.0.0.1:*",
        "https://*.onrender.com",
        "https://amaraeformulations.com",
        "http://amaraeformulations.com"
      )
      .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/index.html");
  }
}

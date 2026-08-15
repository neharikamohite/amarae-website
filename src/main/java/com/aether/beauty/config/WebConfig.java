package com.aether.beauty.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String projectRoot = Path.of("").toAbsolutePath().normalize().toUri().toString();
    registry
      .addResourceHandler("/*.html", "/style.css", "/javas.js", "/assets/**")
      .addResourceLocations(projectRoot, projectRoot + "assets/");
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/index.html");
  }
}

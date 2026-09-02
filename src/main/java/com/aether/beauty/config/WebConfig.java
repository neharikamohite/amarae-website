package com.aether.beauty.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${aether.media.storage-dir:uploads/reviews}")
  private String mediaStorageDir;

  @Value("${aether.media.public-path:/media/reviews}")
  private String mediaPublicPath;

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

  // Serves uploaded review photos/videos from disk at the public URL path
  // MediaStorageService hands out. See that class for the caveat about
  // ephemeral storage on default Render web-service plans.
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = mediaStorageDir.endsWith("/") ? mediaStorageDir : mediaStorageDir + "/";
    registry
      .addResourceHandler(mediaPublicPath + "/**")
      .addResourceLocations("file:" + location);
  }
}

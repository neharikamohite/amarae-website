package com.aether.beauty.api;

import com.aether.beauty.api.dto.AuthResponse;
import com.aether.beauty.api.dto.LoginRequest;
import com.aether.beauty.api.dto.SignupRequest;
import com.aether.beauty.api.dto.UserDto;
import com.aether.beauty.auth.AuthService;
import com.aether.beauty.auth.AuthToken;
import com.aether.beauty.auth.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
    AuthToken token = authService.signup(request.name(), request.email(), request.password());
    return toAuthResponse(token);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    AuthToken token = authService.login(request.email(), request.password());
    return toAuthResponse(token);
  }

  @DeleteMapping("/logout")
  public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
    authService.logout(bearerToken(authorization));
  }

  @GetMapping("/me")
  public UserDto me(@RequestHeader(value = "Authorization", required = false) String authorization) {
    User user = authService.requireUser(bearerToken(authorization));
    return new UserDto(user.getId(), user.getName(), user.getEmail());
  }

  private AuthResponse toAuthResponse(AuthToken authToken) {
    User user = authToken.getUser();
    return new AuthResponse(authToken.getToken(), user.getName(), user.getEmail());
  }

  // Accepts a plain token or an "Authorization: Bearer <token>" header —
  // the frontend sends the latter.
  static String bearerToken(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return null;
    }
    return authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
  }
}

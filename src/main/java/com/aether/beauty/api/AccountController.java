package com.aether.beauty.api;

import com.aether.beauty.api.dto.OrderDto;
import com.aether.beauty.api.dto.SavedAddressDto;
import com.aether.beauty.api.dto.SavedAddressRequest;
import com.aether.beauty.auth.AuthService;
import com.aether.beauty.auth.SavedAddress;
import com.aether.beauty.auth.SavedAddressRepository;
import com.aether.beauty.auth.User;
import com.aether.beauty.order.CustomerOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
  private final AuthService authService;
  private final CustomerOrderRepository customerOrderRepository;
  private final SavedAddressRepository savedAddressRepository;
  private final ApiMapper apiMapper;

  public AccountController(
    AuthService authService,
    CustomerOrderRepository customerOrderRepository,
    SavedAddressRepository savedAddressRepository,
    ApiMapper apiMapper
  ) {
    this.authService = authService;
    this.customerOrderRepository = customerOrderRepository;
    this.savedAddressRepository = savedAddressRepository;
    this.apiMapper = apiMapper;
  }

  @GetMapping("/orders")
  @Transactional(readOnly = true)
  public List<OrderDto> orderHistory(@RequestHeader(value = "Authorization", required = false) String authorization) {
    User user = authService.requireUser(AuthController.bearerToken(authorization));
    return customerOrderRepository
      .findByUserIdOrderByCreatedAtDesc(user.getId())
      .stream()
      .map(apiMapper::toOrderDto)
      .toList();
  }

  @GetMapping("/addresses")
  public List<SavedAddressDto> addresses(@RequestHeader(value = "Authorization", required = false) String authorization) {
    User user = authService.requireUser(AuthController.bearerToken(authorization));
    return savedAddressRepository.findByUserIdOrderByIdDesc(user.getId()).stream().map(this::toDto).toList();
  }

  @PostMapping("/addresses")
  public SavedAddressDto addAddress(
    @RequestHeader(value = "Authorization", required = false) String authorization,
    @Valid @RequestBody SavedAddressRequest request
  ) {
    User user = authService.requireUser(AuthController.bearerToken(authorization));
    SavedAddress address = new SavedAddress();
    address.setUser(user);
    address.setLabel(request.label().trim());
    address.setAddressLine(request.addressLine().trim());
    address.setCity(request.city().trim());
    address.setState(request.state().trim());
    address.setPinCode(request.pinCode().trim());
    address.setPhone(request.phone().trim());
    return toDto(savedAddressRepository.save(address));
  }

  @DeleteMapping("/addresses/{id}")
  public void deleteAddress(
    @RequestHeader(value = "Authorization", required = false) String authorization,
    @PathVariable Long id
  ) {
    User user = authService.requireUser(AuthController.bearerToken(authorization));
    SavedAddress address = savedAddressRepository
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Address not found"));
    if (!address.getUser().getId().equals(user.getId())) {
      // Don't leak whether the address exists at all if it belongs to
      // someone else — same "not found" as a bad id.
      throw new EntityNotFoundException("Address not found");
    }
    savedAddressRepository.delete(address);
  }

  private SavedAddressDto toDto(SavedAddress address) {
    return new SavedAddressDto(
      address.getId(),
      address.getLabel(),
      address.getAddressLine(),
      address.getCity(),
      address.getState(),
      address.getPinCode(),
      address.getPhone()
    );
  }
}

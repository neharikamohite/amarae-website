package com.aether.beauty.auth;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {
  List<SavedAddress> findByUserIdOrderByIdDesc(Long userId);
}

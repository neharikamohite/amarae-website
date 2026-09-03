package com.aether.beauty.admin;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {
  Optional<AdminSession> findByToken(String token);

  void deleteByToken(String token);
}

package com.aether.beauty.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "customer_order.delivery_city" is a leftover column from a much earlier
 * version of the schema — before this codebase's CustomerOrder entity
 * existed, orders used a single combined "delivery city" field. Nothing
 * has written that entity field in a long time, but the database column
 * itself still exists with a NOT NULL constraint from back then, so
 * every insert failed regardless of what the current code actually does.
 *
 * Hibernate's ddl-auto=update only ever adds columns, never removes or
 * relaxes constraints on ones that already exist, so this leftover was
 * never going to fix itself. This runs the one-time cleanup directly,
 * safely, on every startup — it's a no-op once the column is gone.
 */
@Component
public class LegacyColumnCleanup implements CommandLineRunner {
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  @Transactional
  public void run(String... args) {
    List<?> exists = entityManager
      .createNativeQuery(
        "SELECT 1 FROM information_schema.columns " +
        "WHERE table_name = 'customer_order' AND column_name = 'delivery_city'"
      )
      .getResultList();
    if (exists.isEmpty()) {
      return;
    }
    entityManager.createNativeQuery("ALTER TABLE customer_order DROP COLUMN delivery_city").executeUpdate();
  }
}

package com.bookswap.valuation_service.repository;

import com.bookswap.valuation_service.domain.valuation.Valuation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValuationRepository extends JpaRepository<Valuation, String> {
  Optional<Valuation> findByBookId(String bookId);

  void deleteByBookId(String bookId);
}

package com.bookswap.catalog_service.repository;

import com.bookswap.catalog_service.domain.book.Book;
import com.bookswap.catalog_service.domain.book.BookStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, String> {
  Optional<Book> findByBookId(String bookId);

  List<Book> findByOwnerUserId(String findByOwnerUserId);

  void deleteByBookId(String bookId);

  List<Book> findByBookStatusOrderByCreatedAtDesc(BookStatus status, Pageable pageable);

  @Query(
      "SELECT b FROM Book b WHERE b.bookStatus = :status AND b.bookId <> :bookId AND b.valuation BETWEEN :minVal AND :maxVal")
  List<Book> findMatchingBooks(
      @Param("status") BookStatus status,
      @Param("bookId") String bookId,
      @Param("minVal") Double minVal,
      @Param("maxVal") Double maxVal,
      Pageable pageable);

  Optional<Book> findByBookIdAndOwnerUserId(String bookId, String ownerUserId);
}

package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Payment;
import com.nhom7.coworkingspace.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query(value = """
            SELECT
                EXTRACT(MONTH FROM paid_at) AS month,
                COALESCE(SUM(amount), 0) AS revenue
            FROM payment
            WHERE EXTRACT(YEAR FROM paid_at) = :year
              AND UPPER(status) = 'COMPLETED'
            GROUP BY EXTRACT(MONTH FROM paid_at)
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueByYear(
            @Param("year") int year
    );

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM payment
            WHERE EXTRACT(YEAR FROM paid_at) = :year
              AND UPPER(status) = 'COMPLETED'
            """, nativeQuery = true)
    BigDecimal findTotalRevenueByYear(
            @Param("year") int year
    );

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findAllByOrderByPaidAtDesc();

    @EntityGraph(attributePaths = "booking")
    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.status IN :supportedStatuses
              AND (:keywordMissing = true
                   OR LOWER(payment.transactionId) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:statusMissing = true OR payment.status = :status)
              AND (:paymentMethodMissing = true
                   OR UPPER(payment.paymentMethod) = UPPER(:paymentMethod))
              AND (:fromPaidAtMissing = true OR payment.paidAt >= :fromPaidAt)
              AND (:toPaidAtMissing = true OR payment.paidAt < :toPaidAtExclusive)
            """)
    Page<Payment> searchPayments(
            @Param("supportedStatuses") List<PaymentStatus> supportedStatuses,
            @Param("keywordMissing") boolean keywordMissing,
            @Param("keyword") String keyword,
            @Param("statusMissing") boolean statusMissing,
            @Param("status") PaymentStatus status,
            @Param("paymentMethodMissing") boolean paymentMethodMissing,
            @Param("paymentMethod") String paymentMethod,
            @Param("fromPaidAtMissing") boolean fromPaidAtMissing,
            @Param("fromPaidAt") LocalDateTime fromPaidAt,
            @Param("toPaidAtMissing") boolean toPaidAtMissing,
            @Param("toPaidAtExclusive") LocalDateTime toPaidAtExclusive,
            Pageable pageable);
}

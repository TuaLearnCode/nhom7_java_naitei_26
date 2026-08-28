package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.PaymentSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.PaymentResponse;
import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.entity.Payment;
import com.nhom7.coworkingspace.enums.PaymentStatus;
import com.nhom7.coworkingspace.enums.VenueStatus;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.PaymentRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String SUCCESSFUL_BOOKING_STATUS = "COMPLETED";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public StatisticsOverviewResponse getOverview() {

        long totalUsers =
                userRepository.count();

        long successfulBookings =
                bookingRepository.countByStatusIgnoreCase(
                        SUCCESSFUL_BOOKING_STATUS
                );

        long activeVenues =
                venueRepository.countByStatus(
                        VenueStatus.APPROVE
                );

        return StatisticsOverviewResponse.builder()
                .totalUsers(totalUsers)
                .successfulBookings(successfulBookings)
                .activeVenues(activeVenues)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueStatisticsResponse getRevenueByYear(int year) {

        BigDecimal totalRevenue =
                paymentRepository.findTotalRevenueByYear(year);

        List<Object[]> monthlyData =
                paymentRepository.findMonthlyRevenueByYear(year);

        Map<Integer, BigDecimal> revenueByMonth =
                new HashMap<>();

        for (Object[] row : monthlyData) {

            int month =
                    ((Number) row[0]).intValue();

            BigDecimal revenue =
                    new BigDecimal(
                            row[1].toString()
                    );

            revenueByMonth.put(
                    month,
                    revenue
            );
        }

        List<RevenueStatisticsResponse.MonthlyRevenue>
                monthlyRevenue = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {

            monthlyRevenue.add(
                    RevenueStatisticsResponse.MonthlyRevenue
                            .builder()
                            .month(month)
                            .revenue(
                                    revenueByMonth.getOrDefault(
                                            month,
                                            BigDecimal.ZERO
                                    )
                            )
                            .build()
            );
        }

        return RevenueStatisticsResponse.builder()
                .year(year)
                .totalRevenue(
                        totalRevenue != null
                                ? totalRevenue
                                : BigDecimal.ZERO
                )
                .monthlyRevenue(monthlyRevenue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        List<Payment> payments = paymentRepository.findAllByOrderByPaidAtDesc();
        return payments.stream().map(this::toPaymentResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> searchPayments(PaymentSearchRequest request) {
        int pageNumber = Math.max(0, request.getPage());
        int requestedSize = request.getSize() <= 0 ? DEFAULT_PAGE_SIZE : request.getSize();
        int pageSize = Math.min(requestedSize, MAX_PAGE_SIZE);

        PageRequest pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "paidAt"));

        String keyword = normalize(request.getKeyword());
        PaymentStatus status = parsePaymentStatus(request.getStatus());
        String paymentMethod = normalize(request.getPaymentMethod());
        LocalDateTime fromPaidAt = request.getFromDate() == null
                ? LocalDateTime.of(1970, 1, 1, 0, 0)
                : request.getFromDate().atStartOfDay();
        LocalDateTime toPaidAtExclusive = request.getToDate() == null
                ? LocalDateTime.of(9999, 12, 31, 23, 59)
                : request.getToDate().plusDays(1).atStartOfDay();

        Page<PaymentResponse> payments = paymentRepository.searchPayments(
                        List.of(PaymentStatus.values()),
                        keyword == null,
                        keyword == null ? "" : keyword,
                        status == null,
                        status == null ? PaymentStatus.COMPLETED : status,
                        paymentMethod == null,
                        paymentMethod == null ? "" : paymentMethod,
                        request.getFromDate() == null,
                        fromPaidAt,
                        request.getToDate() == null,
                        toPaidAtExclusive,
                        pageable)
                .map(this::toPaymentResponse);

        return PageResponse.fromPage(payments);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking() == null ? null : payment.getBooking().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus() == null ? null : payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .transactionId(payment.getTransactionId())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PaymentStatus parsePaymentStatus(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : PaymentStatus.valueOf(normalized.toUpperCase());
    }
}

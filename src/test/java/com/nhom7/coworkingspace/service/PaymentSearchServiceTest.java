package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.PaymentSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.PaymentResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Payment;
import com.nhom7.coworkingspace.enums.PaymentStatus;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.PaymentRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSearchServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private PaymentRepository paymentRepository;

    @Test
    void searchPaymentsNormalizesFiltersAndUsesBoundedPagination() {
        StatisticsService service = new StatisticsServiceImpl(
                userRepository, bookingRepository, venueRepository, paymentRepository);
        Payment payment = Payment.builder()
                .id(4L)
                .booking(Booking.builder().id(8L).build())
                .amount(new BigDecimal("125000"))
                .status(PaymentStatus.COMPLETED)
                .paymentMethod("MOMO")
                .transactionId("TXN-004")
                .paidAt(LocalDateTime.of(2026, 8, 20, 9, 0))
                .build();
        given(paymentRepository.searchPayments(
                eq(List.of(PaymentStatus.values())),
                eq(false), eq("TXN"), eq(false), eq(PaymentStatus.COMPLETED), eq(false), eq("MOMO"),
                eq(false), eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(false), eq(LocalDateTime.of(2026, 9, 1, 0, 0)), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(payment)));

        PageResponse<PaymentResponse> result = service.searchPayments(PaymentSearchRequest.builder()
                .keyword("  TXN  ")
                .status("COMPLETED")
                .paymentMethod("MOMO")
                .fromDate(LocalDate.of(2026, 8, 1))
                .toDate(LocalDate.of(2026, 8, 31))
                .page(-3)
                .size(500)
                .build());

        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(4L);
            assertThat(item.getBookingId()).isEqualTo(8L);
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).searchPayments(
                eq(List.of(PaymentStatus.values())),
                eq(false), eq("TXN"), eq(false), eq(PaymentStatus.COMPLETED), eq(false), eq("MOMO"),
                eq(false), eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(false), eq(LocalDateTime.of(2026, 9, 1, 0, 0)), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort().getOrderFor("paidAt").isDescending()).isTrue();
    }

    @Test
    void searchPaymentsUsesTypedSentinelsForMissingFilters() {
        StatisticsService service = new StatisticsServiceImpl(
                userRepository, bookingRepository, venueRepository, paymentRepository);
        given(paymentRepository.searchPayments(
                eq(List.of(PaymentStatus.values())),
                eq(true), eq(""), eq(true), eq(PaymentStatus.COMPLETED), eq(true), eq(""),
                eq(true), eq(LocalDateTime.of(1970, 1, 1, 0, 0)),
                eq(true), eq(LocalDateTime.of(9999, 12, 31, 23, 59)), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        service.searchPayments(PaymentSearchRequest.builder().build());

        verify(paymentRepository).searchPayments(
                eq(List.of(PaymentStatus.values())),
                eq(true), eq(""), eq(true), eq(PaymentStatus.COMPLETED), eq(true), eq(""),
                eq(true), eq(LocalDateTime.of(1970, 1, 1, 0, 0)),
                eq(true), eq(LocalDateTime.of(9999, 12, 31, 23, 59)), any(Pageable.class));
    }
}

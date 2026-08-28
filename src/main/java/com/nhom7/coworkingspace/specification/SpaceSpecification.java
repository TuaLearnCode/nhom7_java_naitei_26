package com.nhom7.coworkingspace.specification;

import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.BookingStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SpaceSpecification {

    private SpaceSpecification() {
        // Private constructor for utility class
    }

    public static Specification<Space> buildSearchSpecification(SpaceSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Space, Venue> venueJoin = root.join("venue", JoinType.INNER);

            // Only active spaces in non-deleted, APPROVED venues are searchable
            predicates.add(cb.equal(venueJoin.get("status"), com.nhom7.coworkingspace.enums.VenueStatus.APPROVE));
            predicates.add(cb.equal(venueJoin.get("deleted"), false));
            predicates.add(cb.equal(root.get("status"), com.nhom7.coworkingspace.enums.SpaceStatus.ACTIVE));

            // Filter by space name or venue name
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                String pattern = "%" + request.getName().trim().toLowerCase() + "%";
                Predicate spaceNameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate venueNameMatch = cb.like(cb.lower(venueJoin.get("name")), pattern);
                predicates.add(cb.or(spaceNameMatch, venueNameMatch));
            }

            // Filter by city
            if (request.getCity() != null && !request.getCity().trim().isEmpty()) {
                String pattern = "%" + request.getCity().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(venueJoin.get("city")), pattern));
            }

            // Filter by street
            if (request.getStreet() != null && !request.getStreet().trim().isEmpty()) {
                String pattern = "%" + request.getStreet().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(venueJoin.get("street")), pattern));
            }

            // Filter by full address
            if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
                String pattern = "%" + request.getAddress().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(venueJoin.get("address")), pattern));
            }

            // Filter by space type (e.g. private office, working desk, meeting space)
            if (request.getType() != null && !request.getType().trim().isEmpty()) {
                String typePattern = "%" + request.getType().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("type")), typePattern));
            }

            // Filter by min price
            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }

            // Filter by max price
            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // Filter by price unit (hour, day, month)
            if (request.getPriceUnit() != null && !request.getPriceUnit().trim().isEmpty()) {
                String normalizedUnit = request.getPriceUnit().trim().toLowerCase();
                predicates.add(cb.equal(cb.lower(root.get("priceUnit")), normalizedUnit));
            }

            // Filter by daily operating openTime (space open <= requested open)
            if (request.getOpenTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("openTime"), request.getOpenTime()));
            }

            // Filter by daily operating closeTime (space close >= requested close)
            if (request.getCloseTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("closeTime"), request.getCloseTime()));
            }

            // Filter available time window (check booking collisions)
            if (request.getBookingStart() != null && request.getBookingEnd() != null && query != null) {
                Subquery<Long> bookedSpaceSubquery = query.subquery(Long.class);
                Root<Booking> bookingRoot = bookedSpaceSubquery.from(Booking.class);

                bookedSpaceSubquery.select(bookingRoot.get("space").get("id"))
                        .where(
                                cb.not(bookingRoot.get("status").in(BookingStatus.CANCELLED, BookingStatus.REJECTED)),
                                cb.lessThan(bookingRoot.get("startTime"), request.getBookingEnd()),
                                cb.greaterThan(bookingRoot.get("endTime"), request.getBookingStart())
                        );

                predicates.add(cb.not(root.get("id").in(bookedSpaceSubquery)));

            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

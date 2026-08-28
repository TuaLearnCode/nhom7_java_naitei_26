package com.nhom7.coworkingspace.util;

import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SpaceSearchValidator implements ConstraintValidator<ValidSpaceSearch, SpaceSearchRequest> {

    @Override
    public boolean isValid(SpaceSearchRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            addViolation(context, "{validation.space.priceRange.invalid}", "maxPrice");
            valid = false;
        }

        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getOpenTime().isBefore(request.getCloseTime())) {
            addViolation(context, "{validation.space.operatingHours.invalid}", "closeTime");
            valid = false;
        }

        if (request.getBookingStart() == null && request.getBookingEnd() != null) {
            addViolation(context, "{validation.space.bookingStart.required}", "bookingStart");
            valid = false;
        } else if (request.getBookingStart() != null && request.getBookingEnd() == null) {
            addViolation(context, "{validation.space.bookingEnd.required}", "bookingEnd");
            valid = false;
        } else if (request.getBookingStart() != null
                && !request.getBookingStart().isBefore(request.getBookingEnd())) {
            addViolation(context, "{validation.space.bookingRange.invalid}", "bookingEnd");
            valid = false;
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String message, String field) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}

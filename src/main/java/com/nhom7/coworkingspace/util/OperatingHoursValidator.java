package com.nhom7.coworkingspace.util;

import com.nhom7.coworkingspace.dto.request.SpaceCreateRequest;
import com.nhom7.coworkingspace.dto.request.SpaceUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalTime;

public class OperatingHoursValidator implements ConstraintValidator<ValidOperatingHours, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalTime openTime;
        LocalTime closeTime;
        if (value instanceof SpaceCreateRequest request) {
            openTime = request.getOpenTime();
            closeTime = request.getCloseTime();
        } else if (value instanceof SpaceUpdateRequest request) {
            openTime = request.getOpenTime();
            closeTime = request.getCloseTime();
        } else {
            return true;
        }

        if (openTime == null || closeTime == null || openTime.isBefore(closeTime)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("{validation.space.operatingHours.invalid}")
                .addPropertyNode("closeTime")
                .addConstraintViolation();
        return false;
    }
}

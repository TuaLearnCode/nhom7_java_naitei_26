package com.nhom7.coworkingspace.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = OperatingHoursValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOperatingHours {

    String message() default "{validation.space.operatingHours.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

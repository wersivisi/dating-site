package com.wersiv.datingsite.backend.profile.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class AdultBirthdateValidator implements ConstraintValidator<AdultBirthdate, LocalDate> {
    @Override
    public boolean isValid(LocalDate birthdate, ConstraintValidatorContext context) {
        return birthdate == null || !birthdate.isAfter(LocalDate.now(context.getClockProvider().getClock()).minusYears(18));
    }
}

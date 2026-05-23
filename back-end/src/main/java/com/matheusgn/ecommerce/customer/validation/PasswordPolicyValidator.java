package com.matheusgn.ecommerce.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordPolicyValidator implements ConstraintValidator<PasswordPolicy, String> {

    private static final int MIN = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.length() < MIN) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean special = false;
        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            }
            if (!Character.isLetterOrDigit(c)) {
                special = true;
            }
        }
        return upper && lower && special;
    }
}

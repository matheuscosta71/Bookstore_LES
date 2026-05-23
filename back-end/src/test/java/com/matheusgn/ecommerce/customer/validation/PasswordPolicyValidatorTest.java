package com.matheusgn.ecommerce.customer.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
    }

    @Test
    void acceptsStrongPassword() {
        assertThat(validator.isValid("Abcd1234!", context)).isTrue();
    }

    @Test
    void rejectsTooShort() {
        assertThat(validator.isValid("Ab1!", context)).isFalse();
    }

    @Test
    void rejectsWithoutUppercase() {
        assertThat(validator.isValid("abcd1234!", context)).isFalse();
    }

    @Test
    void rejectsWithoutLowercase() {
        assertThat(validator.isValid("ABCD1234!", context)).isFalse();
    }

    @Test
    void rejectsWithoutSpecial() {
        assertThat(validator.isValid("Abcd1234", context)).isFalse();
    }
}

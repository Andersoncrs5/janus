package org.janus.shared.domain.validations;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;

import java.lang.annotation.*;

@NotBlank
@SlugConstraint
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SlugConstraint {

    String message() default "The slug must be lowercase, contain only letters, numbers and single hyphens.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
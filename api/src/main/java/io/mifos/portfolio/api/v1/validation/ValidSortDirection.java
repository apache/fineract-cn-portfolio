package io.mifos.portfolio.api.v1.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {CheckValidSortDirection.class}
)
public @interface ValidSortDirection {
  String message() default "Only ASC, and DESC are valid sort directions.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

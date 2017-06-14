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
        validatedBy = {CheckValidSortColumn.class}
)
public @interface ValidSortColumn {
  String message() default "Use a sort column available in the data model.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String[] value();
}

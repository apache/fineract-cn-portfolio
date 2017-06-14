package io.mifos.portfolio.api.v1.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
public class CheckValidSortDirection implements ConstraintValidator<ValidSortDirection, String> {
  @Override
  public void initialize(ValidSortDirection constraintAnnotation) {

  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return validate(value);
  }

  public static boolean validate(String value) {
    return (value == null) || (value.equals("ASC") || (value.equals("DESC")));
  }
}

package io.mifos.portfolio.api.v1.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
public class CheckValidSortColumn implements ConstraintValidator<ValidSortColumn, String> {
  private Set<String> allowableColumns;

  @Override
  public void initialize(final ValidSortColumn constraintAnnotation) {
    allowableColumns = new HashSet<>(Arrays.asList(constraintAnnotation.value()));
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return validate(value, allowableColumns);
  }

  public static boolean validate(String value, Set<String> allowableColumns) {
    return value == null || allowableColumns.contains(value);
  }
}
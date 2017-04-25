package io.mifos.portfolio.api.v1.domain;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class InterestRangeTest extends ValidationTest<InterestRange> {
  public InterestRangeTest(ValidationTestCase<InterestRange> testCase) {
    super(testCase);
  }

  @Override
  protected InterestRange createValidTestSubject() {
    return new InterestRange(BigDecimal.valueOf(0.02d).setScale(2, BigDecimal.ROUND_UNNECESSARY),
            BigDecimal.valueOf(0.03d).setScale(2, BigDecimal.ROUND_UNNECESSARY));
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<InterestRange>("basicCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<InterestRange>("5 and 10")
            .adjustment(x -> {
              x.setMinimum(BigDecimal.valueOf(5L).setScale(2, BigDecimal.ROUND_UNNECESSARY));
              x.setMaximum(BigDecimal.valueOf(10L).setScale(2, BigDecimal.ROUND_UNNECESSARY));
            })
            .valid(true));
    ret.add(new ValidationTestCase<InterestRange>("5 and 5")
            .adjustment(x -> {
              x.setMinimum(BigDecimal.valueOf(5L).setScale(2, BigDecimal.ROUND_UNNECESSARY));
              x.setMaximum(BigDecimal.valueOf(5L).setScale(2, BigDecimal.ROUND_UNNECESSARY));
            })
            .valid(true));
    ret.add(new ValidationTestCase<InterestRange>("maxNull")
            .adjustment((x) -> x.setMaximum(null))
            .valid(false));
    ret.add(new ValidationTestCase<InterestRange>("maximim smaller than minimum")
            .adjustment(x -> {
              x.setMinimum(BigDecimal.valueOf(10L).setScale(2, BigDecimal.ROUND_UNNECESSARY));
              x.setMaximum(BigDecimal.valueOf(5L).setScale(2, BigDecimal.ROUND_UNNECESSARY));
            })
            .valid(false));
    return ret;
  }

}
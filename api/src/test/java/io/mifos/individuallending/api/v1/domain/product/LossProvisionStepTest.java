package io.mifos.individuallending.api.v1.domain.product;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

public class LossProvisionStepTest extends ValidationTest<LossProvisionStep> {

  public LossProvisionStepTest(final ValidationTestCase<LossProvisionStep> testCase)
  {
    super(testCase);
  }

  @Override
  protected LossProvisionStep createValidTestSubject() {
    final LossProvisionStep ret = new LossProvisionStep();
    ret.setPercentProvision(BigDecimal.ONE);
    ret.setDaysLate(10);
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<LossProvisionStep>("valid"));
    ret.add(new ValidationTestCase<LossProvisionStep>("largeDaysLate")
        .adjustment(x -> x.setDaysLate(Integer.MAX_VALUE))
        .valid(true));
    ret.add(new ValidationTestCase<LossProvisionStep>("zeroDaysLate")
        .adjustment(x -> x.setDaysLate(0))
        .valid(true));
    ret.add(new ValidationTestCase<LossProvisionStep>("oneDaysLate")
        .adjustment(x -> x.setDaysLate(1))
        .valid(true));
    ret.add(new ValidationTestCase<LossProvisionStep>("negativeDaysLate")
        .adjustment(x -> x.setDaysLate(-1))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionStep>("negativeProvisioning")
        .adjustment(x -> x.setPercentProvision(BigDecimal.TEN.negate()))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionStep>("over100Provisioning")
        .adjustment(x -> x.setPercentProvision(BigDecimal.valueOf(100_01, 2)))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionStep>("exactly100Provisioning")
        .adjustment(x -> x.setPercentProvision(BigDecimal.valueOf(100_00, 2)))
        .valid(true));

    return ret;
  }

}
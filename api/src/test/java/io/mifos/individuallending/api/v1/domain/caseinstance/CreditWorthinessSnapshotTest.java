package io.mifos.individuallending.api.v1.domain.caseinstance;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class CreditWorthinessSnapshotTest extends ValidationTest<CreditWorthinessSnapshot> {
  public CreditWorthinessSnapshotTest(ValidationTestCase<CreditWorthinessSnapshot> testCase) {
    super(testCase);
  }

  @Override
  protected CreditWorthinessSnapshot createValidTestSubject() {
    final CreditWorthinessSnapshot ret = new CreditWorthinessSnapshot();
    ret.setForCustomer("gracie");
    ret.setDebts(Collections.emptyList());
    ret.setAssets(Collections.emptyList());
    ret.setIncomeSources(Collections.emptyList());
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("valid"));
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("nullCustomer")
            .adjustment(x -> x.setForCustomer(null))
            .valid(false));
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("null debts")
            .adjustment(x -> x.setDebts(null))
            .valid(false));
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("null assets")
            .adjustment(x -> x.setAssets(null))
            .valid(false));
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("null income sources")
            .adjustment(x -> x.setIncomeSources(null))
            .valid(false));
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("one valid income sources")
            .adjustment(x -> x.setIncomeSources(Collections.singletonList(CreditWorthinessFactorTest.getValidTestSubject())))
            .valid(true));
    ret.add(new ValidationTestCase<CreditWorthinessSnapshot>("one invalid income sources")
            .adjustment(x -> {
              final CreditWorthinessFactor elt = CreditWorthinessFactorTest.getValidTestSubject();
              elt.setAmount(BigDecimal.valueOf(-1));
              x.setIncomeSources(Collections.singletonList(elt));
            })
            .valid(false));

    return ret;
  }
}

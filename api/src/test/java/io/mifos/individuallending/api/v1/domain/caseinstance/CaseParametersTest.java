/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.individuallending.api.v1.domain.caseinstance;

import io.mifos.Fixture;
import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import io.mifos.portfolio.api.v1.domain.TermRange;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class CaseParametersTest extends ValidationTest<CaseParameters> {

  public CaseParametersTest(final ValidationTestCase<CaseParameters> testCase)
  {
    super(testCase);
  }

  @Override
  protected CaseParameters createValidTestSubject() {
    return Fixture.getTestCaseParameters();
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<CaseParameters>("valid"));
    ret.add(new ValidationTestCase<CaseParameters>("nullTermRange")
            .adjustment(x -> x.setTermRange(null))
            .valid(false));
    ret.add(new ValidationTestCase<CaseParameters>("negativeTermRangeMax")
            .adjustment(x -> x.setTermRange(new TermRange(ChronoUnit.CENTURIES, -1)))
            .valid(false));
    ret.add(new ValidationTestCase<CaseParameters>("nullBalanceRange")
            .adjustment(x -> x.setMaximumBalance(null))
            .valid(false));
    ret.add(new ValidationTestCase<CaseParameters>("tooLargeBalanceRangeScale")
            .adjustment(x -> x.setMaximumBalance(BigDecimal.TEN.setScale(5, BigDecimal.ROUND_FLOOR)))
            .valid(false));
    ret.add(new ValidationTestCase<CaseParameters>("smallerBalanceRangeScale")
            .adjustment(x -> x.setMaximumBalance(BigDecimal.TEN.setScale(3, BigDecimal.ROUND_FLOOR)))
            .valid(true));
    ret.add(new ValidationTestCase<CaseParameters>("invalid payment cycle unit")
            .adjustment(x -> x.getPaymentCycle().setTemporalUnit(ChronoUnit.SECONDS))
            .valid(false));
    ret.add(new ValidationTestCase<CaseParameters>("null CreditWorthinessFactor list")
            .adjustment(x -> x.setCreditWorthinessSnapshots(null))
            .valid(false));

    return ret;
  }
}
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
package io.mifos.portfolio.api.v1.domain;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class ChargeDefinitionTest extends ValidationTest<ChargeDefinition> {
  public ChargeDefinitionTest(ValidationTestCase<ChargeDefinition> testCase) {
    super(testCase);
  }

  @Override
  protected ChargeDefinition createValidTestSubject() {
    final ChargeDefinition ret = new ChargeDefinition();
    ret.setIdentifier("bleblahBlub");
    ret.setName("blubber");
    ret.setDescription("blah");
    ret.setChargeAction(Action.OPEN.name());
    ret.setAmount(BigDecimal.ONE);
    ret.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    ret.setFromAccountDesignator("x1234567898");
    ret.setToAccountDesignator("y1234567898");
    ret.setForCycleSizeUnit(ChronoUnit.YEARS);

    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<ChargeDefinition>("valid")
            .adjustment(x -> x.setForCycleSizeUnit(null))
            .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("nullCycleSizeUnit")
            .adjustment(x -> x.setForCycleSizeUnit(null))
            .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("nullAmount")
            .adjustment(x -> x.setAmount(null))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("amountWithOversizedScale")
            .adjustment(x -> x.setAmount(BigDecimal.ONE.setScale(5, BigDecimal.ROUND_UNNECESSARY)))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("amountWithValidScale")
            .adjustment(x -> x.setAmount(BigDecimal.ONE.setScale(4, BigDecimal.ROUND_UNNECESSARY)))
            .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("nullFromAccountDesignator")
            .adjustment(x -> x.setFromAccountDesignator(null))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("nullToAccountDesignator")
            .adjustment(x -> x.setToAccountDesignator(null))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("accrualAccountDesignatorSetButAccrualActionNot")
            .adjustment(x -> x.setAccrualAccountDesignator("blub"))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("accrualActionSetButAccrualAccountDesignatorNot")
            .adjustment(x -> x.setAccrueAction("blub"))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("accrualActionSetAndAccrualAccountDesignatorSet")
            .adjustment(x -> { x.setAccrueAction("blub"); x.setAccrualAccountDesignator("blub"); })
            .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("nullChargeMethod")
            .adjustment(x -> x.setChargeMethod(null))
            .valid(false));
    return ret;
  }

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.portfolio.api.v1.domain;

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

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
    ret.setProportionalTo("balance");
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
    ret.add(new ValidationTestCase<ChargeDefinition>("invalidProportionalToIdentifier")
            .adjustment(x -> x.setProportionalTo(RandomStringUtils.random(33)))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("missingProportionalToIdentifierOnProportionalCharge")
            .adjustment(x -> x.setProportionalTo(null))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("presentProportionalToIdentifierOnFixedCharge")
            .adjustment(x -> x.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED))
            .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("missingProportionalToIdentifierOnFixedCharge")
        .adjustment(x -> {
          x.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
          x.setProportionalTo(null);
        })
        .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("fixed charge proportional so that segment set can be set.")
        .adjustment(x -> {
          x.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
          x.setProportionalTo("something");
          x.setForSegmentSet("xyz");
          x.setFromSegment("abc");
          x.setToSegment("def");
        })
        .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("proportionalToRunningBalance")
        .adjustment(x -> x.setProportionalTo("{runningbalance}"))
        .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("proportionalToMaximumBalance")
        .adjustment(x -> x.setProportionalTo("{maximumbalance}"))
        .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("segment set set but not list")
        .adjustment(x -> x.setForSegmentSet("xyz"))
        .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("from segment set but not set")
        .adjustment(x -> x.setFromSegment("abc"))
        .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("to segment set but not set")
        .adjustment(x -> x.setFromSegment("def"))
        .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("valid segment references")
        .adjustment(x -> { x.setForSegmentSet("xyz"); x.setFromSegment("abc"); x.setToSegment("def");})
        .valid(true));
    ret.add(new ValidationTestCase<ChargeDefinition>("invalid segment set identifier")
        .adjustment(x -> { x.setForSegmentSet("//"); x.setFromSegment("abc"); x.setToSegment("def");})
        .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("invalid from segment identifier")
        .adjustment(x -> { x.setForSegmentSet("xyz"); x.setFromSegment("//"); x.setToSegment("def");})
        .valid(false));
    ret.add(new ValidationTestCase<ChargeDefinition>("invalid to segment identifier")
        .adjustment(x -> { x.setForSegmentSet("xyz"); x.setFromSegment("abc"); x.setToSegment("//");})
        .valid(false));
    return ret;
  }
}

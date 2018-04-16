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
package org.apache.fineract.cn.individuallending.api.v1.domain.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

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
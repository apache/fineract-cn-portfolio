/*
 * Copyright 2017 Kuelap, Inc.
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
package io.mifos.individuallending.api.v1.domain.product;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LossProvisionConfigurationTest extends ValidationTest<LossProvisionConfiguration> {

  public LossProvisionConfigurationTest(ValidationTestCase<LossProvisionConfiguration> testCase) {
    super(testCase);
  }

  @Override
  protected LossProvisionConfiguration createValidTestSubject() {
    final LossProvisionConfiguration ret = new LossProvisionConfiguration();
    final List<LossProvisionStep> lossProvisionSteps = new ArrayList<>();
    lossProvisionSteps.add(new LossProvisionStep(0, BigDecimal.ONE));
    lossProvisionSteps.add(new LossProvisionStep(1, BigDecimal.valueOf(9)));
    lossProvisionSteps.add(new LossProvisionStep(10, BigDecimal.valueOf(20)));
    lossProvisionSteps.add(new LossProvisionStep(50, BigDecimal.valueOf(70)));
    ret.setLossProvisionSteps(lossProvisionSteps);
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<LossProvisionConfiguration>("valid"));
    ret.add(new ValidationTestCase<LossProvisionConfiguration>("emptyList")
        .adjustment(x -> x.setLossProvisionSteps(Collections.emptyList()))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionConfiguration>("nullList")
        .adjustment(x -> x.setLossProvisionSteps(Collections.emptyList()))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionConfiguration>("sumTooSmall")
        .adjustment(x -> x.getLossProvisionSteps().get(0).setPercentProvision(BigDecimal.valueOf(0.1)))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionConfiguration>("sumTooLarge")
        .adjustment(x -> x.getLossProvisionSteps().get(3).setPercentProvision(BigDecimal.valueOf(71)))
        .valid(false));

    return ret;
  }
}
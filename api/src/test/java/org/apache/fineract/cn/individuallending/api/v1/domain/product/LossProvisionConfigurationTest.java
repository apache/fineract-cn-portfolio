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
import java.util.Collections;
import java.util.List;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

public class LossProvisionConfigurationTest extends ValidationTest<LossProvisionConfiguration> {

  public LossProvisionConfigurationTest(ValidationTestCase<LossProvisionConfiguration> testCase) {
    super(testCase);
  }

  @Override
  protected LossProvisionConfiguration createValidTestSubject() {
    final LossProvisionConfiguration ret = new LossProvisionConfiguration();
    final List<LossProvisionStep> lossProvisionSteps = new ArrayList<>();
    lossProvisionSteps.add(new LossProvisionStep(0, BigDecimal.ONE));
    lossProvisionSteps.add(new LossProvisionStep(1, BigDecimal.valueOf(10)));
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
        .valid(true));
    ret.add(new ValidationTestCase<LossProvisionConfiguration>("nullList")
        .adjustment(x -> x.setLossProvisionSteps(null))
        .valid(false));
    ret.add(new ValidationTestCase<LossProvisionConfiguration>("moreThanOneValuesForOneDay")
        .adjustment(x -> x.getLossProvisionSteps().add(new LossProvisionStep(0, BigDecimal.valueOf(0.1))))
        .valid(false));

    return ret;
  }
}
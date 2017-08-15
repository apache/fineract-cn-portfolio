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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

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
    ret.add(new ValidationTestCase<InterestRange>("too large scale")
            .adjustment(x ->
                    x.setMinimum(x.getMinimum().setScale(3, BigDecimal.ROUND_UNNECESSARY)))
            .valid(false));
    ret.add(new ValidationTestCase<InterestRange>("smaller scale")
            .adjustment(x ->
                    x.setMinimum(x.getMinimum().setScale(1, BigDecimal.ROUND_HALF_EVEN)))
            .valid(true));
    return ret;
  }
}
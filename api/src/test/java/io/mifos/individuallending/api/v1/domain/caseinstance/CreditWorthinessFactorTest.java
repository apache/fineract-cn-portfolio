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

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class CreditWorthinessFactorTest extends ValidationTest<CreditWorthinessFactor> {
  public CreditWorthinessFactorTest(ValidationTestCase<CreditWorthinessFactor> testCase) {
    super(testCase);
  }

  @Override
  protected CreditWorthinessFactor createValidTestSubject() {
    return getValidTestSubject();
  }

  static CreditWorthinessFactor getValidTestSubject() {
    final CreditWorthinessFactor ret = new CreditWorthinessFactor();
    ret.setAmount(BigDecimal.ONE);
    ret.setDescription(null);
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<CreditWorthinessFactor>("valid"));
    ret.add(new ValidationTestCase<CreditWorthinessFactor>("nullDescription")
            .adjustment(x -> x.setDescription(null))
            .valid(true));
    ret.add(new ValidationTestCase<CreditWorthinessFactor>("negative amount")
            .adjustment(x -> x.setAmount(BigDecimal.valueOf(-1)))
            .valid(false));
    ret.add(new ValidationTestCase<CreditWorthinessFactor>("very long description")
            .adjustment(x -> x.setDescription(RandomStringUtils.randomAlphanumeric(4096)))
            .valid(true));
    ret.add(new ValidationTestCase<CreditWorthinessFactor>("too long description")
            .adjustment(x -> x.setDescription(RandomStringUtils.randomAlphanumeric(4097)))
            .valid(false));


    return ret;
  }
}

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
package io.mifos.portfolio.api.v1.domain;

import io.mifos.core.lang.DateConverter;
import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.PROCESSING_FEE_ID;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class ImportParametersTest extends ValidationTest<ImportParameters> {
  public ImportParametersTest(ValidationTestCase<ImportParameters> testCase) {
    super(testCase);
  }

  @Override
  protected ImportParameters createValidTestSubject() {
    final ImportParameters ret = new ImportParameters();
    ret.setCaseAccountAssignments(Collections.emptyList());
    ret.setPaymentSize(BigDecimal.TEN);
    ret.setCurrentBalance(BigDecimal.TEN.multiply(BigDecimal.TEN));
    ret.setStartOfTerm(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
    ret.setCreatedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<ImportParameters>("valid"));
    ret.add(new ValidationTestCase<ImportParameters>("validAccountAssignment")
        .adjustment(x -> x.setCaseAccountAssignments(Collections.singletonList(new AccountAssignment(PROCESSING_FEE_ID, "7534"))))
        .valid(true));
    ret.add(new ValidationTestCase<ImportParameters>("nullPaymentSize")
        .adjustment(x -> x.setPaymentSize(null))
        .valid(false));
    ret.add(new ValidationTestCase<ImportParameters>("nullCurrentBalance")
        .adjustment(x -> x.setCurrentBalance(null))
        .valid(false));
    return ret;
  }

}
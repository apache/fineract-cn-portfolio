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

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.PROCESSING_FEE_ID;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
    ret.setCurrentBalances(Collections.singletonMap(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, BigDecimal.TEN.multiply(BigDecimal.TEN)));
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
        .adjustment(x -> x.setCurrentBalances(null))
        .valid(true));
    return ret;
  }

}
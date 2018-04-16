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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

/**
 * @author Myrle Krantz
 */
public class PaymentCycleTest extends ValidationTest<PaymentCycle> {

  public PaymentCycleTest(ValidationTestCase<PaymentCycle> testCase) {
    super(testCase);
  }

  @Override
  protected PaymentCycle createValidTestSubject() {
    final PaymentCycle ret = new PaymentCycle();

    ret.setPeriod(12);
    ret.setTemporalUnit(ChronoUnit.MONTHS);
    ret.setAlignmentDay(1);
    ret.setAlignmentWeek(null);
    ret.setAlignmentMonth(null);

    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<PaymentCycle>("valid"));
    ret.add(new ValidationTestCase<PaymentCycle>("nullAlignmentDay")
            .adjustment(x -> x.setAlignmentDay(null))
            .valid(true));
    ret.add(new ValidationTestCase<PaymentCycle>("invalidAlignmentWeek")
            .adjustment(x -> x.setAlignmentWeek(5))
            .valid(false));
    ret.add(new ValidationTestCase<PaymentCycle>("nullTemporalUnit")
            .adjustment(x -> x.setTemporalUnit(null))
            .valid(false));
    ret.add(new ValidationTestCase<PaymentCycle>("lastDayOfMonth")
            .adjustment(x -> x.setAlignmentWeek(-1))
            .valid(true));
    return ret;
  }
}

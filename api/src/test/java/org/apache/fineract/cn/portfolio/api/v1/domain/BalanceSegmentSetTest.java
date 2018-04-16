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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

/**
 * @author Myrle Krantz
 */
public class BalanceSegmentSetTest extends ValidationTest<BalanceSegmentSet> {
  public BalanceSegmentSetTest(ValidationTestCase<BalanceSegmentSet> testCase) {
    super(testCase);
  }

  @Override
  protected BalanceSegmentSet createValidTestSubject() {
    final BalanceSegmentSet ret = new BalanceSegmentSet();
    ret.setIdentifier("valid");
    ret.setSegments(Arrays.asList(BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.valueOf(10_000)));
    ret.setSegmentIdentifiers(Arrays.asList("small", "medium", "large"));
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<BalanceSegmentSet>("basicCase")
        .adjustment(x -> {})
        .valid(true));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("null segments")
        .adjustment(x -> x.setSegments(null))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("null identifiers")
        .adjustment(x -> x.setSegmentIdentifiers(null))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("too short identifier list")
        .adjustment(x -> x.setSegmentIdentifiers(Arrays.asList("small", "large")))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("too short segment list")
        .adjustment(x -> x.setSegments(Arrays.asList(BigDecimal.ZERO, BigDecimal.valueOf(100))))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("non-zero first entry")
        .adjustment(x -> x.setSegments(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(100), BigDecimal.valueOf(10_000))))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("mis-ordered segmentation")
        .adjustment(x -> x.setSegments(Arrays.asList(BigDecimal.ZERO, BigDecimal.valueOf(10_000), BigDecimal.valueOf(100))))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("invalid identifier")
        .adjustment(x -> x.setIdentifier("//"))
        .valid(false));
    ret.add(new ValidationTestCase<BalanceSegmentSet>("invalid segment identifier")
        .adjustment(x -> x.setSegmentIdentifiers(Arrays.asList("small", "large", "//")))
        .valid(false));
    return ret;
  }
}
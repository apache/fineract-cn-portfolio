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
package org.apache.fineract.cn.individuallending.internal.service.schedule;

import org.apache.fineract.cn.individuallending.internal.service.ChargeDefinitionService;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class ScheduledChargesServiceTest {

  private static final String PRODUCT_IDENTIFIER = "a";
  private static final String SEGMENT_SET_IDENTIFIER = "b";

  static class TestCase {
    final String description;
    List<BalanceSegmentEntity> balanceSegmentEntities = Collections.emptyList();
    String fromSegment = null;
    String toSegment = null;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<ChargeRange> expectedResult = Optional.empty();

    private TestCase(String description) {
      this.description = description;
    }

    TestCase balanceSegmentEntities(List<BalanceSegmentEntity> balanceSegmentEntities) {
      this.balanceSegmentEntities = balanceSegmentEntities;
      return this;
    }

    TestCase fromSegment(String fromSegment) {
      this.fromSegment = fromSegment;
      return this;
    }

    TestCase toSegment(String toSegment) {
      this.toSegment = toSegment;
      return this;
    }

    TestCase expectedResult(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ChargeRange> expectedResult) {
      this.expectedResult = expectedResult;
      return this;
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ScheduledChargesServiceTest.TestCase> ret = new ArrayList<>();
    ret.add(new ScheduledChargesServiceTest.TestCase("simple"));
    ret.add(new TestCase("two segments, one referenced")
        .balanceSegmentEntities(Arrays.asList(
            new BalanceSegmentEntity(null, SEGMENT_SET_IDENTIFIER, "first", BigDecimal.ZERO),
            new BalanceSegmentEntity(null, SEGMENT_SET_IDENTIFIER, "second", BigDecimal.TEN)))
        .fromSegment("first")
        .toSegment("first")
        .expectedResult(Optional.of(new ChargeRange(BigDecimal.ZERO, Optional.of(BigDecimal.TEN))))
    );
    ret.add(new TestCase("two segments, both referenced")
        .balanceSegmentEntities(Arrays.asList(
            new BalanceSegmentEntity(null, SEGMENT_SET_IDENTIFIER, "lower", BigDecimal.ZERO),
            new BalanceSegmentEntity(null, SEGMENT_SET_IDENTIFIER, "higher", BigDecimal.TEN)))
        .fromSegment("lower")
        .toSegment("higher")
        .expectedResult(Optional.of(new ChargeRange(BigDecimal.ZERO, Optional.empty())))
    );
    ret.add(new TestCase("two segments, one mis-referenced")
        .balanceSegmentEntities(Arrays.asList(
            new BalanceSegmentEntity(null, SEGMENT_SET_IDENTIFIER, "first", BigDecimal.ZERO),
            new BalanceSegmentEntity(null, SEGMENT_SET_IDENTIFIER, "second", BigDecimal.TEN)))
        .fromSegment("first")
        .toSegment("second2")
        .expectedResult(Optional.empty())
    );
    return ret;
  }

  private final ScheduledChargesServiceTest.TestCase testCase;

  public ScheduledChargesServiceTest(final ScheduledChargesServiceTest.TestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void findChargeRange() throws Exception {

    final ChargeDefinitionService chargeDefinitionServiceMock = Mockito.mock(ChargeDefinitionService.class);
    final BalanceSegmentRepository balanceSegmentRepositoryMock = Mockito.mock(BalanceSegmentRepository.class);

    Mockito.doReturn(testCase.balanceSegmentEntities.stream())
        .when(balanceSegmentRepositoryMock)
        .findByProductIdentifierAndSegmentSetIdentifier(PRODUCT_IDENTIFIER, SEGMENT_SET_IDENTIFIER);

    final ScheduledChargesService testSubject = new ScheduledChargesService(chargeDefinitionServiceMock, balanceSegmentRepositoryMock);
    final ChargeDefinition chargeDefinition = new ChargeDefinition();

    chargeDefinition.setForSegmentSet(SEGMENT_SET_IDENTIFIER);
    chargeDefinition.setFromSegment(testCase.fromSegment);
    chargeDefinition.setToSegment(testCase.toSegment);
    final Optional<ChargeRange> result = testSubject.findChargeRange(PRODUCT_IDENTIFIER, chargeDefinition);
    Assert.assertEquals(testCase.expectedResult, result);
  }
}
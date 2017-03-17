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
package io.mifos.individuallending.internal.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class RateCollectorsTest {
  final static private BigDecimal pointOne = BigDecimal.valueOf(0.1);
  private static class TestCase {
    private final String description;
    private Collection<BigDecimal> values;
    private BigDecimal expectedCompound;
    private BigDecimal expectedGeometricMean;
    private int significantDigits;

    TestCase(final String description) {
      this.description = description;
    }

    TestCase values(final Collection<BigDecimal> newVal) {
      values = newVal;
      return this;
    }

    TestCase compound(final BigDecimal newVal) {
      expectedCompound = newVal;
      return this;
    }

    TestCase geometricMean(final BigDecimal newVal) {
      expectedGeometricMean = newVal;
      return this;
    }

    TestCase significantDigits(final int newVal) {
      significantDigits = newVal;
      return this;
    }

    @Override
    public String toString() {
      return "TestCase{" +
              "description='" + description + '\'' +
              '}';
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<TestCase> ret = new ArrayList<>();
    ret.add(baseCase());
    ret.add(lotsaPointOnesCase());
    ret.add(slightlyMoreComplexCase());
    return ret;
  }

  private static TestCase baseCase() {
    return new TestCase("baseCase")
            .values(Collections.singletonList(pointOne))
            .significantDigits(1)
            .compound(pointOne)
            .geometricMean(pointOne);
  }

  private static TestCase lotsaPointOnesCase() {
    return new TestCase("lotsaPointOnesCase")
            .values(Stream
                    .iterate(pointOne, x -> pointOne)
                    .limit(10).collect(Collectors.toList()))
            .significantDigits(3)
            .compound(BigDecimal.valueOf(1.594))
            .geometricMean(pointOne.setScale(3, BigDecimal.ROUND_HALF_EVEN));
  }

  private static TestCase slightlyMoreComplexCase() {
    return new TestCase("slightlyMoreComplexCase")
            .values(Arrays.asList(BigDecimal.valueOf(0.2), BigDecimal.valueOf(0.06), BigDecimal.valueOf(0.01)))
            .significantDigits(4)
            .compound(BigDecimal.valueOf(0.2847))
            .geometricMean(BigDecimal.valueOf(0.0871));
  }

  private final TestCase testCase;

  public RateCollectorsTest(final TestCase testCase)
  {
    this.testCase = testCase;
  }


  @Test
  public void geometricMean() throws Exception {
    Assert.assertEquals(testCase.expectedGeometricMean, testCase.values.stream().collect(RateCollectors.geometricMean(testCase.significantDigits)));
  }


  @Test
  public void compound()
  {
    Assert.assertEquals(testCase.expectedCompound, testCase.values.stream().collect(RateCollectors.compound(testCase.significantDigits)));
  }

  @Test
  public void compoundAndGeometricMeanCompatible()
  {
    final BigDecimal geometricMean = testCase.values.stream()
            .collect(RateCollectors.geometricMean(testCase.significantDigits));

    final BigDecimal compoundingOfGeometricMean = Stream
            .iterate(geometricMean, x -> geometricMean)
            .limit(testCase.values.size())
            .collect(RateCollectors.compound(testCase.significantDigits));

    Assert.assertEquals(testCase.expectedCompound, compoundingOfGeometricMean);
  }

  @Test
  public void compoundViaParalletStream()
  {
    Assert.assertEquals(testCase.expectedCompound, testCase.values.parallelStream().collect(RateCollectors.compound(testCase.significantDigits)));
  }

  @Test
  public void geometricMeanViaParalletStream()
  {
    Assert.assertEquals(testCase.expectedGeometricMean, testCase.values.parallelStream().collect(RateCollectors.geometricMean(testCase.significantDigits)));
  }
}

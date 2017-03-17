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

import java.math.BigDecimal;
import java.util.stream.Collector;

/**
 * @author Myrle Krantz
 */
final class RateCollectors {

  private RateCollectors() {}

  static Collector<BigDecimal, ?, BigDecimal> compound(int significantDigits)
  {
    return Collector.of(
            () -> new Compound(significantDigits),
            Compound::accumulate,
            Compound::combine,
            Compound::finish);
  }

  static Collector<BigDecimal, ?, BigDecimal> geometricMean(int significantDigits)
  {
    return Collector.of(
            () -> new GeometricMean(significantDigits),
            GeometricMean::accumulate,
            GeometricMean::combine,
            GeometricMean::finish);
  }

  private static class Compound
  {
    private final int significantDigits;
    BigDecimal rate;

    Compound(final int significantDigits)
    {
      this.significantDigits = significantDigits;
      rate = BigDecimal.ONE;
    }

    void accumulate(final BigDecimal newRate)
    {
      rate = rate.multiply(BigDecimal.ONE.add(newRate));
    }

    Compound combine(final Compound other)
    {
      this.rate = this.rate.multiply(other.rate);
      return this;
    }

    BigDecimal finish()
    {
      return rate.subtract(BigDecimal.ONE).setScale(significantDigits, BigDecimal.ROUND_HALF_EVEN);
    }
  }

  private static class GeometricMean
  {
    private final int significantDigits;
    BigDecimal rate;
    long rateCount;

    GeometricMean(int significantDigits)
    {
      this.significantDigits = significantDigits;
      rate = BigDecimal.ONE;
    }

    void accumulate(final BigDecimal newRate)
    {
      rate = rate.multiply(BigDecimal.ONE.add(newRate));
      rateCount++;
    }

    GeometricMean combine(final GeometricMean other)
    {
      rate = rate.multiply(other.rate);
      rateCount += other.rateCount;
      return this;
    }

    BigDecimal finish()
    {
      if (rateCount == 0)
        return BigDecimal.ZERO.setScale(significantDigits, BigDecimal.ROUND_UNNECESSARY);

      final double root = Math.pow(rate.doubleValue(), 1d / rateCount);

      return BigDecimal.valueOf(root)
              .subtract(BigDecimal.ONE)
              .setScale(significantDigits, BigDecimal.ROUND_HALF_EVEN);
    }
  }
}

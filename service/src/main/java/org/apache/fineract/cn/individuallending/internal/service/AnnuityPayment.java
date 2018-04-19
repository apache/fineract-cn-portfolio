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
package org.apache.fineract.cn.individuallending.internal.service;


import java.math.MathContext;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public final class AnnuityPayment {
  private BigDecimal rate;
  private int periods;

  /**
   * Private constructor.
   *
   * @param rate    the target rate, not null.
   * @param periods the periods, >= 0.
   */
  private AnnuityPayment(final @Nonnull BigDecimal rate, final @Nonnegative int periods)
  {
    this.rate = Objects.requireNonNull(rate);
    if (periods < 0) {
      throw new IllegalArgumentException("Periods < 0");
    }
    this.periods = periods;
  }

  public static AnnuityPayment of(final @Nonnull BigDecimal rate, final @Nonnegative int periods)
  {
    return new AnnuityPayment(rate, periods);
  }

  public static BigDecimal calculate(
      final @Nonnull BigDecimal amount,
      final @Nonnull BigDecimal rate,
      final @Nonnegative int periods,
      final @Nonnegative int precision)
  {
    Objects.requireNonNull(amount, "Amount required");
    Objects.requireNonNull(rate, "Rate required");
    if (rate.compareTo(BigDecimal.ZERO) == 0)
      return amount.divide(BigDecimal.valueOf(periods), precision, BigDecimal.ROUND_HALF_EVEN);

    // AP(m) = m*r / [ (1-((1 + r).pow(-n))) ]

    return amount.multiply(rate).divide(
            BigDecimal.ONE.subtract((BigDecimal.ONE.add(rate)
                    .pow(-1 * periods, MathContext.DECIMAL64))),
        precision, BigDecimal.ROUND_HALF_EVEN);
  }

  public BigDecimal apply(
      final @Nonnull BigDecimal amount,
      final @Nonnegative int precision)
  {
    return calculate(amount, rate, periods, precision);
  }

  @Override
  public String toString() {
    return "AnnuityPayment{" +
            "rate=" + rate +
            ", periods=" + periods +
            '}';
  }
}

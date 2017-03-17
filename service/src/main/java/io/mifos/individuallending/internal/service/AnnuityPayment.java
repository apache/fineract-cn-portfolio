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

import org.javamoney.calc.CalculationContext;
import org.javamoney.calc.common.Rate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.money.MonetaryAmount;
import javax.money.MonetaryOperator;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
final class AnnuityPayment implements MonetaryOperator {
  private Rate rate;
  private int periods;

  /**
   * Private constructor.
   *
   * @param rate    the target rate, not null.
   * @param periods the periods, >= 0.
   */
  private AnnuityPayment(final @Nonnull Rate rate, final @Nonnegative int periods)
  {
    this.rate = Objects.requireNonNull(rate);
    if (periods < 0) {
      throw new IllegalArgumentException("Periods < 0");
    }
    this.periods = periods;
  }

  public static AnnuityPayment of(final @Nonnull Rate rate, final @Nonnegative int periods)
  {
    return new AnnuityPayment(rate, periods);
  }

  static MonetaryAmount calculate(
          final @Nonnull MonetaryAmount amount,
          final @Nonnull Rate rate,
          final @Nonnegative int periods)
  {
    Objects.requireNonNull(amount, "Amount required");
    Objects.requireNonNull(rate, "Rate required");
    // AP(m) = m*r / [ (1-((1 + r).pow(-n))) ]

    return amount.multiply(rate.get()).divide(
            BigDecimal.ONE.subtract((BigDecimal.ONE.add(rate.get())
                    .pow(-1 * periods, CalculationContext.mathContext()))));
  }

  @Override
  public MonetaryAmount apply(final @Nonnull MonetaryAmount amount) {
    return calculate(amount, rate, periods);
  }

  @Override
  public String toString() {
    return "AnnuityPayment{" +
            "rate=" + rate +
            ", periods=" + periods +
            '}';
  }
}

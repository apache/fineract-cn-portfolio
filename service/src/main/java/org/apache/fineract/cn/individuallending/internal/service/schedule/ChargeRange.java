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

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class ChargeRange {
  final private BigDecimal from;
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  final private Optional<BigDecimal> to;

  ChargeRange(
      final BigDecimal from,
      @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<BigDecimal> to) {
    this.from = from;
    this.to = to;
  }

  public boolean amountIsWithinRange(final BigDecimal amountProportionalTo) {
    return to.map(bigDecimal -> from.compareTo(amountProportionalTo) <= 0 &&
        bigDecimal.compareTo(amountProportionalTo) > 0)
        .orElseGet(() -> from.compareTo(amountProportionalTo) <= 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChargeRange that = (ChargeRange) o;
    return Objects.equals(from, that.from) &&
        Objects.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to);
  }

  @Override
  public String toString() {
    return "ChargeRange{" +
        "from=" + from +
        ", to=" + to +
        '}';
  }
}
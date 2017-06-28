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
package io.mifos.portfolio.service.internal.util;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public class ChargeInstance {
  private final String fromAccount;
  private final String toAccount;
  private final BigDecimal amount;

  public ChargeInstance(final String fromAccount,
                        final String toAccount,
                        final BigDecimal amount) {
    this.fromAccount = fromAccount;
    this.toAccount = toAccount;
    this.amount = amount;
  }

  public String getFromAccount() {
    return fromAccount;
  }

  public String getToAccount() {
    return toAccount;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChargeInstance that = (ChargeInstance) o;
    return Objects.equals(fromAccount, that.fromAccount) &&
            Objects.equals(toAccount, that.toAccount) &&
            Objects.equals(amount, that.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromAccount, toAccount, amount);
  }

  @Override
  public String toString() {
    return "ChargeInstance{" +
            "fromAccount='" + fromAccount + '\'' +
            ", toAccount='" + toAccount + '\'' +
            ", amount=" + amount +
            '}';
  }
}

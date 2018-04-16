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

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Payment {
  private List<CostComponent> costComponents;
  private Map<String, BigDecimal> balanceAdjustments;
  private @Nullable String date;

  public Payment() {
  }

  public Payment(List<CostComponent> costComponents, Map<String, BigDecimal> balanceAdjustments) {
    this.costComponents = costComponents;
    this.balanceAdjustments = balanceAdjustments;
  }

  public List<CostComponent> getCostComponents() {
    return costComponents;
  }

  public void setCostComponents(List<CostComponent> costComponents) {
    this.costComponents = costComponents;
  }

  public Map<String, BigDecimal> getBalanceAdjustments() {
    return balanceAdjustments;
  }

  public void setBalanceAdjustments(Map<String, BigDecimal> balanceAdjustments) {
    this.balanceAdjustments = balanceAdjustments;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Payment that = (Payment) o;
    return Objects.equals(costComponents, that.costComponents) &&
            Objects.equals(balanceAdjustments, that.balanceAdjustments) &&
            Objects.equals(date, that.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(costComponents, balanceAdjustments, date);
  }

  @Override
  public String toString() {
    return "Payment{" +
        "costComponents=" + costComponents +
        ", balanceAdjustments=" + balanceAdjustments +
        ", date='" + date + '\'' +
        '}';
  }
}

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
package org.apache.fineract.cn.individuallending.api.v1.domain.product;

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public class LossProvisionStep {
  @Range(min = 0)
  private int daysLate;

  @DecimalMin(value = "0.00")
  @DecimalMax(value = "100.00")
  @NotNull
  private BigDecimal percentProvision;

  public LossProvisionStep() {
  }

  public LossProvisionStep(int daysLate, BigDecimal percentProvision) {
    this.daysLate = daysLate;
    this.percentProvision = percentProvision;
  }

  public int getDaysLate() {
    return daysLate;
  }

  public void setDaysLate(int daysLate) {
    this.daysLate = daysLate;
  }

  public BigDecimal getPercentProvision() {
    return percentProvision;
  }

  public void setPercentProvision(BigDecimal percentProvision) {
    this.percentProvision = percentProvision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LossProvisionStep that = (LossProvisionStep) o;
    return daysLate == that.daysLate &&
        Objects.equals(percentProvision, that.percentProvision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(daysLate, percentProvision);
  }

  @Override
  public String toString() {
    return "LossProvisionStep{" +
        "daysLate=" + daysLate +
        ", percentProvision=" + percentProvision +
        '}';
  }
}

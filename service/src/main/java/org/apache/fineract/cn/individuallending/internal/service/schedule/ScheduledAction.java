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

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public class ScheduledAction {
  private final Action action;
  private final LocalDate when;
  private final @Nullable Period actionPeriod;
  private final @Nullable Period repaymentPeriod;

  public ScheduledAction(
      @Nonnull final Action action,
      @Nonnull final LocalDate when,
      @Nonnull final Period actionPeriod,
      @Nonnull final Period repaymentPeriod) {
    this.action = action;
    this.when = when;
    this.actionPeriod = actionPeriod;
    this.repaymentPeriod = repaymentPeriod;
  }

  public ScheduledAction(
      @Nonnull final Action action,
      @Nonnull final LocalDate when,
      @Nonnull final Period actionPeriod) {
    this.action = action;
    this.when = when;
    this.actionPeriod = actionPeriod;
    this.repaymentPeriod = null;
  }

  public ScheduledAction(
      @Nonnull final Action action,
      @Nonnull final LocalDate when) {
    this.action = action;
    this.when = when;
    this.actionPeriod = null;
    this.repaymentPeriod = null;
  }

  boolean actionIsOnOrAfter(final LocalDate date) {
    return when.compareTo(date) >= 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScheduledAction that = (ScheduledAction) o;
    return action == that.action &&
            Objects.equals(when, that.when) &&
            Objects.equals(actionPeriod, that.actionPeriod) &&
            Objects.equals(repaymentPeriod, that.repaymentPeriod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, when, actionPeriod, repaymentPeriod);
  }

  @Override
  public String toString() {
    return "ScheduledAction{" +
            "action=" + action +
            ", when=" + when +
            ", actionPeriod=" + actionPeriod +
            ", repaymentPeriod=" + repaymentPeriod +
            '}';
  }

  @Nullable
  public Period getActionPeriod() {
    return actionPeriod;
  }

  public Action getAction() {
    return action;
  }

  @Nullable
  public Period getRepaymentPeriod() {
    return repaymentPeriod;
  }

  public LocalDate getWhen() {
    return when;
  }
}

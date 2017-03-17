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

import io.mifos.individuallending.api.v1.domain.workflow.Action;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
class ScheduledAction {
  final Action action;
  final LocalDate when;
  final Period actionPeriod;
  final Period repaymentPeriod;

  ScheduledAction(@Nonnull final Action action,
                  @Nonnull final LocalDate when,
                  @Nonnull final Period actionPeriod,
                  @Nonnull final Period repaymentPeriod) {
    this.action = action;
    this.when = when;
    this.actionPeriod = actionPeriod;
    this.repaymentPeriod = repaymentPeriod;
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
}

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

import io.mifos.portfolio.api.v1.domain.ChargeDefinition;

import javax.annotation.Nonnull;

/**
 * @author Myrle Krantz
 */
class ScheduledCharge {
  private final ScheduledAction scheduledAction;
  private final ChargeDefinition chargeDefinition;

  ScheduledCharge(@Nonnull final ScheduledAction scheduledAction, @Nonnull final ChargeDefinition chargeDefinition) {
    this.scheduledAction = scheduledAction;
    this.chargeDefinition = chargeDefinition;
  }

  ScheduledAction getScheduledAction() {
    return scheduledAction;
  }

  ChargeDefinition getChargeDefinition() {
    return chargeDefinition;
  }

  @Override
  public String toString() {
    return "ScheduledCharge{" +
            "scheduledAction=" + scheduledAction +
            ", chargeDefinition=" + chargeDefinition +
            '}';
  }
}

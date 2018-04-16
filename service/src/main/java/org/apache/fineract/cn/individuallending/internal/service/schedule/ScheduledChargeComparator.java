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


import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;

import java.util.Comparator;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class ScheduledChargeComparator implements Comparator<ScheduledCharge>
{
  @Override
  public int compare(ScheduledCharge o1, ScheduledCharge o2) {
    return compareScheduledCharges(o1, o2);
  }

  static int compareScheduledCharges(ScheduledCharge o1, ScheduledCharge o2) {
    int ret = o1.getScheduledAction().getWhen().compareTo(o2.getScheduledAction().getWhen());
    if (ret != 0)
      return ret;

    ret = o1.getScheduledAction().getAction().compareTo(o2.getScheduledAction().getAction());
    if (ret != 0)
      return ret;

    ret = proportionalityApplicationOrder(o1.getChargeDefinition(), o2.getChargeDefinition());
    if (ret != 0)
      return ret;

    return o1.getChargeDefinition().getIdentifier().compareTo(o2.getChargeDefinition().getIdentifier());
  }

  static int proportionalityApplicationOrder(final ChargeDefinition o1, final ChargeDefinition o2) {
    final Optional<ChargeProportionalDesignator> aProportionalToDesignator
        = ChargeProportionalDesignator.fromString(o1.getProportionalTo());
    final Optional<ChargeProportionalDesignator> bProportionalToDesignator
        = ChargeProportionalDesignator.fromString(o2.getProportionalTo());

    if (aProportionalToDesignator.isPresent() && bProportionalToDesignator.isPresent())
      return Integer.compare(
          aProportionalToDesignator.get().getOrderOfApplication(),
          bProportionalToDesignator.get().getOrderOfApplication());
    else if (aProportionalToDesignator.isPresent())
      return 1;
    else if (bProportionalToDesignator.isPresent())
      return -1;
    else
      return 0;
  }
}
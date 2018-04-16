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
package org.apache.fineract.cn.individuallending.internal.mapper;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionStep;
import org.apache.fineract.cn.individuallending.internal.repository.LossProvisionStepEntity;

import java.math.BigDecimal;

/**
 * @author Myrle Krantz
 */
public interface LossProvisionStepMapper {
  static LossProvisionStepEntity map(
      final Long productId,
      final LossProvisionStep instance) {
    final LossProvisionStepEntity ret = new LossProvisionStepEntity();
    ret.setProductId(productId);
    ret.setDaysLate(instance.getDaysLate());
    ret.setPercentProvision(instance.getPercentProvision().setScale(2, BigDecimal.ROUND_HALF_EVEN));
    return ret;
  }

  static LossProvisionStep map(
      final LossProvisionStepEntity entity) {
    final LossProvisionStep ret = new LossProvisionStep();
    ret.setDaysLate(entity.getDaysLate());
    ret.setPercentProvision(entity.getPercentProvision());
    return ret;
  }
}

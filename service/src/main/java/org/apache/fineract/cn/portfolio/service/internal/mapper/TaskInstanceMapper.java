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
package org.apache.fineract.cn.portfolio.service.internal.mapper;

import org.apache.fineract.cn.portfolio.api.v1.domain.TaskInstance;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceEntity;
import org.apache.fineract.cn.lang.DateConverter;

/**
 * @author Myrle Krantz
 */
public interface TaskInstanceMapper {
  static TaskInstance map(final TaskInstanceEntity from) {
    final TaskInstance ret = new TaskInstance();

    ret.setTaskIdentifier(from.getTaskDefinition().getIdentifier());
    ret.setComment(from.getComment());
    ret.setExecutedBy(from.getExecutedBy());
    ret.setExecutedOn(from.getExecutedOn() == null ? null : DateConverter.toIsoString(from.getExecutedOn()));

    return ret;
  }

  static TaskInstanceEntity mapOverOldEntity(final TaskInstance from, final TaskInstanceEntity oldEntity) {
    final TaskInstanceEntity ret = new TaskInstanceEntity();
    ret.setComment(from.getComment());

    ret.setId(oldEntity.getId());
    ret.setCustomerCase(oldEntity.getCustomerCase());
    ret.setTaskDefinition(oldEntity.getTaskDefinition());
    ret.setExecutedBy(oldEntity.getExecutedBy());
    ret.setExecutedOn(oldEntity.getExecutedOn());
    return ret;
  }
}

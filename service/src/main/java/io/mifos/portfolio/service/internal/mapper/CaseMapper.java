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
package io.mifos.portfolio.service.internal.mapper;

import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.service.internal.repository.CaseAccountAssignmentEntity;
import io.mifos.portfolio.service.internal.repository.CaseEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
public class CaseMapper {
  public static Case map(final CaseEntity instance, final String parameters) {
    final Case ret = new Case();

    ret.setIdentifier(instance.getIdentifier());
    ret.setProductIdentifier(instance.getProductIdentifier());
    ret.setParameters(parameters);
    ret.setAccountAssignments(instance.getAccountAssignments().stream().map(CaseMapper::mapAccountAssignmentEntity).collect(Collectors.toSet()));
    ret.setCurrentState(instance.getCurrentState());
    ret.setCreatedOn(DateConverter.toIsoString(instance.getCreatedOn()));
    ret.setCreatedBy(instance.getCreatedBy());
    ret.setLastModifiedBy(instance.getLastModifiedBy());
    ret.setLastModifiedOn(DateConverter.toIsoString(instance.getLastModifiedOn()));

    return ret;
  }

  public static AccountAssignment mapAccountAssignmentEntity(final CaseAccountAssignmentEntity instance) {
    final AccountAssignment ret = new AccountAssignment();

    ret.setDesignator(instance.getDesignator());
    ret.setAccountIdentifier(instance.getIdentifier());

    return ret;
  }

  public static CaseEntity map(final Case instance) {
    final CaseEntity ret = new CaseEntity();

    ret.setIdentifier(instance.getIdentifier());
    ret.setProductIdentifier(instance.getProductIdentifier());
    ret.setAccountAssignments(instance.getAccountAssignments().stream()
            .map(x -> CaseMapper.map(x, ret))
            .collect(Collectors.toSet()));
    ret.setCurrentState(instance.getCurrentState());

    final LocalDateTime time = LocalDateTime.now(Clock.systemUTC());
    final String user = UserContextHolder.checkedGetUser();
    ret.setCreatedOn(time);
    ret.setCreatedBy(user);
    ret.setLastModifiedOn(time);
    ret.setLastModifiedBy(user);

    return ret;
  }

  public static CaseAccountAssignmentEntity map(final AccountAssignment instance, final CaseEntity caseInstance) {
    final CaseAccountAssignmentEntity ret = new CaseAccountAssignmentEntity();

    ret.setCaseEntity(caseInstance);
    ret.setDesignator(instance.getDesignator());
    ret.setIdentifier(instance.getAccountIdentifier());

    return ret;
  }

  public static CaseEntity mapOverOldEntity(final Case instance, final CaseEntity oldEntity) {
    final CaseEntity newEntity = map(instance);

    newEntity.setId(oldEntity.getId());
    newEntity.setCreatedBy(oldEntity.getCreatedBy());
    newEntity.setCreatedOn(oldEntity.getCreatedOn());

    final Set<CaseAccountAssignmentEntity> oldAccountAssignmentEntities = oldEntity.getAccountAssignments();
    final Map<String, CaseAccountAssignmentEntity> accountAssignmentsMap
            = oldAccountAssignmentEntities.stream()
            .collect(Collectors.toMap(CaseAccountAssignmentEntity::getDesignator, x -> x));

    final Set<AccountAssignment> newAccountAssignments = instance.getAccountAssignments();
    final Set<CaseAccountAssignmentEntity> newAccountAssignmentEntities =
            newAccountAssignments.stream().map(x -> {
              final CaseAccountAssignmentEntity newAccountAssignmentEntity = CaseMapper.map(x, newEntity);
              final CaseAccountAssignmentEntity oldAccountAssignmentEntity = accountAssignmentsMap.get(x.getDesignator());
              if (oldAccountAssignmentEntity != null) newAccountAssignmentEntity.setId(oldAccountAssignmentEntity.getId());

              return newAccountAssignmentEntity;
            }).collect(Collectors.toSet());


    newEntity.setAccountAssignments(newAccountAssignmentEntities);
    return newEntity;
  }
}
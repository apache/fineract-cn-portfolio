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
package io.mifos.portfolio.service.internal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, Long> {
  Optional<CaseEntity> findByProductIdentifierAndIdentifier(String productIdentifier, String identifier);
  Page<CaseEntity> findByProductIdentifierAndCurrentStateIn(String productIdentifier, Collection<String> currentStates, Pageable pageRequest);

  //TODO: It should be possible to delete the @Query once we've updated to spring-data-release train ingalls.
  @Query("SELECT COUNT(t) > 0  FROM CaseEntity t WHERE t.productIdentifier = :productIdentifier")
  boolean existsByProductIdentifier(@Param("productIdentifier") String productIdentifier);

}

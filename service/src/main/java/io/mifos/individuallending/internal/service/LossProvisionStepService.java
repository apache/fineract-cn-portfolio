/*
 * Copyright 2017 Kuelap, Inc.
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

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.product.LossProvisionStep;
import io.mifos.individuallending.internal.mapper.LossProvisionStepMapper;
import io.mifos.individuallending.internal.repository.LossProvisionStepRepository;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class LossProvisionStepService {
  private final ProductRepository productRepository;
  private final LossProvisionStepRepository lossProvisionStepRepository;

  @Autowired
  public LossProvisionStepService(
      final ProductRepository productRepository,
      final LossProvisionStepRepository lossProvisionStepRepository) {
    this.productRepository = productRepository;
    this.lossProvisionStepRepository = lossProvisionStepRepository;
  }

  public Optional<LossProvisionStep> findByProductIdAndDaysLate(
      final Long id,
      final int daysLate) {
    return lossProvisionStepRepository.findByProductIdAndDaysLate(id, daysLate).map(LossProvisionStepMapper::map);
  }

  public List<LossProvisionStep> findByProductIdentifier(
      final String productIdentifier) {
    final Long productId = productRepository.findByIdentifier(productIdentifier)
        .orElseThrow(() -> ServiceException.notFound("Product ''{}'' doesn''t exist.", productIdentifier))
        .getId();
    return lossProvisionStepRepository.findByProductIdOrderByDaysLateAsc(productId)
        .map(LossProvisionStepMapper::map)
        .collect(Collectors.toList());
  }
}
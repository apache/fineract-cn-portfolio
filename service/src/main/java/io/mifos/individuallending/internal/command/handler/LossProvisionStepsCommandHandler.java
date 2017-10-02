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
package io.mifos.individuallending.internal.command.handler;

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.individuallending.internal.command.ChangeLossProvisionSteps;
import io.mifos.individuallending.internal.mapper.LossProvisionStepMapper;
import io.mifos.individuallending.internal.repository.LossProvisionStepEntity;
import io.mifos.individuallending.internal.repository.LossProvisionStepRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;


/**
 * @author Myrle Krantz
 */
@Aggregate
public class LossProvisionStepsCommandHandler {
  private final LossProvisionStepRepository lossProvisionStepRepository;
  private final ProductRepository productRepository;

  @Autowired
  public LossProvisionStepsCommandHandler(
      final LossProvisionStepRepository lossProvisionStepRepository,
      final ProductRepository productRepository) {
    this.lossProvisionStepRepository = lossProvisionStepRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.PUT_LOSS_PROVISION_STEPS)
  public String process(final ChangeLossProvisionSteps command) {
    final ProductEntity productEntity = productRepository.findByIdentifier(command.getProductIdentifier())
        .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", command.getProductIdentifier()));
    final Stream<LossProvisionStepEntity> lossProvisionSteps = lossProvisionStepRepository.findByProductId(productEntity.getId());
    lossProvisionSteps.forEach(lossProvisionStepRepository::delete);
    command.getLossProvisionConfiguration().getLossProvisionSteps().stream()
        .map(lossProvisionStep -> LossProvisionStepMapper.map(productEntity.getId(), lossProvisionStep))
        .forEach(lossProvisionStepRepository::save);

    return command.getProductIdentifier();
  }
}
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
package io.mifos.portfolio.service.internal.command.handler;

import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.command.ChangeChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.command.CreateChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.command.DeleteProductChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.mapper.ChargeDefinitionMapper;
import io.mifos.portfolio.service.internal.repository.ChargeDefinitionEntity;
import io.mifos.portfolio.service.internal.repository.ChargeDefinitionRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@Aggregate
public class ChargeDefinitionCommandHandler {
  private final ProductRepository productRepository;
  private final ChargeDefinitionRepository chargeDefinitionRepository;

  @Autowired
  public ChargeDefinitionCommandHandler(
          final ProductRepository productRepository,
          final ChargeDefinitionRepository chargeDefinitionRepository) {
    this.productRepository = productRepository;
    this.chargeDefinitionRepository = chargeDefinitionRepository;
  }

  @SuppressWarnings("unused")
  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_CHARGE_DEFINITION)
  public ChargeDefinitionEvent process(final CreateChargeDefinitionCommand command) {
    final ChargeDefinition chargeDefinition = command.getInstance();
    final String productIdentifier = command.getProductIdentifier();

    final ProductEntity productEntity
            = productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.badRequest("The given product identifier does not refer to a product {0}", productIdentifier));

    final ChargeDefinitionEntity chargeDefinitionEntity =
            ChargeDefinitionMapper.map(productEntity, chargeDefinition);
    chargeDefinitionRepository.save(chargeDefinitionEntity);

    return new ChargeDefinitionEvent(
            command.getProductIdentifier(),
            command.getInstance().getIdentifier());
  }

  @SuppressWarnings("unused")
  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_CHARGE_DEFINITION)
  public ChargeDefinitionEvent process(final ChangeChargeDefinitionCommand command) {
    final ChargeDefinition chargeDefinition = command.getInstance();
    final String productIdentifier = command.getProductIdentifier();

    final ChargeDefinitionEntity existingChargeDefinition
            = chargeDefinitionRepository.findByProductIdAndChargeDefinitionIdentifier(productIdentifier, chargeDefinition.getIdentifier())
            .orElseThrow(() -> ServiceException.internalError("task definition not found."));

    final ChargeDefinitionEntity chargeDefinitionEntity =
            ChargeDefinitionMapper.map(existingChargeDefinition.getProduct(), chargeDefinition);
    chargeDefinitionEntity.setId(existingChargeDefinition.getId());
    chargeDefinitionEntity.setId(existingChargeDefinition.getId());
    chargeDefinitionRepository.save(chargeDefinitionEntity);

    return new ChargeDefinitionEvent(
            command.getProductIdentifier(),
            command.getInstance().getIdentifier());
  }

  @SuppressWarnings("unused")
  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.DELETE_PRODUCT_CHARGE_DEFINITION)
  public ChargeDefinitionEvent process(final DeleteProductChargeDefinitionCommand command) {
    final ChargeDefinitionEntity toDelete = chargeDefinitionRepository.findByProductIdAndChargeDefinitionIdentifier(
            command.getProductIdentifier(),
            command.getChargeDefinitionIdentifier())
            .orElseThrow(() -> ServiceException.notFound("Charge definition not found for product identifer ''{0}'' and charge definition identifier ''{1}''.",
                    command.getProductIdentifier(), command.getChargeDefinitionIdentifier()));

    chargeDefinitionRepository.delete(toDelete);

    return new ChargeDefinitionEvent(
            command.getProductIdentifier(),
            command.getChargeDefinitionIdentifier());
  }

}

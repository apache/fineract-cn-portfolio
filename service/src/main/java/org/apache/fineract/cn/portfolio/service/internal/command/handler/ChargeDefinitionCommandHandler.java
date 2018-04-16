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
package org.apache.fineract.cn.portfolio.service.internal.command.handler;

import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.events.ChargeDefinitionEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeChargeDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateChargeDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.DeleteProductChargeDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.mapper.ChargeDefinitionMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import java.util.Optional;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@Aggregate
public class ChargeDefinitionCommandHandler {
  private final ProductRepository productRepository;
  private final ChargeDefinitionRepository chargeDefinitionRepository;
  private final BalanceSegmentRepository balanceSegmentRepository;

  @Autowired
  public ChargeDefinitionCommandHandler(
      final ProductRepository productRepository,
      final ChargeDefinitionRepository chargeDefinitionRepository,
      final BalanceSegmentRepository balanceSegmentRepository) {
    this.productRepository = productRepository;
    this.chargeDefinitionRepository = chargeDefinitionRepository;
    this.balanceSegmentRepository = balanceSegmentRepository;
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

    final SegmentRange segmentRange = getSegmentRange(chargeDefinition, productIdentifier);

    final ChargeDefinitionEntity chargeDefinitionEntity =
            ChargeDefinitionMapper.map(productEntity, chargeDefinition, segmentRange.fromSegment, segmentRange.toSegment);
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

    final SegmentRange segmentRange = getSegmentRange(chargeDefinition, productIdentifier);

    final ChargeDefinitionEntity chargeDefinitionEntity =
            ChargeDefinitionMapper.map(existingChargeDefinition.getProduct(), chargeDefinition, segmentRange.fromSegment, segmentRange.toSegment);
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
            .orElseThrow(() -> ServiceException.notFound("Charge definition not found for product identifier ''{0}'' and charge definition identifier ''{1}''.",
                    command.getProductIdentifier(), command.getChargeDefinitionIdentifier()));

    chargeDefinitionRepository.delete(toDelete);

    return new ChargeDefinitionEvent(
            command.getProductIdentifier(),
            command.getChargeDefinitionIdentifier());
  }

  static class SegmentRange {
    final BalanceSegmentEntity fromSegment;
    final BalanceSegmentEntity toSegment;

    SegmentRange(final BalanceSegmentEntity fromSegment, final BalanceSegmentEntity toSegment) {
      this.fromSegment = fromSegment;
      this.toSegment = toSegment;
    }
  }

  private SegmentRange getSegmentRange(final ChargeDefinition chargeDefinition, final String productIdentifier) {
    if (chargeDefinition.getForSegmentSet() != null) {
      final Optional<BalanceSegmentEntity> fromSegmentOptional =
          balanceSegmentRepository.findByProductIdentifierAndSegmentSetIdentifierAndSegmentIdentifier(
              productIdentifier,
              chargeDefinition.getForSegmentSet(),
              chargeDefinition.getFromSegment());
      final Optional<BalanceSegmentEntity> toSegmentOptional =
          balanceSegmentRepository.findByProductIdentifierAndSegmentSetIdentifierAndSegmentIdentifier(
              productIdentifier,
              chargeDefinition.getForSegmentSet(),
              chargeDefinition.getFromSegment());

      if (fromSegmentOptional.isPresent() && toSegmentOptional.isPresent()) {
        return new SegmentRange(fromSegmentOptional.get(), toSegmentOptional.get());
      }
    }

    return new SegmentRange(null, null);
  }
}

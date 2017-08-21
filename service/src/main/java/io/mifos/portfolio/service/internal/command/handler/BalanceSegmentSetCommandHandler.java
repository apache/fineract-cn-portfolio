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
package io.mifos.portfolio.service.internal.command.handler;

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.events.BalanceSegmentSetEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.command.ChangeBalanceSegmentSetCommand;
import io.mifos.portfolio.service.internal.command.CreateBalanceSegmentSetCommand;
import io.mifos.portfolio.service.internal.command.DeleteBalanceSegmentSetCommand;
import io.mifos.portfolio.service.internal.mapper.BalanceSegmentSetMapper;
import io.mifos.portfolio.service.internal.repository.BalanceSegmentEntity;
import io.mifos.portfolio.service.internal.repository.BalanceSegmentRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Aggregate
public class BalanceSegmentSetCommandHandler {
  private final BalanceSegmentRepository balanceSegmentRepository;
  private final ProductRepository productRepository;

  @Autowired
  public BalanceSegmentSetCommandHandler(
      final BalanceSegmentRepository balanceSegmentRepository,
      final ProductRepository productRepository) {
    this.balanceSegmentRepository = balanceSegmentRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_BALANCE_SEGMENT_SET)
  public BalanceSegmentSetEvent process(final CreateBalanceSegmentSetCommand createBalanceSegmentSetCommand) {
    final ProductEntity product = productRepository.findByIdentifier(createBalanceSegmentSetCommand.getProductIdentifier())
        .orElseThrow(() -> ServiceException.notFound("Product with identifier ''{0}'' doesn''t exist.", createBalanceSegmentSetCommand.getProductIdentifier()));

    final List<BalanceSegmentEntity> balanceSegmentSetEntities = BalanceSegmentSetMapper.map(
        createBalanceSegmentSetCommand.getInstance(), product);

    balanceSegmentRepository.save(balanceSegmentSetEntities);

    return new BalanceSegmentSetEvent(
        createBalanceSegmentSetCommand.getProductIdentifier(),
        createBalanceSegmentSetCommand.getInstance().getIdentifier());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_BALANCE_SEGMENT_SET)
  public BalanceSegmentSetEvent process(final ChangeBalanceSegmentSetCommand changeBalanceSegmentSetCommand) {
    final ProductEntity product = productRepository.findByIdentifier(changeBalanceSegmentSetCommand.getProductIdentifier())
        .orElseThrow(() -> ServiceException.notFound("Product with identifier ''{0}'' doesn''t exist.", changeBalanceSegmentSetCommand.getProductIdentifier()));

    final List<BalanceSegmentEntity> balanceSegmentSets = balanceSegmentRepository.findByProductIdentifierAndSegmentSetIdentifier(
        changeBalanceSegmentSetCommand.getProductIdentifier(),
        changeBalanceSegmentSetCommand.getInstance().getIdentifier())
        .collect(Collectors.toList());
    if (balanceSegmentSets.isEmpty())
      throw ServiceException.notFound("Segment set with identifier ''{0}.{1}'' doesn''t exist.",
          changeBalanceSegmentSetCommand.getProductIdentifier(),
          changeBalanceSegmentSetCommand.getInstance().getIdentifier());

    balanceSegmentRepository.deleteInBatch(balanceSegmentSets);

    final List<BalanceSegmentEntity> balanceSegmentSetEntities = BalanceSegmentSetMapper.map(
        changeBalanceSegmentSetCommand.getInstance(), product);

    balanceSegmentRepository.save(balanceSegmentSetEntities);

    return new BalanceSegmentSetEvent(
        changeBalanceSegmentSetCommand.getProductIdentifier(),
        changeBalanceSegmentSetCommand.getInstance().getIdentifier());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.DELETE_BALANCE_SEGMENT_SET)
  public BalanceSegmentSetEvent process(final DeleteBalanceSegmentSetCommand deleteBalanceSegmentSetCommand) {
    final List<BalanceSegmentEntity> balanceSegmentSets = balanceSegmentRepository.findByProductIdentifierAndSegmentSetIdentifier(
        deleteBalanceSegmentSetCommand.getProductIdentifier(),
        deleteBalanceSegmentSetCommand.getBalanceSegmentSetIdentifier())
        .collect(Collectors.toList());
    if (balanceSegmentSets.isEmpty())
      throw ServiceException.notFound("Segment set with identifier ''{0}.{1}'' doesn''t exist.",
          deleteBalanceSegmentSetCommand.getProductIdentifier(),
          deleteBalanceSegmentSetCommand.getBalanceSegmentSetIdentifier());

    balanceSegmentRepository.deleteInBatch(balanceSegmentSets);

    return new BalanceSegmentSetEvent(
        deleteBalanceSegmentSetCommand.getProductIdentifier(),
        deleteBalanceSegmentSetCommand.getBalanceSegmentSetIdentifier());
  }
}

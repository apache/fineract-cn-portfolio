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
package io.mifos.individuallending.internal.command.handler;

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.command.internal.CommandBus;
import io.mifos.core.lang.ApplicationName;
import io.mifos.core.lang.DateConverter;
import io.mifos.individuallending.internal.command.ApplyInterestCommand;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.service.config.PortfolioProperties;
import io.mifos.portfolio.service.internal.command.CreateBeatPublishCommand;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.rhythm.spi.v1.domain.BeatPublish;
import io.mifos.rhythm.spi.v1.events.BeatPublishEvent;
import io.mifos.rhythm.spi.v1.events.EventConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class BeatPublishCommandHandler {
  private final CaseRepository caseRepository;
  private final PortfolioProperties portfolioProperties;
  private final ApplicationName applicationName;
  private final CommandBus commandBus;

  @Autowired
  public BeatPublishCommandHandler(
      final CaseRepository caseRepository,
      final PortfolioProperties portfolioProperties,
      final ApplicationName applicationName,
      final CommandBus commandBus) {
    this.caseRepository = caseRepository;
    this.portfolioProperties = portfolioProperties;
    this.applicationName = applicationName;
    this.commandBus = commandBus;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_PUBLISHEDBEAT)
  public BeatPublishEvent process(final CreateBeatPublishCommand createBeatPublishCommand) {
    final BeatPublish instance = createBeatPublishCommand.getInstance();
    final LocalDateTime forTime = DateConverter.fromIsoString(instance.getForTime());
    if (portfolioProperties.getBookInterestInTimeSlot() == forTime.getHour())
    {
      final Stream<CaseEntity> activeCases = caseRepository.findByCurrentStateIn(Collections.singleton(Case.State.ACTIVE.name()));
      activeCases.forEach(activeCase -> {
        final ApplyInterestCommand applyInterestCommand = new ApplyInterestCommand(activeCase.getProductIdentifier(), activeCase.getIdentifier());
        commandBus.dispatch(applyInterestCommand);
      });
    }

    return new BeatPublishEvent(applicationName.toString(), instance.getIdentifier(), instance.getForTime());
  }
}
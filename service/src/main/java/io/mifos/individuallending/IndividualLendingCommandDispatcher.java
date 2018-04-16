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
package io.mifos.individuallending;

import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.command.AcceptPaymentCommand;
import io.mifos.individuallending.internal.command.ApproveCommand;
import io.mifos.individuallending.internal.command.CloseCommand;
import io.mifos.individuallending.internal.command.DenyCommand;
import io.mifos.individuallending.internal.command.DisburseCommand;
import io.mifos.individuallending.internal.command.ImportCommand;
import io.mifos.individuallending.internal.command.OpenCommand;
import io.mifos.individuallending.internal.command.RecoverCommand;
import io.mifos.individuallending.internal.command.WriteOffCommand;
import io.mifos.portfolio.api.v1.domain.Command;
import io.mifos.portfolio.api.v1.domain.ImportParameters;
import io.mifos.products.spi.ProductCommandDispatcher;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
@Component
public class IndividualLendingCommandDispatcher implements ProductCommandDispatcher {
  private final CommandGateway commandGateway;

  @Autowired
  public IndividualLendingCommandDispatcher(final CommandGateway commandGateway) {
    this.commandGateway = commandGateway;
  }

  @Override
  public void dispatch(
      final String productIdentifier,
      final String caseIdentifier,
      final String actionIdentifier,
      final Command command) {
    final Action action = Action.valueOf(actionIdentifier);
    switch (action) {
      case OPEN:
        this.commandGateway.process(new OpenCommand(productIdentifier, caseIdentifier, command));
        break;
      case DENY:
        this.commandGateway.process(new DenyCommand(productIdentifier, caseIdentifier, command));
        break;
      case APPROVE:
        this.commandGateway.process(new ApproveCommand(productIdentifier, caseIdentifier, command));
        break;
      case DISBURSE:
        this.commandGateway.process(new DisburseCommand(productIdentifier, caseIdentifier, command));
        break;
      case ACCEPT_PAYMENT:
        this.commandGateway.process(new AcceptPaymentCommand(productIdentifier, caseIdentifier, command));
        break;
      case WRITE_OFF:
        this.commandGateway.process(new WriteOffCommand(productIdentifier, caseIdentifier, command));
        break;
      case CLOSE:
        this.commandGateway.process(new CloseCommand(productIdentifier, caseIdentifier, command));
        break;
      case RECOVER:
        this.commandGateway.process(new RecoverCommand(productIdentifier, caseIdentifier, command));
        break;
      default:
        throw ServiceException
            .badRequest("Action ''{0}'' is not implemented for individual loans.", actionIdentifier);
    }
  }

  @Override
  public void importCase(
      final String productIdentifier,
      final String caseIdentifier,
      final ImportParameters command) {
    this.commandGateway.process(new ImportCommand(productIdentifier, caseIdentifier, command));
  }
}

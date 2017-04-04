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
package io.mifos.portfolio;

import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.Command;
import io.mifos.portfolio.api.v1.domain.Product;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants.*;

/**
 * @author Myrle Krantz
 */
public class TestCommands extends AbstractPortfolioTest {
  @Test
  public void testHappyWorkflow() throws InterruptedException {
    final Product product = createProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, APPROVE_INDIVIDUALLOAN_CASE, Case.State.APPROVED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, DISBURSE_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.ACCEPT_PAYMENT, ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.ACCEPT_PAYMENT, ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.CLOSE, CLOSE_INDIVIDUALLOAN_CASE, Case.State.CLOSED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }

  @Test
  public void testBadCustomerWorkflow() throws InterruptedException {
    final Product product = createProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, APPROVE_INDIVIDUALLOAN_CASE, Case.State.APPROVED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, DISBURSE_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.WRITE_OFF, WRITE_OFF_INDIVIDUALLOAN_CASE, Case.State.CLOSED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }

  @Test
  public void testDisburseBeforeApproval() throws InterruptedException {
    final Product product = createProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);

    checkStateTransferFails(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, DISBURSE_INDIVIDUALLOAN_CASE, Case.State.PENDING);
  }

  public void checkStateTransfer(final String productIdentifier,
                                 final String caseIdentifier,
                                 final Action action,
                                 final String event,
                                 final Case.State nextState) throws InterruptedException {
    final Command command = new Command(action.name(), null);
    portfolioManager.executeCaseCommand(productIdentifier, caseIdentifier, command);

    Assert.assertTrue(eventRecorder.waitForMatch(event,
            (IndividualLoanCommandEvent x) -> individualLoanCommandEventMatches(x, productIdentifier, caseIdentifier)));

    final Case customerCase = portfolioManager.getCase(productIdentifier, caseIdentifier);
    Assert.assertEquals(customerCase.getCurrentState(), nextState.name());
  }

  public void checkStateTransferFails(final String productIdentifier,
                                 final String caseIdentifier,
                                 final Action action,
                                 final String event,
                                 final Case.State initialState) throws InterruptedException {
    final Command command = new Command(action.name(), null);
    try {
      portfolioManager.executeCaseCommand(productIdentifier, caseIdentifier, command);
      Assert.fail();
    }
    catch (final IllegalArgumentException ignored) {}

    Assert.assertFalse(eventRecorder.waitForMatch(event,
            (IndividualLoanCommandEvent x) -> individualLoanCommandEventMatches(x, productIdentifier, caseIdentifier)));

    final Case customerCase = portfolioManager.getCase(productIdentifier, caseIdentifier);
    Assert.assertEquals(customerCase.getCurrentState(), initialState.name());
  }

  private void checkNextActionsCorrect(final String productIdentifier, final String customerCaseIdentifier, final Action... nextActions)
  {
    final Set<String> actionList = Arrays.stream(nextActions).map(Enum::name).collect(Collectors.toSet());
    Assert.assertEquals(actionList, portfolioManager.getActionsForCase(productIdentifier, customerCaseIdentifier));
  }

  private boolean individualLoanCommandEventMatches(
          final IndividualLoanCommandEvent event,
          final String productIdentifier,
          final String caseIdentifier)
  {
    return event.getProductIdentifier().equals(productIdentifier) &&
            event.getCaseIdentifier().equals(caseIdentifier);
  }

  //TODO: Missing test for getExecutedCommandForCase.
}
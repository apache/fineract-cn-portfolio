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

import java.util.Collections;

import static io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants.*;

/**
 * @author Myrle Krantz
 */
public class TestCommands extends AbstractPortfolioTest {
  @Test
  public void testHappyWorkflow() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, Collections.emptyList(), OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Collections.emptyList(), APPROVE_INDIVIDUALLOAN_CASE, Case.State.APPROVED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Collections.emptyList(), DISBURSE_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.ACCEPT_PAYMENT, Collections.emptyList(), ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.ACCEPT_PAYMENT, Collections.emptyList(), ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.CLOSE, Collections.emptyList(), CLOSE_INDIVIDUALLOAN_CASE, Case.State.CLOSED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }

  @Test
  public void testBadCustomerWorkflow() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, Collections.emptyList(), OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Collections.emptyList(), APPROVE_INDIVIDUALLOAN_CASE, Case.State.APPROVED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Action.CLOSE);


    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, Collections.emptyList(), DISBURSE_INDIVIDUALLOAN_CASE, Case.State.ACTIVE);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(),
            Action.APPLY_INTEREST, Action.MARK_LATE, Action.ACCEPT_PAYMENT, Action.DISBURSE, Action.WRITE_OFF, Action.CLOSE);

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.WRITE_OFF, Collections.emptyList(), WRITE_OFF_INDIVIDUALLOAN_CASE, Case.State.CLOSED);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier());
  }

  @Test
  public void testDisburseBeforeApproval() throws InterruptedException {
    final Product product = createAndEnableProduct();
    final Case customerCase = createCase(product.getIdentifier());

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN, Collections.emptyList(), OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);

    checkStateTransferFails(product.getIdentifier(), customerCase.getIdentifier(), Action.DISBURSE, DISBURSE_INDIVIDUALLOAN_CASE, Case.State.PENDING);
  }

  public void checkStateTransferFails(final String productIdentifier,
                                 final String caseIdentifier,
                                 final Action action,
                                 final String event,
                                 final Case.State initialState) throws InterruptedException {
    final Command command = new Command();
    try {
      portfolioManager.executeCaseCommand(productIdentifier, caseIdentifier, action.name(), command);
      Assert.fail();
    }
    catch (final IllegalArgumentException ignored) {}

    Assert.assertFalse(eventRecorder.waitForMatch(event,
            (IndividualLoanCommandEvent x) -> individualLoanCommandEventMatches(x, productIdentifier, caseIdentifier)));

    final Case customerCase = portfolioManager.getCase(productIdentifier, caseIdentifier);
    Assert.assertEquals(customerCase.getCurrentState(), initialState.name());
  }
}
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

import com.google.gson.Gson;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.api.v1.events.ChargeDefinitionEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.*;
import static io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE;
import static io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE;
import static io.mifos.portfolio.Fixture.*;

/**
 * @author Myrle Krantz
 */
public class TestAccountingInteraction extends AbstractPortfolioTest {

  @Test
  public void testLoanApproval() throws InterruptedException {
    //Create product and set charges to fixed fees.
    final Product product = createProduct();

    final ChargeDefinition processingFee = portfolioManager.getChargeDefinition(product.getIdentifier(), PROCESSING_FEE_ID);
    processingFee.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    processingFee.setAmount(BigDecimal.valueOf(10_0000, 4));
    portfolioManager.changeChargeDefinition(product.getIdentifier(), PROCESSING_FEE_ID, processingFee);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CHARGE_DEFINITION,
            new ChargeDefinitionEvent(product.getIdentifier(), PROCESSING_FEE_ID)));

    final ChargeDefinition loanOriginationFee = portfolioManager.getChargeDefinition(product.getIdentifier(), LOAN_ORIGINATION_FEE_ID);
    loanOriginationFee.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
    loanOriginationFee.setAmount(BigDecimal.valueOf(100_0000, 4));
    portfolioManager.changeChargeDefinition(product.getIdentifier(), LOAN_ORIGINATION_FEE_ID, loanOriginationFee);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CHARGE_DEFINITION,
            new ChargeDefinitionEvent(product.getIdentifier(), LOAN_ORIGINATION_FEE_ID)));

    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));



    //Create case.
    final CaseParameters caseParameters = Fixture.createAdjustedCaseParameters(x -> {});
    final String caseParametersAsString = new Gson().toJson(caseParameters);
    final Case customerCase = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(caseParametersAsString));

    //Open the case and accept a processing fee.
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN);
    checkCostComponentForActionCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN,
            new CostComponent(processingFee.getIdentifier(), processingFee.getAmount()));

    final AccountAssignment openCommandProcessingFeeAccountAssignment = new AccountAssignment();
    openCommandProcessingFeeAccountAssignment.setDesignator(processingFee.getFromAccountDesignator());
    openCommandProcessingFeeAccountAssignment.setAccountIdentifier(TELLER_ONE_ACCOUNT_IDENTIFIER);

    checkStateTransfer(product.getIdentifier(), customerCase.getIdentifier(), Action.OPEN,
            Collections.singletonList(openCommandProcessingFeeAccountAssignment),
            OPEN_INDIVIDUALLOAN_CASE, Case.State.PENDING);
    checkNextActionsCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE, Action.DENY);
    checkCostComponentForActionCorrect(product.getIdentifier(), customerCase.getIdentifier(), Action.APPROVE,
            new CostComponent(loanOriginationFee.getIdentifier(), loanOriginationFee.getAmount()));

    AccountingFixture.verifyTransfer(ledgerManager,
            TELLER_ONE_ACCOUNT_IDENTIFIER, PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER,
            processingFee.getAmount()
            );
  }
}

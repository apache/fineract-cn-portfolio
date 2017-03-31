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
import io.mifos.core.test.domain.TimeStampChecker;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.CUSTOMER_LOAN;
import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.ENTRY;

/**
 * @author Myrle Krantz
 */
public class TestCases extends AbstractPortfolioTest {

  public TestCases() { }

  @Test
  public void shouldCreateCase() throws InterruptedException {
    final Product product = createProduct();

    final TimeStampChecker timeStampChecker = TimeStampChecker.roughlyNow();
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> {});
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
    Assert.assertEquals(caseAsSaved.getCreatedBy(), TEST_USER);
    timeStampChecker.assertCorrect(caseAsSaved.getCreatedOn());
    Assert.assertEquals(caseAsSaved.getLastModifiedBy(), TEST_USER);
    timeStampChecker.assertCorrect(caseAsSaved.getLastModifiedOn());
    Assert.assertEquals(Case.State.CREATED.name(), caseAsSaved.getCurrentState());
  }

  @Test
  public void shouldChangeCase() throws InterruptedException {
    final Product product = createProduct();

    final CaseParameters newCaseParameters = Fixture.createAdjustedCaseParameters(x -> {});
    final String originalParameters = new Gson().toJson(newCaseParameters);
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(originalParameters));
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    accountAssignments.add(new AccountAssignment(CUSTOMER_LOAN, "002-011"));
    accountAssignments.add(new AccountAssignment(ENTRY, "002-012"));
    caseInstance.setAccountAssignments(accountAssignments);

    newCaseParameters.setInitialBalance(Fixture.fixScale(BigDecimal.TEN));
    newCaseParameters.getPaymentCycle().setAlignmentDay(1);
    newCaseParameters.getPaymentCycle().setAlignmentWeek(2);
    final String changedParameters = new Gson().toJson(newCaseParameters);
    caseInstance.setParameters(changedParameters);

    TimeUnit.SECONDS.sleep(2);

    final TimeStampChecker timeStampChecker = TimeStampChecker.roughlyNow();

    portfolioManager.changeCase(product.getIdentifier(), caseInstance.getIdentifier(), caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
    Assert.assertEquals(TEST_USER, caseAsSaved.getLastModifiedBy());
    timeStampChecker.assertCorrect(caseAsSaved.getLastModifiedOn());
    Assert.assertEquals(Case.State.CREATED.name(), caseAsSaved.getCurrentState());
  }

  @Test
  public void shouldRemoveCaseAccountAssignments() throws InterruptedException {
    final Product product = createProduct();

    product.setAccountAssignments(Collections.emptySet());
    portfolioManager.changeProduct(product.getIdentifier(), product);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT, product.getIdentifier()));

    final Product productAsSaved = portfolioManager.getProduct(product.getIdentifier());
    Assert.assertTrue("Account assignments should be empty, but contain: " + productAsSaved.getAccountAssignments(),
            productAsSaved.getAccountAssignments().isEmpty());
    Assert.assertEquals(product, productAsSaved);

    final Set<AccountAssignment> incompleteAccountAssignmentsAfterChange
            = portfolioManager.getIncompleteAccountAssignments(product.getIdentifier());
    Assert.assertFalse("Incomplete account assignments should not be empty, but is. (Beware the double negative.)",
            incompleteAccountAssignmentsAfterChange.isEmpty());
  }
}

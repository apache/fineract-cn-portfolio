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
import io.mifos.individuallending.api.v1.domain.caseinstance.CreditWorthinessFactor;
import io.mifos.individuallending.api.v1.domain.caseinstance.CreditWorthinessSnapshot;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.CasePage;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.CUSTOMER_LOAN;
import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.ENTRY;

/**
 * @author Myrle Krantz
 */
public class TestCases extends AbstractPortfolioTest {

  public TestCases() { }

  @Test
  public void shouldCreateCase() throws InterruptedException {
    final Product product = createAndEnableProduct();

    final TimeStampChecker timeStampChecker = TimeStampChecker.roughlyNow();
    final Case caseInstance = createCase(product.getIdentifier());

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
    final Product product = createAndEnableProduct();

    final CaseParameters newCaseParameters = Fixture.createAdjustedCaseParameters(x -> {});
    final String originalParameters = new Gson().toJson(newCaseParameters);
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(originalParameters));

    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    accountAssignments.add(new AccountAssignment(CUSTOMER_LOAN, "002-011"));
    accountAssignments.add(new AccountAssignment(ENTRY, "002-012"));
    caseInstance.setAccountAssignments(accountAssignments);

    newCaseParameters.setMaximumBalance(Fixture.fixScale(BigDecimal.TEN));
    newCaseParameters.getPaymentCycle().setAlignmentDay(1);
    newCaseParameters.getPaymentCycle().setAlignmentWeek(2);

    newCaseParameters.setCreditWorthinessSnapshots(Collections.emptyList());

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
  public void shouldRemoveCosigner() throws InterruptedException {
    final Product product = createAndEnableProduct();

    final CaseParameters caseParameters = Fixture.createAdjustedCaseParameters(x -> {});
    final String caseParametersAsString = new Gson().toJson(caseParameters);
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(caseParametersAsString));

    caseParameters.setCreditWorthinessSnapshots(Collections.singletonList(caseParameters.getCreditWorthinessSnapshots().get(0)));
    final String changedParameters = new Gson().toJson(caseParameters);
    caseInstance.setParameters(changedParameters);

    portfolioManager.changeCase(product.getIdentifier(), caseInstance.getIdentifier(), caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
  }

  @Test
  public void shouldAddDebt() throws InterruptedException {
    final Product product = createAndEnableProduct();

    final CaseParameters caseParameters = Fixture.createAdjustedCaseParameters(x -> {});
    final String caseParametersAsString = new Gson().toJson(caseParameters);
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(caseParametersAsString));

    final List<CreditWorthinessFactor> debts = caseParameters.getCreditWorthinessSnapshots().get(0).getDebts();
    final ArrayList<CreditWorthinessFactor> newDebts = new ArrayList<>();
    newDebts.addAll(debts);
    newDebts.add(new CreditWorthinessFactor("boop", BigDecimal.valueOf(5, 4)));
    caseParameters.getCreditWorthinessSnapshots().get(0).setDebts(newDebts);
    final String changedParameters = new Gson().toJson(caseParameters);
    caseInstance.setParameters(changedParameters);

    portfolioManager.changeCase(product.getIdentifier(), caseInstance.getIdentifier(), caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
  }

  @Test
  public void shouldRemoveCaseAccountAssignments() throws InterruptedException {
    final Product product = createAndEnableProduct();

    final Case caseInstance = createCase(product.getIdentifier());
    caseInstance.setAccountAssignments(Collections.emptySet());

    portfolioManager.changeCase(product.getIdentifier(), caseInstance.getIdentifier(), caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());
    Assert.assertEquals(caseInstance, caseAsSaved);
    Assert.assertTrue(caseInstance.getAccountAssignments().isEmpty());
  }

  @Test
  public void shouldCreateCosignerWithoutDetails() throws InterruptedException {
    final Product product = createAndEnableProduct();

    final CaseParameters caseParameters = Fixture.createAdjustedCaseParameters(x -> {
      final CreditWorthinessSnapshot bob = new CreditWorthinessSnapshot("bob");
      bob.setDebts(Collections.emptyList());
      bob.setAssets(Collections.emptyList());
      bob.setIncomeSources(Collections.emptyList());
      x.getCreditWorthinessSnapshots().add(bob);
    });
    final String caseParametersAsString = new Gson().toJson(caseParameters);
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> x.setParameters(caseParametersAsString));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
  }

  @Test
  public void shouldThrowWhenProductNotActivated() throws InterruptedException {
    final Product product = createProduct();

    try {
      createCase(product.getIdentifier());
      Assert.fail("This should cause an illegal argument exception.");
    }
    catch (final IllegalArgumentException ignored){
    }

    portfolioManager.enableProduct(product.getIdentifier(), true);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));

    try {
      final Case caseInstance = createCase(product.getIdentifier());
      Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CASE,
              new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));
    }
    catch (final IllegalArgumentException ignored){
      Assert.fail("This should *not* cause an illegal argument exception.");
    }

    portfolioManager.enableProduct(product.getIdentifier(), false);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_PRODUCT_ENABLE, product.getIdentifier()));
    try {
      createCase(product.getIdentifier());
      Assert.fail("This should cause an illegal argument exception.");
    }
    catch (final IllegalArgumentException ignored){
    }
  }

  @Test
  public void shouldListAndPageCases() throws InterruptedException {
    final Product product = createAndEnableProduct();

    final Set<String> expectedCaseIdentifiers = new HashSet<>();

    for (int i = 0; i < 20; i++) {
      final Case caseInstance = createCase(product.getIdentifier());
      expectedCaseIdentifiers.add(caseInstance.getIdentifier());
    }

    final CasePage casePage1 = portfolioManager.getAllCasesForProduct(product.getIdentifier(), false, 0, 10);
    Assert.assertEquals(casePage1.getTotalElements(), Long.valueOf(20L));
    Assert.assertEquals(casePage1.getElements().size(), 10);
    Assert.assertEquals(casePage1.getTotalPages(), Integer.valueOf(2));

    final CasePage casePage2 = portfolioManager.getAllCasesForProduct(product.getIdentifier(), false, 1, 10);
    Assert.assertEquals(casePage2.getTotalElements(), Long.valueOf(20L));
    Assert.assertEquals(casePage2.getElements().size(), 10);
    Assert.assertEquals(casePage2.getTotalPages(), Integer.valueOf(2));

    final Set<String> returnedCaseIdentifiers = Stream.concat(
            casePage1.getElements().stream().map(Case::getIdentifier),
            casePage2.getElements().stream().map(Case::getIdentifier))
            .collect(Collectors.toSet());

    Assert.assertEquals(expectedCaseIdentifiers, returnedCaseIdentifiers);
  }
}

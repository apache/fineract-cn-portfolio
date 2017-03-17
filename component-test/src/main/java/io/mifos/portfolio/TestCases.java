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
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.core.lang.DateConverter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

    final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
    final Case caseInstance = createAdjustedCase(product.getIdentifier(), x -> {});
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
    Assert.assertEquals(caseAsSaved.getCreatedBy(), TEST_USER);
    checkTimeStamp(caseAsSaved.getCreatedOn(), now);
    Assert.assertEquals(caseAsSaved.getLastModifiedBy(), TEST_USER);
    checkTimeStamp(caseAsSaved.getLastModifiedOn(), now);
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

    TimeUnit.SECONDS.sleep(3);

    final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
    portfolioManager.changeCase(product.getIdentifier(), caseInstance.getIdentifier(), caseInstance);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.PUT_CASE,
            new CaseEvent(product.getIdentifier(), caseInstance.getIdentifier())));

    final Case caseAsSaved = portfolioManager.getCase(product.getIdentifier(), caseInstance.getIdentifier());

    Assert.assertEquals(caseInstance, caseAsSaved);
    Assert.assertEquals(caseAsSaved.getLastModifiedBy(), TEST_USER);
    checkTimeStamp(caseAsSaved.getLastModifiedOn(), now);
    Assert.assertEquals(Case.State.CREATED.name(), caseAsSaved.getCurrentState());
  }

  private void checkTimeStamp(final String timeStamp, final LocalDateTime expectedTimeStamp) {
    final LocalDateTime parsedTimeStamp = DateConverter.fromIsoString(timeStamp);

    final long deltaFromExpected = Math.abs(parsedTimeStamp.until(expectedTimeStamp, ChronoUnit.SECONDS));

    Assert.assertTrue("Delta from expected should have been less than 3 seconds, but was " + deltaFromExpected +
                    ". Timestamp string was " + timeStamp + ".",
            deltaFromExpected <= 2);
  }
}
